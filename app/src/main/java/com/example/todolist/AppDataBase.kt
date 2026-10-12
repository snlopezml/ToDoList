package com.example.todolist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TareaEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tareaDao(): TareaDao

    companion object {
        @Volatile
        private var INSTANCIA: AppDatabase? = null

        fun obtener(context: Context): AppDatabase {
            return INSTANCIA ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tareas.db"
                ).build()
                INSTANCIA = db
                db
            }
        }
    }
}