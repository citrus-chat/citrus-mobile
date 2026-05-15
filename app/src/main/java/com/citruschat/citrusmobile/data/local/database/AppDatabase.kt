package com.citruschat.citrusmobile.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.local.dao.MessageDao
import com.citruschat.citrusmobile.data.local.entity.ChatEntity
import com.citruschat.citrusmobile.data.local.entity.MessageEntity

@Database(
    entities = [
        MessageEntity::class,
        ChatEntity::class,
    ],
    version = 2,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    abstract fun chatDao(): ChatDao
}
