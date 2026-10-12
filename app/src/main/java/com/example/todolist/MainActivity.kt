package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.launch
import java.util.Calendar

// ---------- Colores fijos ----------
private val Marino = Color(0xFF1F2A44)
private val Azul = Color(0xFF3D7BFF)
private val Verde = Color(0xFF34C77B)

// ---------- Paleta de tema (claro / oscuro) ----------
data class Paleta(
    val fondo: Color,
    val superficie: Color,
    val texto: Color,
    val textoSuave: Color,
    val encabezado: Color,
    val borde: Color,
    val chipActivo: Color
)

private val PaletaClara = Paleta(
    fondo = Color(0xFFF4F3F0),
    superficie = Color.White,
    texto = Color(0xFF1F2A44),
    textoSuave = Color(0xFF8A8F98),
    encabezado = Color(0xFF1F2A44),
    borde = Color(0xFFCCD0D8),
    chipActivo = Color(0xFF1F2A44)
)

private val PaletaOscura = Paleta(
    fondo = Color(0xFF14161A),
    superficie = Color(0xFF1F2329),
    texto = Color(0xFFE8EAED),
    textoSuave = Color(0xFF9AA0A6),
    encabezado = Color(0xFF26324F),
    borde = Color(0xFF454B55),
    chipActivo = Color(0xFF3D7BFF)
)

val LocalPaleta = compositionLocalOf { PaletaClara }

// ---------- Avatares predeterminados ----------
private val avatares = listOf("🐥", "🐱", "🦊", "🐼", "🐨", "🐸", "🦄", "🐧")

// ---------- Secciones del menú ----------
enum class Seccion(val titulo: String, val emoji: String) {
    TAREAS("Mis tareas", "📝"),
    PERFIL("Perfil", "👤"),
    CONFIGURACION("Configuración", "⚙️")
}

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
            val sistemaOscuro = isSystemInDarkTheme()
            var oscuro by rememberSaveable { mutableStateOf(sistemaOscuro) }
            val paleta = if (oscuro) PaletaOscura else PaletaClara

            LaunchedEffect(oscuro) {
                val transparente = android.graphics.Color.TRANSPARENT
                val estilo = if (oscuro) SystemBarStyle.dark(transparente)
                else SystemBarStyle.light(transparente, transparente)
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
            }

            MaterialTheme(
                colorScheme = if (oscuro)
                    darkColorScheme(primary = Azul, background = paleta.fondo, surface = paleta.superficie)
                else
                    lightColorScheme(primary = Azul, background = paleta.fondo, surface = paleta.superficie)
            ) {
                CompositionLocalProvider(LocalPaleta provides paleta) {
                    PantallaPrincipal(
                        oscuro = oscuro,
                        onCambiarTema = { oscuro = !oscuro }
                    )
                }
            }
        }
    }
}

