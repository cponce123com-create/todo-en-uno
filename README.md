# Todo en Uno · Android

Aplicación Android nativa y modular en **Kotlin + Jetpack Compose + Material 3**, diseñada en español.

## Arquitectura: versión 0.5.0
- Navigation Compose con rutas tipadas (Kotlin Serialization) y Back Stack real.
- Deep links locales: `todoenuno://tool/flashlight` o `todoenuno://tool/calculator`.
- Pantalla de información como destino secundario (Herramienta → Información → Herramienta).
- `HomeViewModel` con `SavedStateHandle` para búsqueda y filtros; favoritos en DataStore.
- `gradle/libs.versions.toml` centraliza versiones de los módulos y plugins.
- Wrapper oficial de Gradle 8.13: `./gradlew` o `gradlew.bat`.
- CI ejecuta pruebas de todos los módulos Android, tests JVM, Lint y APK.
- Los gráficos 3D son decorativos para lectores de pantalla; las tarjetas y estrellas tienen etiquetas legibles.

## Herramientas heredadas de 0.4.0

**33 herramientas disponibles** en el catálogo y organizadas por categorías, con búsqueda, favoritos persistentes y portada de iconos vectoriales 3D en cuadrícula:

- Iluminación y sensores: linterna, brújula magnética, nivel de burbuja, regla calibrable, GPS y sonómetro **relativo** (dBFS).
- Cálculos: calculadora estándar/científica, conversores de 8 categorías, propinas, préstamos, edad e IMC orientativo.
- Tiempo: reloj mundial, cronómetro, temporizador, Pomodoro y metrónomo.
- Multimedia y cámara: reproductor de audio **en segundo plano** (Media3), grabadora de voz **en primer plano**, espejo, lupa y escáner QR sin conexión.
- Productividad: crear QR, generador de contraseñas, procesamiento de texto, colores RGB/HEX, Morse.
- Información y red: datos del dispositivo, estado de red y DNS, tipo de cambio multimoneda con tasas fechadas y caché offline.
- Nueva ronda: linterna SOS, intensidad compatible, Sol y Luna, campo magnético, afinador y distancia geodésica.

## Descargar APK

En GitHub, abre **Actions → Android CI → última ejecución con marca verde → Artifacts → todo-en-uno-debug-apk**.

El artefacto contiene `app-debug.apk`. Requiere Android 8.0 (API 26) o superior.

## Desarrollo

- `app`: navegación, inicio, temas, favoritos con DataStore.
- `core:catalog`: metadatos puros de herramientas.
- `feature:utilities`: utilidades pequeñas y sensores.
- `feature:audio`: Media3 MediaSessionService para reproducción de audio local en segundo plano.
- `feature:recorder`: captura de micrófono, exportación SAF y dBFS.
- `feature:finance`: consulta HTTPS de Frankfurter y caché fechada.
- `feature:camera`: cámara con CameraX y reconocimiento QR ZXing.
- `.github/workflows/android.yml`: pruebas unitarias y APK debug.

Para compilar localmente necesitas JDK 17 y Android SDK 36; usa el Wrapper incluido: `./gradlew :app:assembleDebug` (Linux/macOS) o `gradlew.bat :app:assembleDebug` (Windows).

## Privacidad y límites

Los permisos de cámara, micrófono y ubicación se solicitan cuando entras en la herramienta correspondiente. La grabadora guarda inicialmente archivos en almacenamiento privado y permite exportarlos de forma explícita. Solo la consulta de divisas requiere Internet; el reproductor utiliza archivos seleccionados por el usuario.

El sonómetro indica **dBFS relativo, no dB SPL calibrados**. El GPS y la regla dependen de la precisión de sensores y pantalla. La cotización de divisas es una tasa media de referencia, no el precio de compra/venta ni la cotización oficial BCRP. IMC y cálculos financieros son informativos.

El temporizador dispone de aviso opcional con WorkManager (puede demorarse), mientras que la grabadora y Pomodoro no mantienen un servicio propio de segundo plano. La **reproducción de audio** sí utiliza un servicio Media3 compatible con pantalla bloqueada.

## Estado de pruebas

El flujo CI ejecuta pruebas JVM y compila APK debug. Es necesario validar cámara, micrófono, segundo plano, GPS y diversos tamaños de pantalla en equipos Android reales.

Consulta [ROADMAP](docs/ROADMAP.md) para las funciones pendientes y el plan de pruebas.
