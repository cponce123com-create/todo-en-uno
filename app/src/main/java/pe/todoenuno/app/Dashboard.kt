package pe.todoenuno.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.todoenuno.catalog.ToolCatalog
import pe.todoenuno.catalog.ToolCategory
import pe.todoenuno.catalog.ToolDefinition

private val defaultQuickAccess = listOf(
    "flashlight","calculator","recorder","compass","qr","countdown"
)

private fun parseAccess(value: String?): List<String> {
    val items=value.orEmpty().split(',').filter { ToolCatalog.find(it)!=null }.distinct().take(6)
    return if(items.isEmpty()) defaultQuickAccess else items
}

/** Pantalla de inicio inspirada en el diseño premium solicitado, sin imágenes ni internet. */
@Composable
fun HomeDashboard(
    model: HomeViewModel,
    onOpen: (String)->Unit,
    onAll: ()->Unit,
    onCategories: ()->Unit
) {
    val context=LocalContext.current
    val prefs=remember(context){context.getSharedPreferences("dashboard",Context.MODE_PRIVATE)}
    var quickIds by remember { mutableStateOf(parseAccess(prefs.getString("quick_access",null))) }
    var editing by rememberSaveable{mutableStateOf(false)}
    val query by model.query.collectAsState()
    val favorites by model.favorites.collectAsState()
    val results=remember(query) {
        if(query.isBlank()) emptyList()
        else ToolCatalog.tools.filter { tool ->
            tool.title.contains(query.trim(),ignoreCase=true) ||
                tool.description.contains(query.trim(),ignoreCase=true)
        }
    }
    LazyColumn(
        modifier=Modifier.fillMaxSize(),
        contentPadding=androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ) {
        item {
            val scheme=MaterialTheme.colorScheme
            Box(
                Modifier.fillMaxWidth().height(172.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF15365E),Color(0xFF112344),Color(0xFF332459))
                        ),
                        RoundedCornerShape(26.dp)
                    )
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val w=size.width
                    val h=size.height
                    val sun=Offset(w*0.78f,h*0.30f)
                    drawCircle(
                        brush=Brush.radialGradient(
                            colors=listOf(Color(0xFFFFDE94).copy(alpha=.90f),
                                Color(0xFFF8AA78).copy(alpha=.45f),
                                Color.Transparent),
                            center=sun,
                            radius=h*0.50f
                        ),
                        radius=h*0.50f,center=sun
                    )
                    val distant=Path().apply {
                        moveTo(0f,h*.78f)
                        lineTo(w*.13f,h*.50f)
                        lineTo(w*.24f,h*.60f)
                        lineTo(w*.41f,h*.25f)
                        lineTo(w*.55f,h*.58f)
                        lineTo(w*.75f,h*.39f)
                        lineTo(w,h*.71f)
                        lineTo(w,h)
                        lineTo(0f,h)
                        close()
                    }
                    drawPath(distant,Color(0xFF6D7EAC).copy(alpha=.72f))
                    val foreground=Path().apply {
                        moveTo(0f,h*.90f)
                        lineTo(w*.18f,h*.66f)
                        lineTo(w*.30f,h*.76f)
                        lineTo(w*.50f,h*.57f)
                        lineTo(w*.67f,h*.85f)
                        lineTo(w*.86f,h*.62f)
                        lineTo(w,h*.82f)
                        lineTo(w,h)
                        lineTo(0f,h)
                        close()
                    }
                    drawPath(foreground,Color(0xFF153847).copy(alpha=.80f))
                    drawRect(
                        Brush.verticalGradient(
                            colors=listOf(Color(0x4407152C),Color(0xBB0C1434))
                        )
                    )
                }
                Column(
                    Modifier.fillMaxSize().padding(22.dp),
                    verticalArrangement=Arrangement.Center,
                    horizontalAlignment=Alignment.CenterHorizontally
                ) {
                    Tool3DIcon(ToolCatalog.find("calculator")!!)
                    Text("Todo en Uno",fontSize=30.sp,color=Color.White,fontWeight=FontWeight.ExtraBold)
                    Text("${ToolCatalog.tools.size} herramientas en tu bolsillo",
                        color=Color(0xFFDDDEF5),style=MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            OutlinedTextField(
                value=query,onValueChange=model::onQueryChanged,
                label={Text("Buscar herramientas")},
                placeholder={Text("Ej. brújula, calculadora, química…")},
                modifier=Modifier.fillMaxWidth().testTag("tool_search"),
                shape=RoundedCornerShape(24.dp),singleLine=true
            )
        }
        if(query.isNotBlank()) {
            if(results.isEmpty()) item { Text("No encontramos coincidencias.") }
            items(results,key={it.id}){ tool ->
                ElevatedCard(
                    modifier=Modifier.fillMaxWidth().testTag("tool_${tool.id}"),
                    onClick={onOpen(tool.id)}
                ) {
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Tool3DIcon(tool)
                        Column {
                            Text(tool.title,fontWeight=FontWeight.Bold)
                            Text(tool.description,color=MaterialTheme.colorScheme.onSurfaceVariant,
                                style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                    verticalAlignment=Alignment.CenterVertically) {
                    Text("⚡ Accesos rápidos",style=MaterialTheme.typography.titleMedium,
                        fontWeight=FontWeight.Bold)
                    TextButton(onClick={editing=!editing}) {
                        Text(if(editing)"Terminar" else "Personalizar")
                    }
                }
            }
            if(editing) {
                item {
                    Text("Elige hasta seis accesos. Se guardan en este teléfono.",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Column {
                        ToolCatalog.tools.forEach { tool ->
                            FilterChip(
                                selected=tool.id in quickIds,
                                onClick={
                                    quickIds=if(tool.id in quickIds) quickIds.filterNot{it==tool.id}
                                    else if(quickIds.size<6) quickIds+tool.id else quickIds
                                    prefs.edit().putString("quick_access",quickIds.joinToString(",")).apply()
                                },
                                label={Text(tool.title)}
                            )
                        }
                    }
                }
            } else {
                items(quickIds.mapNotNull{ToolCatalog.find(it)}.chunked(3),
                    key={it.firstOrNull()?.id ?: "empty"}) { chunk ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        chunk.forEach { tool ->
                            Card(
                                onClick={onOpen(tool.id)},
                                modifier=Modifier.weight(1f).height(126.dp).testTag("tool_${tool.id}"),
                                colors=CardDefaults.cardColors(
                                    containerColor=MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                shape=RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    Modifier.fillMaxSize().padding(6.dp),
                                    horizontalAlignment=Alignment.CenterHorizontally,
                                    verticalArrangement=Arrangement.SpaceEvenly
                                ) {
                                    Tool3DIcon(tool)
                                    Text(tool.title,style=MaterialTheme.typography.labelSmall,
                                        fontWeight=FontWeight.SemiBold,maxLines=2,
                                        overflow=TextOverflow.Ellipsis,textAlign=TextAlign.Center)
                                }
                            }
                        }
                        repeat(3-chunk.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                    verticalAlignment=Alignment.CenterVertically) {
                    Text("Explorar por categoría",style=MaterialTheme.typography.titleMedium,
                        fontWeight=FontWeight.Bold)
                    TextButton(onClick=onCategories) { Text("Ver todas ›") }
                }
            }
            items(ToolCategory.entries.chunked(2),key={it.first().name}) { chunk ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    chunk.forEach { category ->
                        CategoryMiniCard(
                            category=category,
                            modifier=Modifier.weight(1f),
                            onClick={
                                model.onCategorySelected(category.name)
                                onAll()
                            }
                        )
                    }
                    repeat(2-chunk.size){Spacer(Modifier.weight(1f))}
                }
            }
            item {
                TextButton(onClick=onAll,modifier=Modifier.fillMaxWidth()) {
                    Text("Explorar las ${ToolCatalog.tools.size} herramientas")
                }
            }
        }
    }
}

@Composable
private fun CategoryMiniCard(category:ToolCategory,modifier:Modifier=Modifier,onClick:()->Unit) {
    val count=ToolCatalog.tools.count{it.category==category}
    val example=ToolCatalog.tools.firstOrNull{it.category==category}
    ElevatedCard(onClick=onClick,modifier=modifier.height(108.dp),
        shape=RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxSize().padding(10.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            if(example!=null) Tool3DIcon(example)
            Column {
                Text(category.title,style=MaterialTheme.typography.labelMedium,
                    fontWeight=FontWeight.Bold,maxLines=2)
                Text("$count herramientas",style=MaterialTheme.typography.labelSmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CategoryExploreScreen(
    model:HomeViewModel,onBack:()->Unit,onSelect:(ToolCategory)->Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        item{
            TextButton(onClick=onBack){Text("‹ Volver")}
            Text("Categorías",style=MaterialTheme.typography.headlineMedium,
                fontWeight=FontWeight.Bold)
        }
        items(ToolCategory.entries,key={it.name}) { category ->
            val count=ToolCatalog.tools.count {it.category==category}
            val sample=ToolCatalog.tools.first {it.category==category}
            ElevatedCard(
                onClick={onSelect(category)},
                modifier=Modifier.fillMaxWidth().height(96.dp),
                shape=RoundedCornerShape(22.dp)
            ) {
                Row(Modifier.fillMaxSize().padding(12.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                    Tool3DIcon(sample)
                    Column(Modifier.weight(1f)) {
                        Text(category.title,style=MaterialTheme.typography.titleMedium,
                            fontWeight=FontWeight.Bold)
                        Text("$count herramientas",
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("›",fontSize=28.sp)
                }
            }
        }
    }
}
