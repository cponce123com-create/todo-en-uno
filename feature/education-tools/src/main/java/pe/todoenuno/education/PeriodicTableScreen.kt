package pe.todoenuno.education

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PeriodicTableScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedNumber by rememberSaveable { mutableStateOf(1) }
    val selected=PeriodicElements.byNumber(selectedNumber)!!
    Column(
        modifier=Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ) {
        Text("Tabla periódica",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("118 elementos · Selecciona un elemento para consultar su ficha.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value=query,onValueChange={query=it.take(40)},
            modifier=Modifier.fillMaxWidth(),
            singleLine=true,label={Text("Buscar símbolo, nombre o número")}
        )
        if(query.isNotBlank()) {
            val found=PeriodicElements.find(query)
            if(found.isEmpty()) Text("No encontramos elementos.")
            found.take(25).forEach { element ->
                ElevatedCard(
                    modifier=Modifier.fillMaxWidth().clickable{selectedNumber=element.atomicNumber}
                ) {
                    Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        ElementTile(element){selectedNumber=element.atomicNumber}
                        Column {
                            Text(element.name,style=MaterialTheme.typography.titleMedium)
                            Text("Número atómico: ${element.atomicNumber}")
                        }
                    }
                }
            }
        } else {
            Text("Desliza horizontalmente para ver los 18 grupos.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Column(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(3.dp)
            ) {
                Row(horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    (1..18).forEach { group ->
                        Box(Modifier.width(59.dp),contentAlignment=Alignment.Center) {
                            Text("$group",style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                for(period in 1..7) {
                    val periodElements=PeriodicElements.all.filter { it.period==period &&
                        it.family!=ElementFamily.LANTHANIDE && it.family!=ElementFamily.ACTINIDE }
                        .associateBy { it.group }
                    Row(horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                        for(group in 1..18) {
                            val element=periodElements[group]
                            if(element==null) Spacer(Modifier.width(59.dp))
                            else ElementTile(element){selectedNumber=element.atomicNumber}
                        }
                    }
                }
            }
            Text("Lantánidos",style=MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                PeriodicElements.all.filter{it.family==ElementFamily.LANTHANIDE}.forEach { element ->
                    ElementTile(element){selectedNumber=element.atomicNumber}
                }
            }
            Text("Actínidos",style=MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                PeriodicElements.all.filter{it.family==ElementFamily.ACTINIDE}.forEach { element ->
                    ElementTile(element){selectedNumber=element.atomicNumber}
                }
            }
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("${selected.symbol} · ${selected.name}",
                    style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                Text("Número atómico: ${selected.atomicNumber}")
                Text("Grupo: ${selected.group} · Período: ${selected.period}")
                Text("Familia: ${selected.family.label}")
                Text("Referencia educativa offline; los elementos superpesados pueden tener propiedades experimentales o aún no confirmadas.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Datos: nombres, símbolos, posiciones y familias. No se muestran masas o configuraciones electrónicas sin datos verificados.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ElementTile(element: Element,onClick:()->Unit) {
    val scheme=MaterialTheme.colorScheme
    val background=when(element.family) {
        ElementFamily.NOBLE->scheme.secondaryContainer
        ElementFamily.HALOGEN->scheme.tertiaryContainer
        ElementFamily.ALKALI->scheme.errorContainer
        ElementFamily.ALKALINE->scheme.primaryContainer
        ElementFamily.METALLOID->scheme.surfaceContainerHighest
        ElementFamily.LANTHANIDE->scheme.secondaryContainer
        ElementFamily.ACTINIDE->scheme.tertiaryContainer
        else->scheme.surfaceContainerHigh
    }
    val foreground=when(element.family) {
        ElementFamily.NOBLE,ElementFamily.LANTHANIDE->scheme.onSecondaryContainer
        ElementFamily.HALOGEN,ElementFamily.ACTINIDE->scheme.onTertiaryContainer
        ElementFamily.ALKALI->scheme.onErrorContainer
        ElementFamily.ALKALINE->scheme.onPrimaryContainer
        else->scheme.onSurface
    }
    Column(
        modifier=Modifier.width(59.dp).height(66.dp)
            .background(background,RoundedCornerShape(8.dp))
            .clickable(onClick=onClick)
            .semantics { contentDescription="${element.atomicNumber}: ${element.name}, ${element.family.label}" }
            .padding(4.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.SpaceBetween
    ) {
        Text("${element.atomicNumber}",style=MaterialTheme.typography.labelSmall,color=foreground)
        Text(element.symbol,fontSize=19.sp,fontWeight=FontWeight.Bold,color=foreground)
        Text(element.name,fontSize=6.sp,maxLines=1,color=foreground)
    }
}
