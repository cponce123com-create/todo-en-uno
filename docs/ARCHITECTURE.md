# Arquitectura · Todo en Uno 0.6.0

## Límites de módulos

| Módulo | Responsabilidad |
|---|---|
| `:app` | Navegación Compose, tarjetas, registro de rutas, estado de inicio y UI tests |
| `:core:catalog` | Metadatos de 33 herramientas y filtros puros, sin dependencia de Android |
| `:feature:math-tools` | Cálculo científico, conversores, finanzas y cálculos informativos de edad/IMC |
| `:feature:sensor-tools` | Linterna, brújula, nivel, campo magnético y regla de pantalla |
| `:feature:location-tools` | GPS, distancia geodésica, Sol/Luna y reloj mundial |
| `:feature:utilities` | Productividad, QR, sistema, cronómetros y temporizadores |
| `:feature:audio` | Reproductor Media3 con servicio en segundo plano |
| `:feature:recorder` | Grabadora, sonómetro relativo y afinador |
| `:feature:camera` | CameraX: espejo, lupa y lector QR |
| `:feature:finance` | Divisas con red y caché local |

Los módulos de dominio exponen una única pantalla de entrada `MathToolsScreen`, `SensorToolsScreen` o `LocationToolsScreen`. `app/ToolRenderer.kt` decide a qué módulo delegar según la identidad de la herramienta. Los permisos declarados de ubicación están en `:feature:location-tools`, no en el módulo general de productividad.

## Inyección de dependencias

**Decisión: mantener DI manual por ahora.** `HomeViewModel` recibe `FavoritesStore` mediante `HomeViewModel.factory`. Las herramientas con sensores tienen su ciclo de vida acotado por Compose y `DisposableEffect`. Introducir Hilt ahora obligaría a agregar anotaciones y procesamiento de código a módulos que apenas necesitan objetos compartidos, elevando tiempo de compilación sin una mejora medible. Evaluar Hilt cuando haya tres o más repositorios con dependencias compartidas, repositorios falsos para pruebas y servicios inyectables.

## Calidad y cobertura

El CI debe usar el Wrapper y ejecutar:

```bash
./gradlew :core:catalog:test testDebugUnitTest lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Una segunda tarea de CI ejecuta `:app:connectedDebugAndroidTest` sobre un emulador API 35. Las pruebas cubren que el botón Atrás no pierda el historial y que se puedan activar/desactivar favoritos desde la interfaz. `ToolRoutingTest` exige que todas las entradas del catálogo posean pantalla. Los informes Lint y JUnit se publican como artefactos de GitHub Actions.

## Accesibilidad y temas

- Tarjetas y favoritos tienen nombres semánticos claros y etiquetas de prueba estables.
- Los iconos 3D son decorativos para TalkBack: no se debe leer el emoji ni duplicar el nombre de la tarjeta.
- El encabezado, la brújula, el nivel y la regla consumen paletas `MaterialTheme.colorScheme` para responder a modo oscuro y colores dinámicos.
- Los colores fijos que **representan datos** (el selector RGB, los patrones de QR generados) se preservan, porque cambiar esos colores alteraría la herramienta misma.

## Validación pendiente

CI verde no asegura compatibilidad con todas las marcas ni excelencia en sensores. Revisar manualmente TalkBack, contraste, bloqueo de pantalla, permisos y recambio de proceso en Android 8, 13 y 15. Los módulos por dominio y las pruebas instrumentadas son preparación estructural para esas rondas.
