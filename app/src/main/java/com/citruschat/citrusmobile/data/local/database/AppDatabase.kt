package com.citruschat.citrusmobile.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.local.dao.MessageDao
import com.citruschat.citrusmobile.data.local.dao.UserDao
import com.citruschat.citrusmobile.data.local.entity.ChatEntity
import com.citruschat.citrusmobile.data.local.entity.ChatParticipantCrossRef
import com.citruschat.citrusmobile.data.local.entity.MessageEntity
import com.citruschat.citrusmobile.data.local.entity.UserEntity

@Database(
    entities = [
        MessageEntity::class,
        ChatEntity::class,
        ChatParticipantCrossRef::class,
        UserEntity::class,
    ],
    version = 8,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    abstract fun chatDao(): ChatDao

    abstract fun userDao(): UserDao
}
