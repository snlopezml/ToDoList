package com.example.todolist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TareaViewModel(app: Application) : AndroidViewModel(app) {

    // Antes: el DAO de Room. Ahora: la API del backend por Retrofit
    private val api = RetrofitClient.api

    // Lista de tareas que la pantalla observa
    private val _tareas = MutableStateFlow<List<TareaEntity>>(emptyList())
    val tareas: StateFlow<List<TareaEntity>> = _tareas.asStateFlow()

    // Al crear el ViewModel, pide las tareas al backend
    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            try {
                _tareas.value = api.obtenerTodas()
            } catch (e: Exception) {
                Log.e("TareaViewModel", "Error al cargar tareas", e)
            }
        }
    }

    fun agregar(texto: String, categoria: String) {
        viewModelScope.launch {
            try {
                api.crear(TareaEntity(texto = texto, categoria = categoria))
                cargar()
            } catch (e: Exception) {
                Log.e("TareaViewModel", "Error al agregar tarea", e)
            }
        }
    }

    fun alternar(tarea: TareaEntity) {
        viewModelScope.launch {
            try {
                api.actualizar(tarea.id.toLong(), tarea.copy(completada = !tarea.completada))
                cargar()
            } catch (e: Exception) {
                Log.e("TareaViewModel", "Error al actualizar tarea", e)
            }
        }
    }

    fun borrar(tarea: TareaEntity) {
        viewModelScope.launch {
            try {
                api.borrar(tarea.id.toLong())
                cargar()
            } catch (e: Exception) {
                Log.e("TareaViewModel", "Error al borrar tarea", e)
            }
        }
    }
}