package com.example.todolist

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @GET("api/tareas")
    suspend fun obtenerTodas(): List<TareaEntity>

    @POST("api/tareas")
    suspend fun crear(@Body tarea: TareaEntity): TareaEntity

    @PUT("api/tareas/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body tarea: TareaEntity): TareaEntity

    @DELETE("api/tareas/{id}")
    suspend fun borrar(@Path("id") id: Long)
}