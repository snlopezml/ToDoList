package com.example.todolist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tareas")
data class TareaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val texto: String,
    val categoria: String,
    val completada: Boolean = false
)