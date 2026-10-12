package com.example.todolist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {

    @Query("SELECT * FROM tareas ORDER BY id DESC")
    fun obtenerTodas(): Flow<List<TareaEntity>>

    @Insert
    suspend fun insertar(tarea: TareaEntity)

    @Update
    suspend fun actualizar(tarea: TareaEntity)

    @Delete
    suspend fun borrar(tarea: TareaEntity)
}