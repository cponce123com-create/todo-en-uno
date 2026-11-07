package pe.todoenuno.app

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pe.todoenuno.audio.AudioPlayerScreen
import pe.todoenuno.camera.CameraToolScreen
import pe.todoenuno.catalog.CatalogMode
import pe.todoenuno.catalog.ToolCatalog
import pe.todoenuno.catalog.ToolCategory
import pe.todoenuno.catalog.ToolDefinition
import pe.todoenuno.catalog.ToolFilters
import pe.todoenuno.finance.CurrencyScreen
import pe.todoenuno.recorder.SoundMeterScreen
import pe.todoenuno.recorder.TunerScreen
import pe.todoenuno.recorder.VoiceRecorderScreen
import pe.todoenuno.utilities.ToolScreen

private data class BottomTab(val name: String, val symbol: String, val route: Any)
private val bottomTabs = listOf(
    BottomTab("Inicio", "⌂", HomeRoute),
    BottomTab("Herramientas", "▦", AllToolsRoute),
    BottomTab("Favoritos", "★", FavoritesRoute),
    BottomTab("Ajustes", "⚙", SettingsRoute),
)

@Composable
fun TodoEnUnoApp() {
    val context = LocalContext.current
    val factory = remember(context) { HomeViewModel.factory(context.applicationContext) }
    val model: HomeViewModel = viewModel(factory = factory)
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val onRoot = destination.isRootDestination()

    Scaffold(
        bottomBar = {
            if (onRoot) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = when (tab.route) {
                                HomeRoute -> destination?.hasRoute<HomeRoute>() == true
                                AllToolsRoute -> destination?.hasRoute<AllToolsRoute>() == true
                                FavoritesRoute -> destination?.hasRoute<FavoritesRoute>() == true
                                else -> destination?.hasRoute<SettingsRoute>() == true
                            },
                            onClick = {
                                model.clearSearch()
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(tab.symbol, fontSize = 20.sp, modifier = Modifier.clearAndSetSemantics {}) },
                            label = { Text(tab.name) },
                            modifier = Modifier.semantics { contentDescription = tab.name }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = HomeRoute,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable<HomeRoute> {
                CatalogScreen(CatalogMode.HOME, model) { nav.navigate(ToolRoute(it)) }
            }
            composable<AllToolsRoute> {
                CatalogScreen(CatalogMode.ALL, model) { nav.navigate(ToolRoute(it)) }
            }
            composable<FavoritesRoute> {
                CatalogScreen(CatalogMode.FAVORITES, model) { nav.navigate(ToolRoute(it)) }
            }
            composable<SettingsRoute> { SettingsPanel() }
            composable<ToolRoute>(
                deepLinks = listOf(navDeepLink<ToolRoute>(basePath = "todoenuno://tool"))
            ) { backStackEntry ->
                val id = backStackEntry.toRoute<ToolRoute>().id
                ToolDetail(
                    id = id,
                    onBack = { nav.popBackStack() },
                    onInfo = { nav.navigate(ToolGuideRoute(id)) }
                )
            }
            composable<ToolGuideRoute> { backStackEntry ->
                val id = backStackEntry.toRoute<ToolGuideRoute>().id
                ToolGuideScreen(id = id, onBack = { nav.popBackStack() })
            }
        }
    }
}

private fun NavDestination?.isRootDestination(): Boolean =
    this?.hasRoute<HomeRoute>() == true ||
        this?.hasRoute<AllToolsRoute>() == true ||
        this?.hasRoute<FavoritesRoute>() == true ||
        this?.hasRoute<SettingsRoute>() == true

