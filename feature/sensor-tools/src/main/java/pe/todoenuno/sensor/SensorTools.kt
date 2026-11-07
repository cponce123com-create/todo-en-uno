package pe.todoenuno.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

/** Brújula de norte magnético, depende de sensor de rotación o acelerómetro + magnetómetro. */
@Composable
fun CompassScreen() {
    val context = LocalContext.current
    val sensorManager = remember(context) { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotation = remember(sensorManager) { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val accelerometer = remember(sensorManager) { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val magnetometer = remember(sensorManager) { sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    val available = rotation != null || (accelerometer != null && magnetometer != null)
    var azimuth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(sensorManager, rotation, accelerometer, magnetometer) {
        val gravity = FloatArray(3)
        val magnetic = FloatArray(3)
        var hasGravity = false
        var hasMagnetic = false
        val matrix = FloatArray(9)
        val angles = FloatArray(3)
        fun update() {
            if (hasGravity && hasMagnetic && SensorManager.getRotationMatrix(matrix, null, gravity, magnetic)) {
                SensorManager.getOrientation(matrix, angles)
                azimuth = ((Math.toDegrees(angles[0].toDouble()) + 360.0) % 360.0).toFloat()
            }
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(matrix, event.values)
                    SensorManager.getOrientation(matrix, angles)
                    azimuth = ((Math.toDegrees(angles[0].toDouble()) + 360.0) % 360.0).toFloat()
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        event.values.copyInto(gravity, endIndex = 3)
                        hasGravity = true
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        event.values.copyInto(magnetic, endIndex = 3)
                        hasMagnetic = true
                    }
                    update()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (rotation != null) sensorManager.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_UI)
        else {
            accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Brújula magnética", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (!available) {
            Text("Tu dispositivo no tiene los sensores necesarios.")
        } else {
            Text("${azimuth.roundToInt()}° · ${cardinal(azimuth)}",
                style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
            val palette = MaterialTheme.colorScheme
            Canvas(Modifier.size(260.dp)) {
                val radius = size.minDimension / 2.3f
                val c = center
                drawCircle(palette.primary, radius)
                drawCircle(palette.primaryContainer, radius * .9f)
                val angle = (azimuth.toDouble() * PI / 180.0).toFloat()
                val dx = kotlin.math.sin(angle) * radius * .75f
                val dy = -kotlin.math.cos(angle) * radius * .75f
                drawLine(palette.error, Offset(c.x - dx, c.y - dy), Offset(c.x + dx, c.y + dy), strokeWidth = 14f)
                drawCircle(palette.onPrimaryContainer, radius = 11f)
            }
            Text("Norte magnético aproximado. Calibra moviendo el teléfono en forma de ocho.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp))
        }
    }
}

internal fun cardinal(degrees: Float): String {
    val sectors = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    return sectors[((degrees + 22.5f) / 45f).toInt().mod(8)]
}

/** Nivel de burbuja orientativo basado en la gravedad. */
@Composable
fun LevelScreen() {
    val context = LocalContext.current
    val sensorManager = remember(context) { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember(sensorManager) { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var xAngle by remember { mutableFloatStateOf(0f) }
    var yAngle by remember { mutableFloatStateOf(0f) }
    DisposableEffect(sensorManager, sensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()
                xAngle = Math.toDegrees(atan2(x, abs(z))).toFloat()
                yAngle = Math.toDegrees(atan2(y, abs(z))).toFloat()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nivel de burbuja", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (sensor == null) {
            Text("No hay acelerómetro disponible.")
        } else {
            val palette = MaterialTheme.colorScheme
            Canvas(Modifier.size(265.dp).padding(8.dp)) {
                val radius = size.minDimension * .42f
                drawCircle(palette.primary, radius)
                drawCircle(palette.primaryContainer, radius * .87f)
                drawCircle(palette.tertiary, radius * .19f, style = androidx.compose.ui.graphics.drawscope.Stroke(5f))
                drawLine(palette.outline, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 3f)
                drawLine(palette.outline, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), 3f)
                val bubble = Offset(
                    center.x + (xAngle / 35f).coerceIn(-1f, 1f) * radius * .72f,
                    center.y + (yAngle / 35f).coerceIn(-1f, 1f) * radius * .72f
                )
                drawCircle(palette.primary, radius * .16f, bubble)
                drawCircle(palette.tertiaryContainer, radius * .12f, bubble)
            }
            Text("Eje X: ${xAngle.roundToInt()}° · Eje Y: ${yAngle.roundToInt()}°",
                style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            Text("Medición orientativa. No sustituye un nivel profesional calibrado.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
