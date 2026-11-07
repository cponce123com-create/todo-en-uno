# Todo en Uno · Android

Aplicación Android nativa y modular en **Kotlin + Jetpack Compose + Material 3**, diseñada en español.

## Versión 0.3.0

**29 herramientas disponibles** en el catálogo y organizadas por categorías, con búsqueda, favoritos persistentes y portada de iconos vectoriales 3D en cuadrícula:

- Iluminación y sensores: linterna, brújula magnética, nivel de burbuja, regla calibrable, GPS y sonómetro **relativo** (dBFS).
- Cálculos: calculadora estándar/científica, conversores de 8 categorías, propinas, préstamos, edad e IMC orientativo.
- Tiempo: reloj mundial, cronómetro, temporizador, Pomodoro y metrónomo.
- Multimedia y cámara: reproductor de audio **en segundo plano** (Media3), grabadora de voz **en primer plano**, espejo, lupa y escáner QR sin conexión.
- Productividad: crear QR, generador de contraseñas, procesamiento de texto, colores RGB/HEX, Morse.
- Información y red: datos del dispositivo, estado de red y DNS, tipo de cambio multimoneda con tasas fechadas y caché offline.

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

Para compilar localmente se necesita JDK 17, Android SDK 36 y Gradle 8.13. El Gradle Wrapper aún no se ha añadido.

## Privacidad y límites

Los permisos de cámara, micrófono y ubicación se solicitan cuando entras en la herramienta correspondiente. La grabadora guarda inicialmente archivos en almacenamiento privado y permite exportarlos de forma explícita. Solo la consulta de divisas requiere Internet; el reproductor utiliza archivos seleccionados por el usuario.

El sonómetro indica **dBFS relativo, no dB SPL calibrados**. El GPS y la regla dependen de la precisión de sensores y pantalla. La cotización de divisas es una tasa media de referencia, no el precio de compra/venta ni la cotización oficial BCRP. IMC y cálculos financieros son informativos.

La grabadora, el temporizador y el Pomodoro no mantienen su propia captura o cuenta atrás como servicio de segundo plano. La **reproducción de audio** sí utiliza un servicio Media3 compatible con pantalla bloqueada.

## Estado de pruebas

El flujo CI ejecuta pruebas JVM y compila APK debug. Es necesario validar cámara, micrófono, segundo plano, GPS y diversos tamaños de pantalla en equipos Android reales.

Consulta [ROADMAP](docs/ROADMAP.md) para las funciones pendientes y el plan de pruebas.
