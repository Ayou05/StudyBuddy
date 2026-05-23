package com.studybuddy.v2.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.studybuddy.v2.data.model.FocusSession
import com.studybuddy.v2.data.model.Pet

@Database(entities = [FocusSession::class, Pet::class], version = 3, exportSchema = false)
abstract class V2Database : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun petDao(): PetDao

    companion object {
        fun build(context: Context): V2Database = Room.databaseBuilder(
            context.applicationContext, V2Database::class.java, "studybuddy_v2.db"
        )
            // 数据库纯本地缓存，PB 是 source of truth，破坏直接清掉重建
            .fallbackToDestructiveMigration()
            .build()
    }
}
