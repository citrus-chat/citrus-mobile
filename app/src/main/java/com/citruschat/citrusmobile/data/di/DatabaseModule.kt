package com.citruschat.citrusmobile.data.di

import android.content.Context
import androidx.room.Room
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.local.dao.MessageDao
import com.citruschat.citrusmobile.data.local.database.AppDatabase
import com.citruschat.citrusmobile.data.repository.ChatRepositoryImpl
import com.citruschat.citrusmobile.data.repository.MessageRepositoryImpl
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import com.citruschat.citrusmobile.domain.repository.MessageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.jvm.java

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                "citrus.db",
            ).build()

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()

    @Provides
    @Singleton
    fun provideMessageRepository(
        dao: MessageDao,
        logger: Logger,
    ): MessageRepository = MessageRepositoryImpl(dao, logger)

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao = database.chatDao()

    @Provides
    @Singleton
    fun provideChatRepository(
        dao: ChatDao,
        logger: Logger,
    ): ChatRepository = ChatRepositoryImpl(dao, logger)
}
