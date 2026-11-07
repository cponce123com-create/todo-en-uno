# Todo en Uno · Android

Centro de herramientas Android, desarrollado con Kotlin y Jetpack Compose. Aplicación en español, orientada a funcionar sin conexión.

## Versión 0.3.0

- Inicio con catálogo, búsqueda y favoritos persistentes.
- Calculadora, linterna, conversor de unidades, cronómetro, temporizador y reloj mundial.
- Reproductor de audio local (Media3 ExoPlayer + MediaSessionService) con control desde pantalla bloqueada y notificación.
- Brújula magnética, nivel de burbuja, generador QR y contraseñas seguras.
- Iconografía 3D refinada y catálogo ampliado a 29 herramientas.
- Grabadora de voz con archivos privados y exportación mediante selector del sistema.
- Divisas con tasas fechadas y guardadas offline (Frankfurter), GPS, regla calibrable, Pomodoro.
- Texto, RGB/HEX, finanzas, código Morse y diagnóstico del dispositivo.
- Espejo, lupa, lector QR con CameraX y medidor relativo de audio dBFS.
- Calculadora científica, ocho categorías de unidades, metrónomo, IMC, edad e información de red.
- Interfaz Material 3 con modo claro/oscuro automático.
- Arquitectura modular: `app`, `core:catalog`, `feature:utilities`.
- APK de depuración generado por GitHub Actions, sin instalar Android Studio.

## Compilación

Abre **Actions → Android CI → último flujo correcto → Artifacts → todo-en-uno-debug-apk**.

También se puede compilar localmente con Gradle 8.13 y JDK 17:

```bash
gradle :app:assembleDebug
```

> Nota: aún no se incluye el Gradle Wrapper; CI instala una versión fija de Gradle. No se ha verificado la aplicación en dispositivos físicos.

## Estructura

- `app`: interfaz principal, navegación y favoritos guardados con DataStore.
- `core:catalog`: definiciones y registro de herramientas, sin dependencia Android.
- `feature:utilities`: pantallas y lógica de las primeras herramientas.

La navegación se centraliza en la app y las herramientas exponen interfaces declarativas. Las funciones más complejas obtendrán módulos propios conforme crezcan.

## Privacidad

Sin inicio de sesión, anuncios ni analíticas. La reproducción local no requiere permiso general de almacenamiento: el usuario selecciona sus archivos con el selector del sistema. El servicio de audio utiliza únicamente los permisos de primer plano exigidos por Android para reproducción multimedia; no pide micrófono ni acceso a la ubicación. El uso de sensores depende del equipo.

## Próximos pasos

Ver [ROADMAP](docs/ROADMAP.md).