@Composable
private fun CatalogScreen(mode: CatalogMode, model: HomeViewModel, open: (String) -> Unit) {
    val favorites by model.favorites.collectAsState()
    val query by model.query.collectAsState()
    val category by model.category.collectAsState()
    val tools = ToolFilters.visible(ToolCatalog.tools, mode, query, category, favorites)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (mode == CatalogMode.HOME) item { WelcomeHeader() }
        item {
            Text(
                when (mode) {
                    CatalogMode.HOME -> "Encuentra lo que necesitas"
                    CatalogMode.ALL -> "Todas las herramientas"
                    CatalogMode.FAVORITES -> "Tus favoritas"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = model::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar una herramienta") },
                placeholder = { Text("Ej. kilómetros, linterna...") },
                shape = RoundedCornerShape(18.dp)
            )
        }
        if (mode == CatalogMode.ALL) item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = category == "Todas",
                    onClick = { model.onCategorySelected("Todas") },
                    label = { Text("Todas") }
                )
                ToolCategory.entries.forEach { item ->
                    FilterChip(
                        selected = category == item.name,
                        onClick = { model.onCategorySelected(item.name) },
                        label = { Text(item.title) }
                    )
                }
            }
        }
        if (tools.isEmpty()) item {
            Text(
                if (mode == CatalogMode.FAVORITES) "Aún no tienes herramientas favoritas o no coinciden con tu búsqueda."
                else "No encontramos herramientas con esa búsqueda.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 28.dp)
            )
        }
        if (mode == CatalogMode.ALL) {
            items(tools, key = { it.id }) { tool ->
                ToolCatalogCard(tool, tool.id in favorites, { open(tool.id) }) {
                    model.toggleFavorite(tool.id)
                }
            }
        } else {
            items(tools.chunked(3), key = { it.first().id }) { chunk ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    chunk.forEach { tool ->
                        ToolTile(
                            modifier = Modifier.weight(1f),
                            tool = tool,
                            favorite = tool.id in favorites,
                            open = { open(tool.id) },
                            toggle = { model.toggleFavorite(tool.id) }
                        )
                    }
                    repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (mode == CatalogMode.HOME && query.isBlank()) item {
                Text(
                    "Desliza a Herramientas para explorar el catálogo completo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ToolDetail(id: String, onBack: () -> Unit, onInfo: () -> Unit) {
    val tool = ToolCatalog.find(id)
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ Volver") }
            Text(
                tool?.title ?: "Herramienta no encontrada",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 5.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (tool != null) TextButton(onClick = onInfo) { Text("Información") }
        }
        HorizontalDivider()
        if (tool == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Esta herramienta no existe o aún no está disponible.")
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                when (id) {
                    "audio" -> AudioPlayerScreen()
                    "recorder" -> VoiceRecorderScreen()
                    "currency" -> CurrencyScreen()
                    "sound" -> SoundMeterScreen()
                    "tuner" -> TunerScreen()
                    "mirror", "magnifier", "qrscan" -> CameraToolScreen(id)
                    else -> ToolScreen(id)
                }
            }
        }
    }
}

@Composable
private fun ToolGuideScreen(id: String, onBack: () -> Unit) {
    val tool = ToolCatalog.find(id)
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("‹ Volver a la herramienta") }
        Text("Información de la herramienta", style = MaterialTheme.typography.headlineSmall)
        Text(tool?.title ?: "Herramienta no disponible", style = MaterialTheme.typography.titleLarge)
        Text(tool?.description ?: "No existe información para este identificador.")
        Text(
            "Los permisos se solicitan al usar las funciones que los requieren. Las mediciones mediante sensores " +
                "dependen de la precisión y compatibilidad de cada dispositivo.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WelcomeHeader() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer)),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("TODO EN UNO", color = colors.onPrimaryContainer, style = MaterialTheme.typography.labelMedium)
            Text(
                "Tus herramientas,\nen un solo lugar",
                color = colors.onPrimaryContainer,
                fontSize = 26.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "${ToolCatalog.tools.size} herramientas · Rápido · Privado",
                color = colors.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ToolTile(
    modifier: Modifier,
    tool: ToolDefinition,
    favorite: Boolean,
    open: () -> Unit,
    toggle: () -> Unit
) {
    Card(
        onClick = open,
        modifier = modifier.height(166.dp).semantics { contentDescription = "${tool.title}: ${tool.description}" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Tool3DIcon(tool)
            Text(
                tool.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            FavoriteButton(tool, favorite, toggle)
        }
    }
}

@Composable
private fun ToolCatalogCard(tool: ToolDefinition, favorite: Boolean, open: () -> Unit, toggle: () -> Unit) {
    Card(
        onClick = open,
        modifier = Modifier.semantics { contentDescription = "${tool.title}: ${tool.description}" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Tool3DIcon(tool)
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
            FavoriteButton(tool, favorite, toggle)
        }
    }
}

@Composable
private fun FavoriteButton(tool: ToolDefinition, favorite: Boolean, toggle: () -> Unit) {
    val description = if (favorite) "Quitar ${tool.title} de favoritos" else "Añadir ${tool.title} a favoritos"
    TextButton(
        onClick = toggle,
        modifier = Modifier.semantics { contentDescription = description },
        contentPadding = PaddingValues(3.dp)
    ) {
        Text(
            if (favorite) "★" else "☆",
            fontSize = 25.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clearAndSetSemantics {}
        )
    }
}

@Composable
private fun SettingsPanel() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Todo en Uno · Versión 0.4.0", style = MaterialTheme.typography.titleMedium)
        Text("Tema claro y oscuro: sigue la configuración del dispositivo.")
        Text("Tus favoritos se guardan solo en este teléfono.")
        Text("La mayoría de herramientas funciona sin conexión. Las divisas requieren Internet para actualizarse.")
        Text("Desarrollado con Kotlin y Jetpack Compose.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
