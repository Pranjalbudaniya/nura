package com.nura.messaging.data.repositories.di

import com.nura.messaging.data.repositories.auth.AuthRepositoryImpl
import com.nura.messaging.domain.repositories.auth.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindContactsRepository(
        impl: com.nura.messaging.data.repositories.contacts.ContactsRepositoryImpl
    ): com.nura.messaging.domain.repositories.contacts.ContactsRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: com.nura.messaging.data.repositories.chat.ChatRepositoryImpl
    ): com.nura.messaging.domain.repositories.chat.ChatRepository
}
