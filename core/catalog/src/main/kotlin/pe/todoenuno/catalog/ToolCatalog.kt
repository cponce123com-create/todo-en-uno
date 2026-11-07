package pe.todoenuno.catalog

enum class ToolCategory(val title: String) {
    RAPIDAS("Rápidas"),
    CALCULO("Cálculo"),
    TIEMPO("Tiempo"),
    SENSORES("Sensores"),
    PRODUCTIVIDAD("Productividad"),
    MULTIMEDIA("Multimedia"),
    UBICACION("Ubicación"),
    FINANZAS("Finanzas"),
    SISTEMA("Sistema"),
    EDUCACION("Educación")
}

data class ToolDefinition(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val category: ToolCategory,
)

object ToolCatalog {
    val tools = listOf(
        ToolDefinition("flashlight", "Linterna", "Ilumina al instante", "🔦", ToolCategory.RAPIDAS),
        ToolDefinition("calculator", "Calculadora", "Operaciones rápidas", "🧮", ToolCategory.CALCULO),
        ToolDefinition("converter", "Conversores", "Longitud, masa y temperatura", "🔄", ToolCategory.CALCULO),
        ToolDefinition("stopwatch", "Cronómetro", "Tiempo con precisión", "⏱️", ToolCategory.TIEMPO),
        ToolDefinition("countdown", "Temporizador", "Cuenta regresiva en pantalla", "⏳", ToolCategory.TIEMPO),
        ToolDefinition("worldclock", "Reloj mundial", "Hora en distintas ciudades", "🌍", ToolCategory.TIEMPO),
        ToolDefinition("audio", "Reproductor de audio", "Música y audios en segundo plano", "🎧", ToolCategory.MULTIMEDIA),
        ToolDefinition("compass", "Brújula", "Orientación y puntos cardinales", "🧭", ToolCategory.SENSORES),
        ToolDefinition("level", "Nivel", "Inclinación de superficies", "📐", ToolCategory.SENSORES),
        ToolDefinition("qr", "Crear código QR", "Genera códigos sin conexión", "🔳", ToolCategory.PRODUCTIVIDAD),
        ToolDefinition("password", "Contraseñas", "Crea claves aleatorias seguras", "🔐", ToolCategory.PRODUCTIVIDAD),
        ToolDefinition("recorder", "Grabadora", "Graba y exporta audios locales", "🎙️", ToolCategory.MULTIMEDIA),
        ToolDefinition("currency", "Tipo de cambio", "Divisas con cotizaciones fechadas", "💱", ToolCategory.FINANZAS),
        ToolDefinition("gps", "GPS", "Coordenadas y velocidad", "📍", ToolCategory.UBICACION),
        ToolDefinition("ruler", "Regla", "Regla física calibrable en pantalla", "📏", ToolCategory.SENSORES),
        ToolDefinition("pomodoro", "Pomodoro", "Enfoque y descansos", "🍅", ToolCategory.TIEMPO),
        ToolDefinition("text", "Texto", "Contar, convertir y copiar", "📝", ToolCategory.PRODUCTIVIDAD),
        ToolDefinition("color", "Colores", "Paleta RGB y HEX", "🎨", ToolCategory.PRODUCTIVIDAD),
        ToolDefinition("finance", "Cálculos financieros", "Propinas, cuentas y préstamos", "💰", ToolCategory.FINANZAS),
        ToolDefinition("morse", "Código Morse", "Convierte textos y señales", "📡", ToolCategory.PRODUCTIVIDAD),
        ToolDefinition("device", "Información del equipo", "Batería, RAM, versión Android", "📱", ToolCategory.SISTEMA),
        ToolDefinition("mirror", "Espejo", "Cámara frontal", "🪞", ToolCategory.MULTIMEDIA),
        ToolDefinition("magnifier", "Lupa", "Ampliación con cámara trasera", "🔎", ToolCategory.MULTIMEDIA),
        ToolDefinition("qrscan", "Escanear QR", "Lee códigos QR sin conexión", "📷", ToolCategory.PRODUCTIVIDAD),
        ToolDefinition("sound", "Sonómetro relativo", "Señal del micrófono en dBFS", "🔊", ToolCategory.SENSORES),
        ToolDefinition("metronome", "Metrónomo", "Pulsos de audio ajustables", "🎼", ToolCategory.MULTIMEDIA),
        ToolDefinition("age", "Calcular edad", "Años, meses y días", "📅", ToolCategory.CALCULO),
        ToolDefinition("bmi", "IMC", "Índice de masa corporal orientativo", "⚖️", ToolCategory.CALCULO),
        ToolDefinition("network", "Información de red", "Conexión y DNS", "📶", ToolCategory.SISTEMA),
        ToolDefinition("solar", "Sol y Luna", "Amanecer, atardecer y fase lunar", "🌗", ToolCategory.UBICACION),
        ToolDefinition("magnetic", "Campo magnético", "Sensor en microteslas", "🧲", ToolCategory.SENSORES),
        ToolDefinition("tuner", "Afinador", "Detecta notas musicales", "🎸", ToolCategory.MULTIMEDIA),
        ToolDefinition("distance", "Distancia entre puntos", "Dos coordenadas, distancia recta", "📐", ToolCategory.UBICACION),
        ToolDefinition("periodic", "Tabla periódica", "118 elementos y sus familias", "⚛", ToolCategory.EDUCACION),
        ToolDefinition("physics", "Fórmulas de física", "Física básica con calculadora", "🧪", ToolCategory.EDUCACION),
        ToolDefinition("triangles", "Triángulos notables", "30°–60°–90°, 45° y 3–4–5", "📐", ToolCategory.EDUCACION),
        ToolDefinition("labels", "Etiquetas PDF", "Diseña etiquetas con QR y exporta A4", "🏷️", ToolCategory.PRODUCTIVIDAD),
        ToolDefinition("laserlevel", "Nivel con cámara", "Retícula láser virtual para alinear", "📸", ToolCategory.SENSORES),
    )

    fun find(id: String): ToolDefinition? = tools.firstOrNull { it.id == id }
}
