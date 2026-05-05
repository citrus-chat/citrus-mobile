package com.citruschat.citrusmobile.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.citruschat.citrusmobile.data.local.dao.MessageDao
import com.citruschat.citrusmobile.data.local.entity.MessageEntity

@Database(entities = [MessageEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