// ---------- Pantalla principal con menú lateral ----------
@Composable
fun PantallaPrincipal(
    oscuro: Boolean,
    onCambiarTema: () -> Unit,
    vm: TareaViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var seccion by rememberSaveable { mutableStateOf(Seccion.TAREAS) }
    var avatar by rememberSaveable { mutableStateOf("🐥") }
    var mostrarAvatares by remember { mutableStateOf(false) }

    val abrirMenu: () -> Unit = { scope.launch { drawerState.open() } }

    // El botón "atrás" del teléfono regresa a Mis tareas
    BackHandler(enabled = seccion != Seccion.TAREAS && drawerState.isClosed) {
        seccion = Seccion.TAREAS
    }

    if (mostrarAvatares) {
        SelectorAvatar(
            actual = avatar,
            onElegir = { avatar = it; mostrarAvatares = false },
            onCerrar = { mostrarAvatares = false }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuLateral(
                avatar = avatar,
                seccionActual = seccion,
                oscuro = oscuro,
                onCambiarTema = onCambiarTema,
                onAvatarClick = { mostrarAvatares = true },
                onSeccion = { nueva ->
                    seccion = nueva
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        when (seccion) {
            Seccion.TAREAS -> PantallaTareas(
                vm = vm,
                avatar = avatar,
                onMenu = abrirMenu,
                onPerfil = { seccion = Seccion.PERFIL }
            )
            Seccion.PERFIL -> PantallaPerfil(
                avatar = avatar,
                onMenu = abrirMenu,
                onElegirAvatar = { avatar = it }
            )
            Seccion.CONFIGURACION -> PantallaConfiguracion(
                oscuro = oscuro,
                onCambiarTema = onCambiarTema,
                avatar = avatar,
                onMenu = abrirMenu,
                onPerfil = { seccion = Seccion.PERFIL }
            )
        }
    }
}

// ---------- Menú lateral (navigation drawer) ----------
@Composable
fun MenuLateral(
    avatar: String,
    seccionActual: Seccion,
    oscuro: Boolean,
    onCambiarTema: () -> Unit,
    onAvatarClick: () -> Unit,
    onSeccion: (Seccion) -> Unit
) {
    val p = LocalPaleta.current
    ModalDrawerSheet(
        drawerContainerColor = p.superficie,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        // Cabecera con avatar
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(p.encabezado)
                .padding(20.dp)
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(avatar, fontSize = 40.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Invitado", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Toca tu avatar para cambiarlo",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        // Opciones del menú
        Seccion.entries.forEach { s ->
            NavigationDrawerItem(
                label = { Text(s.titulo, fontWeight = FontWeight.Medium) },
                icon = { Text(s.emoji, fontSize = 18.sp) },
                selected = s == seccionActual,
                onClick = { onSeccion(s) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = p.chipActivo.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent,
                    selectedTextColor = p.texto,
                    unselectedTextColor = p.texto
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider(
            color = p.borde.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        // Interruptor día / noche
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (oscuro) "Modo noche" else "Modo día",
                color = p.texto,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            InterruptorDiaNoche(oscuro, onCambiarTema)
        }

        Text(
            "Versión 1.0",
            color = p.textoSuave,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 24.dp, bottom = 24.dp)
        )
    }
}

// ---------- Interruptor creativo día / noche ----------
@Composable
fun InterruptorDiaNoche(oscuro: Boolean, onCambiar: () -> Unit) {
    val cielo by animateColorAsState(
        if (oscuro) Color(0xFF1A2340) else Color(0xFF8FD3FF), tween(400), label = "cielo"
    )
    val astro by animateColorAsState(
        if (oscuro) Color(0xFFE9ECF5) else Color(0xFFFFD34E), tween(400), label = "astro"
    )
    val desplazamiento by animateDpAsState(
        if (oscuro) 30.dp else 0.dp,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "deslizar"
    )

    Box(
        Modifier
            .width(66.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(cielo)
            .clickable { onCambiar() }
            .padding(3.dp)
    ) {
        // Decoración: estrellitas de noche o nube de día
        if (oscuro) {
            Text(
                "✦", color = Color.White.copy(alpha = 0.9f), fontSize = 8.sp,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp, bottom = 10.dp)
            )
            Text(
                "✦", color = Color.White.copy(alpha = 0.6f), fontSize = 6.sp,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp, top = 12.dp)
            )
        } else {
            Text(
                "☁️", fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)
            )
        }
        // La bolita: sol o luna
        Box(
            Modifier
                .offset(x = desplazamiento)
                .size(30.dp)
                .clip(CircleShape)
                .background(astro),
            contentAlignment = Alignment.Center
        ) {
            Text(if (oscuro) "🌙" else "☀️", fontSize = 14.sp)
        }
    }
}

// ---------- Barra superior con ☰ y avatar ----------
@Composable
fun BarraSuperior(titulo: String, avatar: String, onMenu: () -> Unit, onAvatar: () -> Unit) {
    val p = LocalPaleta.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(p.fondo)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable { onMenu() },
            contentAlignment = Alignment.Center
        ) {
            Text("☰", fontSize = 24.sp, color = p.texto)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            titulo,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = p.texto,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(p.superficie)
                .clickable { onAvatar() },
            contentAlignment = Alignment.Center
        ) {
            Text(avatar, fontSize = 22.sp)
        }
    }
}

// ---------- Pantalla: Mis tareas ----------
@Composable
fun PantallaTareas(
    vm: TareaViewModel,
    avatar: String,
    onMenu: () -> Unit,
    onPerfil: () -> Unit
) {
    val p = LocalPaleta.current
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
        containerColor = p.fondo,
        topBar = { BarraSuperior("Mis tareas", avatar, onMenu, onPerfil) },
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Encabezado(completadas, tareas.size) }
            item { FiltroCategorias(filtro) { filtro = it } }
            item {
                Text(
                    "Tus pendientes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.texto,
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

// ---------- Pantalla: Perfil (vista, sin login todavía) ----------
@Composable
fun PantallaPerfil(avatar: String, onMenu: () -> Unit, onElegirAvatar: (String) -> Unit) {
    val p = LocalPaleta.current
    Scaffold(
        containerColor = p.fondo,
        topBar = { BarraSuperior("Perfil", avatar, onMenu, {}) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(p.chipActivo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(avatar, fontSize = 64.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Invitado", color = p.texto, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "Inicia sesión para guardar tu perfil",
                color = p.textoSuave,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(24.dp))

            TarjetaConfig("Elige tu avatar") {
                Box(Modifier.padding(16.dp)) {
                    GrillaAvatares(avatar, onElegirAvatar)
                }
            }

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = {},
                enabled = false,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔒  Iniciar sesión · Próximamente")
            }
        }
    }
}

// ---------- Pantalla: Configuración (vista) ----------
@Composable
fun PantallaConfiguracion(
    oscuro: Boolean,
    onCambiarTema: () -> Unit,
    avatar: String,
    onMenu: () -> Unit,
    onPerfil: () -> Unit
) {
    val p = LocalPaleta.current
    var idioma by rememberSaveable { mutableStateOf("Español") }

    Scaffold(
        containerColor = p.fondo,
        topBar = { BarraSuperior("Configuración", avatar, onMenu, onPerfil) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TarjetaConfig("Apariencia") {
                FilaConfig("🌗", "Modo oscuro", if (oscuro) "Activado" else "Desactivado") {
                    InterruptorDiaNoche(oscuro, onCambiarTema)
                }
            }

            TarjetaConfig("Idioma") {
                FilaConfig("🌐", "Idioma de la app", idioma)
                Row(
                    Modifier.padding(start = 66.dp, end = 16.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Español" to "🇸🇻", "English" to "🇺🇸").forEach { (nombre, bandera) ->
                        val activo = nombre == idioma
                        val fondo by animateColorAsState(
                            if (activo) p.chipActivo else p.fondo, label = "idioma"
                        )
                        Text(
                            "$bandera $nombre",
                            color = if (activo) Color.White else p.texto,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(fondo)
                                .clickable { idioma = nombre }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            TarjetaConfig("Próximamente") {
                FilaConfig("🔔", "Notificaciones", "Disponible pronto") {
                    Switch(checked = false, onCheckedChange = null, enabled = false)
                }
                FilaConfig("⏰", "Recordatorios", "Disponible pronto") {
                    Switch(checked = false, onCheckedChange = null, enabled = false)
                }
            }

            TarjetaConfig("Acerca de") {
                FilaConfig("📱", "ToDoList", "Versión 1.0")
                FilaConfig("👩‍💻", "Desarrollado por", "Susan · Programación 3, UTEC")
            }
        }
    }
}

// ---------- Piezas reutilizables de Configuración ----------
@Composable
fun TarjetaConfig(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    val p = LocalPaleta.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            titulo,
            color = p.textoSuave,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = p.superficie),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = contenido)
        }
    }
}

@Composable
fun FilaConfig(
    emoji: String,
    titulo: String,
    subtitulo: String,
    extra: @Composable () -> Unit = {}
) {
    val p = LocalPaleta.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(p.fondo),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, color = p.texto, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitulo, color = p.textoSuave, fontSize = 12.sp)
        }
        extra()
    }
}

// ---------- Selector de avatar ----------
@Composable
fun GrillaAvatares(actual: String, onElegir: (String) -> Unit) {
    val p = LocalPaleta.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        avatares.chunked(4).forEach { fila ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                fila.forEach { emoji ->
                    val elegido = emoji == actual
                    val escala by animateFloatAsState(if (elegido) 1.12f else 1f, label = "avatar")
                    Box(
                        Modifier
                            .size(56.dp)
                            .scale(escala)
                            .clip(CircleShape)
                            .background(if (elegido) p.chipActivo.copy(alpha = 0.2f) else p.fondo)
                            .border(
                                2.dp,
                                if (elegido) p.chipActivo else Color.Transparent,
                                CircleShape
                            )
                            .clickable { onElegir(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 28.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SelectorAvatar(actual: String, onElegir: (String) -> Unit, onCerrar: () -> Unit) {
    val p = LocalPaleta.current
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = p.superficie,
        title = { Text("Elige tu avatar", color = p.texto, fontWeight = FontWeight.Bold) },
        text = { GrillaAvatares(actual, onElegir) },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        }
    )
}

// ---------- Encabezado con saludo, progreso y mascota ----------
@Composable
fun Encabezado(completadas: Int, total: Int) {
    val p = LocalPaleta.current
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
        colors = CardDefaults.cardColors(containerColor = p.encabezado),
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
    val p = LocalPaleta.current
    val opciones = listOf(Categoria("Todas", "✨", Color(0xFFFFF4C7))) + categorias
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(opciones) { cat ->
            val activa = cat.nombre == seleccion
            val fondo by animateColorAsState(if (activa) p.chipActivo else p.superficie, label = "chip")
            val texto by animateColorAsState(if (activa) Color.White else p.texto, label = "chipTexto")
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
    val p = LocalPaleta.current
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
        if (tarea.completada) p.textoSuave else p.texto, label = "texto"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = p.superficie),
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
                        if (tarea.completada) Verde else p.borde,
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
    val p = LocalPaleta.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌱", fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Text("Nada por aquí todavía", color = p.texto, fontWeight = FontWeight.SemiBold)
        Text("Escribe abajo tu primera tarea", color = p.textoSuave, fontSize = 13.sp)
    }
}

// ---------- Barra inferior para agregar ----------
@Composable
fun BarraAgregar(texto: String, onTexto: (String) -> Unit, onAgregar: () -> Unit) {
    val p = LocalPaleta.current
    val habilitado = texto.isNotBlank()
    val colorBoton by animateColorAsState(
        if (habilitado) Azul else p.borde, label = "boton"
    )
    val escala by animateFloatAsState(if (habilitado) 1f else 0.9f, label = "escalaBoton")

    Surface(
        color = p.superficie,
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
                placeholder = { Text("Quiero hacer...", color = p.textoSuave) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = p.fondo,
                    unfocusedContainerColor = p.fondo,
                    focusedTextColor = p.texto,
                    unfocusedTextColor = p.texto,
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