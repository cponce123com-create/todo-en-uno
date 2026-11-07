package pe.todoenuno.documents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LabelDesignerScreen() {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var title by rememberSaveable{mutableStateOf("Producto")}
    var subtitle by rememberSaveable{mutableStateOf("Descripción breve")}
    var price by rememberSaveable{mutableStateOf("S/ 19.90")}
    var code by rememberSaveable{mutableStateOf("SKU-001")}
    var quantity by rememberSaveable{mutableStateOf("24")}
    var theme by rememberSaveable{mutableStateOf(LabelTheme.COMMERCIAL)}
    var status by remember{mutableStateOf("")}
    var exporting by remember{mutableStateOf(false)}
    val count=quantity.toIntOrNull()?.takeIf{it in 1..96}
    val launcher=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if(uri==null) return@rememberLauncherForActivityResult
        val number=count?:return@rememberLauncherForActivityResult
        exporting=true
        val spec=LabelData(title,subtitle,price,code,number,theme)
        scope.launch{
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use{LabelPdf.write(it,spec)}
                        ?: error("El sistema no permitió crear el PDF.")
                }
            }.onSuccess {status="PDF exportado: $it página(s) A4."}
                .onFailure {status="No se pudo exportar: ${it.localizedMessage}"}
            exporting=false
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        Text("Generador de etiquetas",style=MaterialTheme.typography.headlineSmall,
            fontWeight=FontWeight.Bold)
        Text("Diseña una etiqueta y duplícala sobre hojas A4 listas para imprimir.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        @Composable fun field(label:String,value:String,onChanged:(String)->Unit){
            OutlinedTextField(value=value,onValueChange={onChanged(it.take(70))},
                modifier=Modifier.fillMaxWidth(),label={Text(label)},singleLine=true)
        }
        field("Nombre del producto",title){title=it}
        field("Subtítulo",subtitle){subtitle=it}
        field("Precio / texto destacado",price){price=it}
        field("Código para QR (opcional)",code){code=it}
        OutlinedTextField(value=quantity,onValueChange={quantity=it.filter(Char::isDigit).take(2)},
            modifier=Modifier.fillMaxWidth(),label={Text("Cantidad de etiquetas (1–96)")},
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
        Text("Plantilla",style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            LabelTheme.entries.forEach { option ->
                FilterChip(selected=theme==option,onClick={theme=option},label={Text(option.title)})
            }
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("Vista previa aproximada",style=MaterialTheme.typography.labelSmall)
                Text(title.ifBlank{"Producto"},style=MaterialTheme.typography.titleLarge,
                    fontWeight=FontWeight.Bold)
                Text(subtitle)
                Text(price,color=MaterialTheme.colorScheme.primary,
                    style=MaterialTheme.typography.titleMedium)
                if(code.isNotBlank())Text("QR: $code",style=MaterialTheme.typography.bodySmall)
                Text("Diseño ${theme.title} · 3 columnas × 8 filas por página",
                    style=MaterialTheme.typography.bodySmall)
            }
        }
        Button(
            enabled=!exporting && count!=null && title.isNotBlank(),
            onClick={status="";launcher.launch("Etiquetas-Todo-en-Uno.pdf")},
            modifier=Modifier.fillMaxWidth()
        ){Text(if(exporting)"Exportando…" else "Exportar etiquetas en PDF")}
        if(status.isNotBlank())Text(status,color=MaterialTheme.colorScheme.primary)
        Text("Exportación local mediante selector de archivos. No requiere permiso general de almacenamiento ni envía datos a Internet.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            style=MaterialTheme.typography.bodySmall)
    }
}
