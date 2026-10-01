package com.nura.messaging.core.notifications.di

import com.nura.messaging.core.notifications.NuraNotificationManager
import com.nura.messaging.domain.repositories.notification.NotificationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationService(
        impl: NuraNotificationManager
    ): NotificationService
}
