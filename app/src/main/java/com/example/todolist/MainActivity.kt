package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar

// ---------- Colores ----------
private val Fondo = Color(0xFFF4F3F0)
private val Marino = Color(0xFF1F2A44)
private val Azul = Color(0xFF3D7BFF)
private val GrisTexto = Color(0xFF8A8F98)
private val Verde = Color(0xFF34C77B)

// ---------- Datos de categoría ----------
data class Categoria(val nombre: String, val emoji: String, val color: Color)

private val categorias = listOf(
    Categoria("Personal", "🏠", Color(0xFFFFE3D3)),
    Categoria("Estudio", "📚", Color(0xFFDCE8FF)),
    Categoria("Trabajo", "💼", Color(0xFFE6DDFF)),
    Categoria("Compras", "🛒", Color(0xFFD9F5E5)),
    Categoria("Finanzas", "💰", Color(0xFFFFF0C2))
)

private fun categoriaDe(nombre: String) =
    categorias.firstOrNull { it.nombre == nombre } ?: categorias[0]

// ---------- Adivinador automático de categoría ----------
private val palabrasClave = mapOf(
    "Estudio" to listOf("tarea", "parcial", "programación", "programacion", "estudiar", "clase", "examen"),
    "Trabajo" to listOf("estatus", "status", "bandeja", "software", "reunión", "reunion", "informe", "ticket"),
    "Compras" to listOf("comprar", "botas", "mercado", "tienda", "súper", "super"),
    "Finanzas" to listOf("pagar", "recibo", "netflix", "disney", "vix", "claro", "deber", "deuda")
)

private fun adivinarCategoria(texto: String): String {
    val t = texto.lowercase()
    for ((categoria, palabras) in palabrasClave) {
        if (palabras.any { t.contains(it) }) return categoria
    }
    return "Personal"
}

// ---------- Actividad ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Azul,
                    background = Fondo,
                    surface = Color.White
                )
            ) {
                PantallaPrincipal()
            }
        }
    }
}

