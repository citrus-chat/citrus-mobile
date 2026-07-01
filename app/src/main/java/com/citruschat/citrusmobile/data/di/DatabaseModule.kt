package com.citruschat.citrusmobile.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.citruschat.citrusmobile.app.AppVisibilityTracker
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.data.chat.ChatApiClient
import com.citruschat.citrusmobile.data.crypto.ConversationCrypto
import com.citruschat.citrusmobile.data.crypto.ConversationKeyStore
import com.citruschat.citrusmobile.data.device.DeviceIdentityProvider
import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.local.dao.MessageDao
import com.citruschat.citrusmobile.data.local.dao.UserDao
import com.citruschat.citrusmobile.data.local.database.AppDatabase
import com.citruschat.citrusmobile.data.message.MessageApiClient
import com.citruschat.citrusmobile.data.notification.ChatNotificationNotifier
import com.citruschat.citrusmobile.data.repository.ChatRepositoryImpl
import com.citruschat.citrusmobile.data.repository.MessageRepositoryImpl
import com.citruschat.citrusmobile.data.repository.ThemeRepositoryImpl
import com.citruschat.citrusmobile.data.repository.UserRepositoryImpl
import com.citruschat.citrusmobile.data.user.FileUserAvatarLocalDataSource
import com.citruschat.citrusmobile.data.user.UserAvatarLocalDataSource
import com.citruschat.citrusmobile.data.user.UserRemoteDataSource
import com.citruschat.citrusmobile.domain.realtime.ChatRealtimeClient
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
            ).addMigrations(
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
            ).build()

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()

    @Provides
    @Singleton
    fun provideMessageRepository(
        dao: MessageDao,
        chatDao: ChatDao,
        userDao: UserDao,
        tokenStore: TokenStore,
        messageApiClient: MessageApiClient,
        realtimeClient: ChatRealtimeClient,
        notificationNotifier: ChatNotificationNotifier,
        appVisibilityTracker: AppVisibilityTracker,
        logger: Logger,
    ): MessageRepository =
        MessageRepositoryImpl(
            dao = dao,
            chatDao = chatDao,
            userDao = userDao,
            tokenStore = tokenStore,
            messageApiClient = messageApiClient,
            realtimeClient = realtimeClient,
            notificationNotifier = notificationNotifier,
            appVisibilityTracker = appVisibilityTracker,
            logger = logger,
        )

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao = database.chatDao()

    @Provides
    @Singleton
    fun provideChatRepository(
        dao: ChatDao,
        userDao: UserDao,
        userRemoteDataSource: UserRemoteDataSource,
        avatarLocalDataSource: UserAvatarLocalDataSource,
        chatApiClient: ChatApiClient,
        deviceIdentityProvider: DeviceIdentityProvider,
        conversationCrypto: ConversationCrypto,
        conversationKeyStore: ConversationKeyStore,
        logger: Logger,
    ): ChatRepository =
        ChatRepositoryImpl(
            dao = dao,
            userDao = userDao,
            userRemoteDataSource = userRemoteDataSource,
            avatarLocalDataSource = avatarLocalDataSource,
            chatApiClient = chatApiClient,
            deviceIdentityProvider = deviceIdentityProvider,
            conversationCrypto = conversationCrypto,
            conversationKeyStore = conversationKeyStore,
            logger = logger,
        )

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideUserRepository(
        dao: UserDao,
        userRemoteDataSource: UserRemoteDataSource,
        avatarLocalDataSource: UserAvatarLocalDataSource,
        logger: Logger,
    ): UserRepository = UserRepositoryImpl(dao, userRemoteDataSource, avatarLocalDataSource, logger)

    @Provides
    @Singleton
    fun provideUserAvatarLocalDataSource(localDataSource: FileUserAvatarLocalDataSource): UserAvatarLocalDataSource = localDataSource

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

private val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE messages ADD COLUMN deliveryStatus TEXT NOT NULL DEFAULT 'SENT'")
        }
    }

private val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE chats ADD COLUMN profilePictureUrl TEXT")
        }
    }

private val MIGRATION_6_7 =
    object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE users_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    email TEXT NOT NULL,
                    username TEXT NOT NULL,
                    remoteProfilePictureUrl TEXT,
                    localProfilePicturePath TEXT,
                    statusMessage TEXT,
                    createdAt TEXT NOT NULL DEFAULT '',
                    isCurrentUser INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO users_new (
                    id,
                    email,
                    username,
                    remoteProfilePictureUrl,
                    localProfilePicturePath,
                    statusMessage,
                    createdAt,
                    isCurrentUser
                )
                SELECT
                    id,
                    email,
                    username,
                    profilePictureUrl,
                    NULL,
                    statusMessage,
                    createdAt,
                    isCurrentUser
                FROM users
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TABLE chats_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    lastMessageId INTEGER,
                    remoteProfilePictureUrl TEXT,
                    localProfilePicturePath TEXT,
                    FOREIGN KEY(lastMessageId) REFERENCES messages(id) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO chats_new (
                    id,
                    name,
                    lastMessageId,
                    remoteProfilePictureUrl,
                    localProfilePicturePath
                )
                SELECT
                    id,
                    name,
                    lastMessageId,
                    profilePictureUrl,
                    NULL
                FROM chats
                """.trimIndent(),
            )

            db.execSQL("DROP TABLE users")
            db.execSQL("ALTER TABLE users_new RENAME TO users")
            db.execSQL("DROP TABLE chats")
            db.execSQL("ALTER TABLE chats_new RENAME TO chats")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_chats_lastMessageId ON chats(lastMessageId)")
        }
    }

private val MIGRATION_7_8 =
    object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE chats
                ADD COLUMN type TEXT NOT NULL DEFAULT 'DIRECT'
                """.trimIndent(),
            )
        }
    }

private val MIGRATION_8_9 =
    object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE chats ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN remoteId TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_chats_remoteId ON chats(remoteId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_messages_remoteId ON messages(remoteId)")
        }
    }

private val MIGRATION_9_10 =
    object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS chat_read_state (
                    chatId INTEGER NOT NULL PRIMARY KEY,
                    unreadCount INTEGER NOT NULL DEFAULT 0,
                    firstUnreadMessageId INTEGER,
                    FOREIGN KEY(chatId) REFERENCES chats(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
        }
    }
