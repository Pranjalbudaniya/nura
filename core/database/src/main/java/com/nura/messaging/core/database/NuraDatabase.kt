package com.nura.messaging.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nura.messaging.core.database.dao.ConversationDao
import com.nura.messaging.core.database.dao.MessageDao
import com.nura.messaging.core.database.entity.ConversationEntity
import com.nura.messaging.core.database.entity.MessageEntity

@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NuraDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
}
