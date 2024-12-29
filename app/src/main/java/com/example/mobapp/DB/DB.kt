package com.example.mobapp.DB

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DBStranka::class, DBKotva::class, DBHodnota::class, DBHodnotaExtra::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DB : RoomDatabase() {
    abstract fun strankaDao(): StrankaDao
}