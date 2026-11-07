package pe.todoenuno.education

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.MathContext

data class PhysicsFormula(
    val id: String,
    val title: String,
    val expression: String,
    val variableA: String,
    val variableB: String,
    val resultName: String,
    val explanation: String,
    val unit: String,
)

object PhysicsCatalog {
    val formulas = listOf(
        PhysicsFormula("speed","Velocidad media","v = d / t","Distancia d (m)","Tiempo t (s)","Velocidad v","Distancia recorrida dividida entre tiempo.","m/s"),
        PhysicsFormula("acceleration","Aceleración media","a = Δv / Δt","Cambio de velocidad Δv (m/s)","Tiempo Δt (s)","Aceleración a","Variación de velocidad por unidad de tiempo.","m/s²"),
        PhysicsFormula("force","Segunda ley de Newton","F = m · a","Masa m (kg)","Aceleración a (m/s²)","Fuerza F","Fuerza neta de un cuerpo.","N"),
        PhysicsFormula("work","Trabajo mecánico","W = F · d","Fuerza F (N)","Desplazamiento d (m)","Trabajo W","Supone fuerza paralela al desplazamiento.","J"),
        PhysicsFormula("kinetic","Energía cinética","Eₖ = ½ · m · v²","Masa m (kg)","Velocidad v (m/s)","Energía Eₖ","Energía de movimiento (modelo clásico).","J"),
        PhysicsFormula("potential","Energía potencial gravitatoria","Eₚ = m · g · h","Masa m (kg)","Altura h (m)","Energía Eₚ","Usa g = 9.81 m/s², cerca de la superficie terrestre.","J"),
        PhysicsFormula("power","Potencia mecánica","P = W / t","Trabajo W (J)","Tiempo t (s)","Potencia P","Rapidez de realización de trabajo.","W"),
        PhysicsFormula("density","Densidad","ρ = m / V","Masa m (kg)","Volumen V (m³)","Densidad ρ","Masa por volumen.","kg/m³"),
        PhysicsFormula("pressure","Presión","p = F / A","Fuerza F (N)","Área A (m²)","Presión p","Fuerza perpendicular distribuida en una superficie.","Pa"),
        PhysicsFormula("ohm","Ley de Ohm","V = I · R","Corriente I (A)","Resistencia R (Ω)","Voltaje V","Aproximación de materiales óhmicos.","V"),
        PhysicsFormula("electric","Potencia eléctrica","P = V · I","Tensión V (V)","Corriente I (A)","Potencia P","En corriente continua o con factor de potencia apropiado.","W"),
        PhysicsFormula("frequency","Frecuencia","f = 1 / T","Período T (s)","Factor (introducir 1)","Frecuencia f","Inverso del período. El segundo campo debe valer 1.","Hz"),
    )

    fun solve(id: String,a: Double,b: Double): Result<Double> = runCatching {
        require(a.isFinite() && b.isFinite()) { "Introduce valores finitos." }
        val value=when(id) {
            "speed","acceleration","power","density","pressure" -> {
                require(b!=0.0) { "El divisor no puede ser cero." }
                a/b
            }
            "force","work","ohm","electric" -> a*b
            "kinetic" -> 0.5*a*b*b
            "potential" -> a*9.81*b
            "frequency" -> {
                require(a>0) { "El período debe ser positivo." }
                1/a
            }
            else -> error("Fórmula desconocida")
        }
        require(value.isFinite()) { "Resultado fuera de rango." }
        value
    }
}

@Composable
fun PhysicsToolsScreen() {
    var current by rememberSaveable{mutableStateOf("speed")}
    var a by rememberSaveable{mutableStateOf("100")}
    var b by rememberSaveable{mutableStateOf("10")}
    val selected=PhysicsCatalog.formulas.first{it.id==current}
    val result=runCatching {
        PhysicsCatalog.solve(current,a.replace(',','.').toDouble(),b.replace(',','.').toDouble()).getOrThrow()
    }.getOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Fórmulas básicas de física",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("12 fórmulas con calculadora y explicación. Sistema Internacional (SI).",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        PhysicsCatalog.formulas.forEach { formula ->
            FilterChip(
                selected=formula.id==current,
                onClick={
                    current=formula.id
                    a="";b=""
                },
                label={Text(formula.title,style=MaterialTheme.typography.bodyMedium)},
                modifier=Modifier.fillMaxWidth()
            )
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(selected.title,style=MaterialTheme.typography.titleLarge)
                Text(selected.expression,style=MaterialTheme.typography.headlineMedium,
                    color=MaterialTheme.colorScheme.primary)
                Text(selected.explanation)
                OutlinedTextField(
                    value=a,onValueChange={a=it.take(25)},
                    modifier=Modifier.fillMaxWidth(),label={Text(selected.variableA)},
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                    singleLine=true
                )
                OutlinedTextField(
                    value=b,onValueChange={b=it.take(25)},
                    modifier=Modifier.fillMaxWidth(),label={Text(selected.variableB)},
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                    singleLine=true
                )
                Text(
                    if(result==null) "Introduce valores válidos."
                    else "${selected.resultName}: ${BigDecimal.valueOf(result).round(MathContext(8)).stripTrailingZeros().toPlainString()} ${selected.unit}",
                    style=MaterialTheme.typography.titleLarge,
                    color=MaterialTheme.colorScheme.primary
                )
            }
        }
        Text("Resultados educativos aproximados. Verifica hipótesis, unidades y convenciones antes de utilizarlos en trabajos profesionales.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
    }
}
