package com.nura.messaging.core.database.di

import android.content.Context
import androidx.room.Room
import com.nura.messaging.core.database.NuraDatabase
import com.nura.messaging.core.database.dao.ConversationDao
import com.nura.messaging.core.database.dao.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNuraDatabase(
        @ApplicationContext context: Context
    ): NuraDatabase {
        return Room.databaseBuilder(
            context,
            NuraDatabase::class.java,
            "nura_chat.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: NuraDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideConversationDao(database: NuraDatabase): ConversationDao {
        return database.conversationDao()
    }
}
