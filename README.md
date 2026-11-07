# Todo en Uno · Android

Centro de herramientas Android, desarrollado con Kotlin y Jetpack Compose. Aplicación en español, orientada a funcionar sin conexión.

## Versión 0.1.0

- Inicio con catálogo, búsqueda y favoritos persistentes.
- Calculadora, linterna, conversor de unidades, cronómetro, temporizador y reloj mundial.
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

Sin inicio de sesión, sin anuncios, sin analíticas, sin conexiones de red y sin permisos generales de almacenamiento o micrófono en esta versión. Linterna solo funciona cuando el dispositivo dispone de flash compatible.

## Próximos pasos

Ver [ROADMAP](docs/ROADMAP.md).
