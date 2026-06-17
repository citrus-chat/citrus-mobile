package com.citruschat.citrusmobile.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.local.dao.MessageDao
import com.citruschat.citrusmobile.data.local.dao.UserDao
import com.citruschat.citrusmobile.data.local.database.AppDatabase
import com.citruschat.citrusmobile.data.repository.ChatRepositoryImpl
import com.citruschat.citrusmobile.data.repository.MessageRepositoryImpl
import com.citruschat.citrusmobile.data.repository.ThemeRepositoryImpl
import com.citruschat.citrusmobile.data.repository.UserRepositoryImpl
import com.citruschat.citrusmobile.data.user.UserRemoteDataSource
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import com.citruschat.citrusmobile.domain.repository.MessageRepository
import com.citruschat.citrusmobile.domain.repository.ThemeRepository
import com.citruschat.citrusmobile.domain.repository.UserRepository
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
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                "citrus.db",
            ).addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .build()

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

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideUserRepository(
        dao: UserDao,
        userRemoteDataSource: UserRemoteDataSource,
        logger: Logger,
    ): UserRepository = UserRepositoryImpl(dao, userRemoteDataSource, logger)

    @Provides
    @Singleton
    fun provideThemeRepository(repository: ThemeRepositoryImpl): ThemeRepository = repository
}

private val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT NOT NULL PRIMARY KEY,
                    email TEXT NOT NULL,
                    username TEXT NOT NULL,
                    profilePictureUrl TEXT,
                    statusMessage TEXT,
                    createdAt TEXT NOT NULL DEFAULT '',
                    isCurrentUser INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
        }
    }

private val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS chat_participants (
                    chatId INTEGER NOT NULL,
                    userId TEXT NOT NULL,
                    PRIMARY KEY(chatId, userId),
                    FOREIGN KEY(chatId) REFERENCES chats(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_chats_lastMessageId ON chats(lastMessageId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_chatId ON messages(chatId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_participants_chatId ON chat_participants(chatId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_participants_userId ON chat_participants(userId)")
        }
    }
