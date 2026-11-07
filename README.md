# Todo en Uno · Android

## Versión 0.7.0 — Rediseño y expansión educativa

- Inicio con diseño premium, accesos rápidos 3D personalizables y categorías.
- Catálogo en cuadrícula o lista, manteniendo búsqueda, favoritos y navegación tipada.
- Calculadora científica avanzada con motor de expresiones, trigonometría, DEG/RAD e historial local.
- Tabla periódica offline con 118 elementos, 12 fórmulas de física con calculadora y triángulos notables.
- Nivel virtual sobre CameraX con retícula de alineación, regla cm/mm calibrable y persistente.
- Linterna Normal/SOS/Estrobo/Vela, con advertencias de fotosensibilidad.
- Generador de etiquetas en A4 con QR y exportación PDF mediante selector Android.
- Nuevos módulos `:feature:education-tools` y `:feature:document-tools`.

**Estado:** consulta Android CI para conocer el resultado de pruebas, Lint y APK del commit que vayas a instalar. El mockup de referencia es inspiración visual; la implementación utiliza iconografía vectorial en lugar de copiar la imagen completa.

Aplicación Android nativa y modular en **Kotlin + Jetpack Compose + Material 3**, diseñada en español.

## Arquitectura: versión 0.6.0

Las herramientas se separaron por dominios en `:feature:math-tools`, `:feature:sensor-tools` y `:feature:location-tools`. También incorporamos pruebas de UI en emulador, rutas comprobadas para 33 herramientas y colores dinámicos en brújula/nivel/regla. Consulta [ARCHITECTURE](docs/ARCHITECTURE.md) para límites y la decisión de DI manual.
- Navigation Compose con rutas tipadas (Kotlin Serialization) y Back Stack real.
- Deep links locales: `todoenuno://tool/flashlight` o `todoenuno://tool/calculator`.
- Pantalla de información como destino secundario (Herramienta → Información → Herramienta).
- `HomeViewModel` con `SavedStateHandle` para búsqueda y filtros; favoritos en DataStore.
- `gradle/libs.versions.toml` centraliza versiones de los módulos y plugins.
- Wrapper oficial de Gradle 8.13: `./gradlew` o `gradlew.bat`.
- CI ejecuta pruebas de todos los módulos Android, tests JVM, Lint y APK.
- Los gráficos 3D son decorativos para lectores de pantalla; las tarjetas y estrellas tienen etiquetas legibles.

## Herramientas heredadas de 0.4.0

**Herramientas disponibles** en el catálogo y organizadas por categorías, con búsqueda, favoritos persistentes y portada de iconos vectoriales 3D en cuadrícula:

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

Los permisos de cámara, micrófono y ubicación se solicitan cuando entras en la herramienta correspondiente. La grabadora guarda inicialmente archivos en almacenamiento privado y permite exportarlos de forma explícita. Las consultas de divisas, clima y sismos requieren Internet; el reproductor utiliza archivos seleccionados por el usuario.

El sonómetro indica **dBFS relativo, no dB SPL calibrados**. El GPS y la regla dependen de la precisión de sensores y pantalla. La cotización de divisas es una tasa media de referencia, no el precio de compra/venta ni la cotización oficial BCRP. IMC y cálculos financieros son informativos.

El temporizador dispone de aviso opcional con WorkManager (puede demorarse), mientras que la grabadora y Pomodoro no mantienen un servicio propio de segundo plano. La **reproducción de audio** sí utiliza un servicio Media3 compatible con pantalla bloqueada.

## Estado de pruebas

El flujo CI ejecuta pruebas JVM y compila APK debug. Es necesario validar cámara, micrófono, segundo plano, GPS y diversos tamaños de pantalla en equipos Android reales.

Consulta [ROADMAP](docs/ROADMAP.md) para las funciones pendientes y el plan de pruebas.

## Correcciones de integridad y lectores

- Notas: escrituras atómicas y exclusivas, límite visible de 300, confirmación tras guardar y bloqueo de cambios si falla la lectura inicial.
- Bóveda DNI: resultados de lectura invalidados al bloquear/cambiar lado; destino de importación capturado al elegir la foto.
- EPUB: los documentos que exceden 65 000 caracteres por sección o 400 secciones se rechazan explícitamente, sin truncar texto.
- Temporizador: conceder permiso durante una cuenta programa el aviso pendiente.
- Grabaciones: exportación fuera del hilo de interfaz y preparación asíncrona del reproductor.
- Sismos: orden cronológico antes de limitar resultados. Se consulta la capa histórica oficial Sismos Reportados del IGP, ordenada por fechaevento descendente, con hasta 30 reportes por consulta y 100 eventos guardados localmente.

## Auditoría de lectores, PDF y emulador

- El compositor PDF lee dimensiones sin interpretar el retorno nulo de la decodificación de límites como un error; cierra las páginas también si una imagen falla.
- Recetario y Constitución comparten importación EPUB validada antes de reemplazar el libro, escritura atómica y limpieza de temporales.
- Importar un PDF de Constitución activa su lectura y corrige índices de página restaurados fuera de rango.
- CI comprueba acceso real a KVM y exige aceleración; utiliza 2 núcleos y 2 GB de RAM para el emulador. Un fallo de infraestructura no cuenta como ejecución de pruebas instrumentadas.
- Se añaden pruebas instrumentadas con archivos EPUB e imágenes/PDF reales para estas regresiones.
