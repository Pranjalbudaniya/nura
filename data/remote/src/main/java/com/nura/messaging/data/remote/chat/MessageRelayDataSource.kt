package com.nura.messaging.data.remote.chat

import android.util.Log
import com.nura.messaging.core.common.util.DispatcherProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRelayDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val postgrest: Postgrest,
    private val realtime: Realtime,
    private val dispatchers: DispatcherProvider
) {
    companion object {
        private const val TAG = "MessageRelayDataSource"
        private const val TABLE_NAME = "messages_relay"
        private const val EVENT_NEW_MESSAGE = "new_message"
        private const val EVENT_DELIVERY_ACK = "delivery_ack"
    }

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    private suspend fun getOrJoinChannel(topic: String): RealtimeChannel {
        val existing = realtime.subscriptions[topic]
        if (existing != null && existing.status.value == RealtimeChannel.Status.SUBSCRIBED) {
            return existing
        }

        val channel = existing ?: client.channel(topic)
        if (channel.status.value != RealtimeChannel.Status.SUBSCRIBED) {
            try {
                if (realtime.status.value != Realtime.Status.CONNECTED) {
                    realtime.connect()
                }
                channel.subscribe(blockUntilSubscribed = true)
                Log.d(TAG, "Subscribed successfully to channel: $topic")
            } catch (e: Exception) {
                Log.w(TAG, "Blocking subscribe failed for $topic, trying non-blocking: ${e.message}")
                try {
                    channel.subscribe(blockUntilSubscribed = false)
                } catch (_: Exception) {}
            }
        }
        return channel
    }

    suspend fun sendMessageToRelay(dto: MessageRelayDto): Result<Unit> = withContext(dispatchers.io) {
        var broadcastSuccess = false
        var postgrestSuccess = false
        var lastError: Throwable? = null

        // 1. Direct Realtime Broadcast to receiver's inbox topic
        try {
            val inboxTopic = "nura_inbox_${dto.receiverId}"
            val channel = getOrJoinChannel(inboxTopic)
            channel.broadcast(event = EVENT_NEW_MESSAGE, message = dto)
            Log.d(TAG, "Broadcast message ${dto.messageId} to $inboxTopic")
            broadcastSuccess = true
        } catch (e: Exception) {
            Log.w(TAG, "Broadcast to inbox topic failed: ${e.message}", e)
            lastError = e
        }

        // 2. Also broadcast to conversation topic if available
        if (dto.conversationId.isNotBlank()) {
            try {
                val convTopic = "nura_conv_${dto.conversationId}"
                val channel = getOrJoinChannel(convTopic)
                channel.broadcast(event = EVENT_NEW_MESSAGE, message = dto)
                Log.d(TAG, "Broadcast message ${dto.messageId} to $convTopic")
                broadcastSuccess = true
            } catch (e: Exception) {
                Log.w(TAG, "Broadcast to conv topic failed: ${e.message}")
            }
        }

        // 3. Persistent Supabase table insert (for offline pickup when schema exists)
        try {
            postgrest[TABLE_NAME].insert(dto)
            Log.d(TAG, "Message ${dto.messageId} inserted into $TABLE_NAME table")
            postgrestSuccess = true
        } catch (e: Exception) {
            Log.w(TAG, "PostgREST insert failed (schema may be pending): ${e.message}")
            if (lastError == null) lastError = e
        }

        if (broadcastSuccess || postgrestSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(lastError ?: RuntimeException("Failed to relay message via broadcast or table"))
        }
    }

    suspend fun fetchPendingMessages(receiverId: String): Result<List<MessageRelayDto>> = withContext(dispatchers.io) {
        runCatching {
            Log.d(TAG, "Fetching pending messages from $TABLE_NAME for receiver $receiverId")
            val messages = postgrest[TABLE_NAME].select {
                filter {
                    eq("receiver_id", receiverId)
                }
            }.decodeList<MessageRelayDto>()
            Log.d(TAG, "Fetched ${messages.size} pending messages from $TABLE_NAME")
            messages
        }.onFailure { e ->
            Log.w(TAG, "Failed to fetch pending messages for receiver $receiverId: ${e.message}")
        }
    }

    suspend fun acknowledgeAndRemoveMessage(
        messageId: String,
        senderId: String,
        receiverId: String
    ): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            // 1. Broadcast delivery ACK back to sender's inbox topic
            if (senderId.isNotBlank()) {
                try {
                    val senderTopic = "nura_inbox_$senderId"
                    val channel = getOrJoinChannel(senderTopic)
                    val ack = DeliveryAckDto(messageId = messageId, receiverId = receiverId)
                    channel.broadcast(event = EVENT_DELIVERY_ACK, message = ack)
                    Log.d(TAG, "Broadcast delivery ACK for $messageId to $senderTopic")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to broadcast delivery ACK to sender $senderId: ${e.message}")
                }
            }

            // 2. Remove temporary copy from PostgREST messages_relay table
            try {
                postgrest[TABLE_NAME].delete {
                    filter {
                        eq("message_id", messageId)
                        eq("receiver_id", receiverId)
                    }
                }
                Log.d(TAG, "Temporary relay message $messageId removed from $TABLE_NAME")
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete from $TABLE_NAME: ${e.message}")
            }
            Unit
        }
    }

    fun observeIncomingMessages(receiverId: String): Flow<MessageRelayDto> = callbackFlow {
        val topic = "nura_inbox_$receiverId"
        Log.d(TAG, "Starting observeIncomingMessages for $topic")
        val channel = getOrJoinChannel(topic)

        // 1. Collect Realtime Broadcasts
        val broadcastJob = launch {
            try {
                channel.broadcastFlow<MessageRelayDto>(EVENT_NEW_MESSAGE).collect { dto ->
                    Log.d(TAG, "Realtime broadcast received: ${dto.messageId} from ${dto.senderId}")
                    trySend(dto)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in broadcastFlow collection for $topic: ${e.message}", e)
            }
        }

        // 2. Collect Postgres Changes if table exists
        val postgresJob = launch {
            try {
                val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = TABLE_NAME
                }
                changeFlow.collect { action ->
                    try {
                        val dto = json.decodeFromJsonElement(MessageRelayDto.serializer(), action.record)
                        if (dto.receiverId == receiverId) {
                            Log.d(TAG, "Postgres change received: ${dto.messageId}")
                            trySend(dto)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error decoding postgres change record: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Postgres change subscription for $topic inactive: ${e.message}")
            }
        }

        awaitClose {
            Log.d(TAG, "Closing observeIncomingMessages for $topic")
            broadcastJob.cancel()
            postgresJob.cancel()
        }
    }.flowOn(dispatchers.io)

    fun observeDeliveryAcks(senderId: String): Flow<DeliveryAckDto> = callbackFlow {
        val topic = "nura_inbox_$senderId"
        Log.d(TAG, "Starting observeDeliveryAcks for $topic")
        val channel = getOrJoinChannel(topic)

        val ackJob = launch {
            try {
                channel.broadcastFlow<DeliveryAckDto>(EVENT_DELIVERY_ACK).collect { ack ->
                    Log.d(TAG, "Realtime delivery ACK received for ${ack.messageId}")
                    trySend(ack)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in delivery ACK flow for $topic: ${e.message}", e)
            }
        }

        awaitClose {
            Log.d(TAG, "Closing observeDeliveryAcks for $topic")
            ackJob.cancel()
        }
    }.flowOn(dispatchers.io)
}
