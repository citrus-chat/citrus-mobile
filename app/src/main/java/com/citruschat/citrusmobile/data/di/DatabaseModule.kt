package com.citruschat.citrusmobile.data.di

import android.content.Context
import androidx.room.Room
import com.citruschat.citrusmobile.data.local.dao.MessageDao
import com.citruschat.citrusmobile.data.local.database.AppDatabase
import com.citruschat.citrusmobile.data.repository.ChatRepositoryImpl
import com.citruschat.citrusmobile.domain.repository.ChatRepository
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
    fun provideChatRepository(dao: MessageDao): ChatRepository = ChatRepositoryImpl(dao)
}
