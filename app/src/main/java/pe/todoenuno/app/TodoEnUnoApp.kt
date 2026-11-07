package pe.todoenuno.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pe.todoenuno.catalog.ToolCatalog
import pe.todoenuno.catalog.ToolDefinition
import pe.todoenuno.utilities.ToolScreen

private val tabs = listOf("Inicio", "Herramientas", "Favoritos", "Ajustes")
private val tabSymbols = listOf("⌂", "▦", "★", "⚙")

@Composable
fun TodoEnUnoApp() {
    val context = LocalContext.current
    val favoritesStore = remember(context) { FavoritesStore(context.applicationContext) }
    val favorites by favoritesStore.favorites.collectAsState(initial = FavoritesStore.DEFAULT_FAVORITES)
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var openedId by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(openedId != null) { openedId = null }

    Scaffold(
        bottomBar = {
            if (openedId == null) {
                NavigationBar {
                    tabs.forEachIndexed { index, name ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index; query = "" },
                            icon = { Text(tabSymbols[index], fontSize = 21.sp) },
                            label = { Text(name, maxLines = 1) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (openedId != null) {
            val tool = ToolCatalog.find(openedId.orEmpty())
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { openedId = null }) { Text("‹ Volver") }
                    Text(
                        text = tool?.title ?: "Herramienta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                HorizontalDivider()
                Box(modifier = Modifier.fillMaxSize()) {
                    ToolScreen(openedId.orEmpty())
                }
            }
        } else if (tab == 3) {
            SettingsPanel(Modifier.fillMaxSize().padding(padding))
        } else {
            val tools = ToolCatalog.tools.filter { tool ->
                (tab != 2 || tool.id in favorites) &&
                    (query.isBlank() || (tool.title + " " + tool.description).contains(query.trim(), ignoreCase = true))
            }.let { result ->
                if (tab == 0 && query.isBlank()) result.sortedWith(compareByDescending<ToolDefinition> { it.id in favorites }.thenBy { it.title }) else result
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (tab == 0) item { WelcomeHeader() }
                item {
                    Text(
                        when (tab) { 1 -> "Todas las herramientas"; 2 -> "Tus favoritas"; else -> "Encuentra lo que necesitas" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Buscar una herramienta") },
                        placeholder = { Text("Ej. kilómetros, linterna...") },
                        shape = RoundedCornerShape(18.dp),
                    )
                }
                if (tools.isEmpty()) item {
                    Text(
                        if (tab == 2) "Aún no tienes herramientas favoritas. Marca una estrella en Herramientas." else "No encontramos herramientas con esa búsqueda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 28.dp)
                    )
                }
                items(tools, key = { it.id }) { tool ->
                    ToolCard(
                        tool = tool,
                        favorite = tool.id in favorites,
                        open = { openedId = tool.id },
                        toggle = { scope.launch { favoritesStore.toggle(tool.id) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeader() {
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(Color(0xFF114B5F), Color(0xFF127F7A))),
                shape = RoundedCornerShape(24.dp)
            ).padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("TODO EN UNO", color = Color(0xFFA8F0D8), style = MaterialTheme.typography.labelMedium)
            Text("Tus herramientas,\nen un solo lugar", color = Color.White, fontSize = 26.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Rápido · Privado · Sin conexión", color = Color(0xFFE1FFF6), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ToolCard(tool: ToolDefinition, favorite: Boolean, open: () -> Unit, toggle: () -> Unit) {
    Card(
        onClick = open,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) { Text(tool.emoji, fontSize = 27.sp) }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tool.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = toggle, contentPadding = PaddingValues(3.dp)) {
                Text(if (favorite) "★" else "☆", fontSize = 25.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SettingsPanel(modifier: Modifier = Modifier) {
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Todo en Uno · Versión 0.1.0", style = MaterialTheme.typography.titleMedium)
        Text("Tema claro y oscuro: sigue la configuración del dispositivo.")
        Text("Tus favoritos se guardan solo en este teléfono.")
        Text("Las herramientas incluidas funcionan sin conexión a internet.")
        Text("Desarrollado con Kotlin y Jetpack Compose.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