// ---------- Pantalla principal ----------
@Composable
fun PantallaPrincipal(vm: TareaViewModel = viewModel()) {
    val tareas by vm.tareas.collectAsState()
    var filtro by remember { mutableStateOf("Todas") }
    var textoNuevo by remember { mutableStateOf("") }

    val visibles = if (filtro == "Todas") tareas
    else tareas.filter { it.categoria == filtro }
    val completadas = tareas.count { it.completada }

    fun agregar() {
        if (textoNuevo.isNotBlank()) {
            val categoria = if (filtro == "Todas") adivinarCategoria(textoNuevo)
            else filtro
            vm.agregar(texto = textoNuevo.trim(), categoria = categoria)
            textoNuevo = ""
        }
    }

    Scaffold(
        containerColor = Fondo,
        bottomBar = {
            BarraAgregar(
                texto = textoNuevo,
                onTexto = { textoNuevo = it },
                onAgregar = { agregar() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Encabezado(completadas, tareas.size) }
            item { FiltroCategorias(filtro) { filtro = it } }
            item {
                Text(
                    "Mis tareas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Marino,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (visibles.isEmpty()) {
                item { EstadoVacio() }
            }
            items(visibles, key = { it.id }) { tarea ->
                TarjetaTarea(
                    tarea = tarea,
                    onToggle = { vm.alternar(tarea) },
                    onBorrar = { vm.borrar(tarea) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

// ---------- Encabezado con saludo, progreso y mascota ----------
@Composable
fun Encabezado(completadas: Int, total: Int) {
    val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val saludo = when {
        hora < 12 -> "¡Buenos días!"
        hora < 19 -> "¡Buenas tardes!"
        else -> "¡Buenas noches!"
    }
    val progreso = if (total == 0) 0f else completadas.toFloat() / total
    val progresoAnimado by animateFloatAsState(progreso, label = "progreso")

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Marino),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(saludo, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "¿Qué vamos a lograr hoy?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progresoAnimado },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color(0xFF7FE3B0),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$completadas de $total completadas",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Mascota(progreso, total)
        }
    }
}

// ---------- Mascota que brinca ----------
@Composable
fun Mascota(progreso: Float, total: Int) {
    val (emoji, frase) = when {
        total == 0 -> "🐣" to "¡Agrega algo!"
        progreso >= 1f -> "🥳" to "¡Lo lograste!"
        progreso >= 0.5f -> "🐥" to "¡Ya casi!"
        else -> "🐤" to "¡Tú puedes!"
    }
    val transicion = rememberInfiniteTransition(label = "mascota")
    val salto by transicion.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            tween(650, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "salto"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(frase, fontSize = 11.sp, color = Marino, fontWeight = FontWeight.SemiBold)
        }
        Text(
            emoji,
            fontSize = 48.sp,
            modifier = Modifier.graphicsLayer { translationY = salto * density }
        )
    }
}

// ---------- Filtro de categorías ----------
@Composable
fun FiltroCategorias(seleccion: String, onSeleccion: (String) -> Unit) {
    val opciones = listOf(Categoria("Todas", "✨", Color(0xFFFFF4C7))) + categorias
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(opciones) { cat ->
            val activa = cat.nombre == seleccion
            val fondo by animateColorAsState(if (activa) Marino else Color.White, label = "chip")
            val texto by animateColorAsState(if (activa) Color.White else Marino, label = "chipTexto")
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(fondo)
                    .clickable { onSeleccion(cat.nombre) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(cat.color),
                    contentAlignment = Alignment.Center
                ) { Text(cat.emoji, fontSize = 14.sp) }
                Spacer(Modifier.width(8.dp))
                Text(cat.nombre, color = texto, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ---------- Tarjeta de tarea ----------
@Composable
fun TarjetaTarea(
    tarea: TareaEntity,
    onToggle: () -> Unit,
    onBorrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cat = categoriaDe(tarea.categoria)
    val colorCheck by animateColorAsState(
        if (tarea.completada) Verde else Color.Transparent, label = "check"
    )
    val escala by animateFloatAsState(
        if (tarea.completada) 1.15f else 1f,
        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "escala"
    )
    val colorTexto by animateColorAsState(
        if (tarea.completada) GrisTexto else Marino, label = "texto"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .scale(escala)
                    .clip(CircleShape)
                    .background(colorCheck)
                    .border(
                        2.dp,
                        if (tarea.completada) Verde else Color(0xFFCCD0D8),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (tarea.completada) {
                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tarea.texto,
                    color = colorTexto,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (tarea.completada) TextDecoration.LineThrough
                    else TextDecoration.None
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${cat.emoji} ${cat.nombre}",
                    fontSize = 12.sp,
                    color = Marino,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(cat.color)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            Text(
                "🗑️",
                fontSize = 20.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onBorrar() }
                    .padding(8.dp)
            )
        }
    }
}

// ---------- Estado vacío ----------
@Composable
fun EstadoVacio() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌱", fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Text("Nada por aquí todavía", color = Marino, fontWeight = FontWeight.SemiBold)
        Text("Escribe abajo tu primera tarea", color = GrisTexto, fontSize = 13.sp)
    }
}

// ---------- Barra inferior para agregar ----------
@Composable
fun BarraAgregar(texto: String, onTexto: (String) -> Unit, onAgregar: () -> Unit) {
    val habilitado = texto.isNotBlank()
    val colorBoton by animateColorAsState(
        if (habilitado) Azul else Color(0xFFCCD0D8), label = "boton"
    )
    val escala by animateFloatAsState(if (habilitado) 1f else 0.9f, label = "escalaBoton")

    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = texto,
                onValueChange = onTexto,
                placeholder = { Text("Quiero hacer...") },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Fondo,
                    unfocusedContainerColor = Fondo,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAgregar() }),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .size(52.dp)
                    .scale(escala)
                    .clip(CircleShape)
                    .background(colorBoton)
                    .clickable(enabled = habilitado) { onAgregar() },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}