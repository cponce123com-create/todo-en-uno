# Hoja de ruta — Todo en Uno

## 0.1.0: estructura y primeras utilidades

- [x] Repositorio Android Kotlin / Compose y modularización inicial.
- [x] Catálogo central de herramientas.
- [x] Calculadora, conversor de unidades, linterna, cronómetro, temporizador y reloj mundial.
- [x] Favoritos locales, búsqueda y temas de sistema.
- [x] GitHub Actions: pruebas unitarias y APK debug.
- [x] Confirmar compilación y pruebas en GitHub Actions (7 de noviembre de 2026).
- [ ] Probar en dispositivos Android físicos.

## 0.2.0: productividad

- Grabadora con permisos solicitados al usar y gestión de archivos mediante SAF/MediaStore.
- Brújula con detección de sensor, calibración y mensajes de disponibilidad.
- QR y códigos de barras con escáner local.
- Conversor de divisas con tasas fechadas y fuente verificable.

## 0.3.0: geolocalización y medición

- GPS, rutas y cálculo de áreas y distancias.
- Mapas con estrategia offline y atribución compatible con la licencia del proveedor.
- Medición con sensores: nivel e inclinómetro.

## Ingeniería continua

- UI tests Compose, pruebas de accesibilidad, CI con emulador, rendimiento, firma segura.
- Evolucionar a interfaces API/implementación para las funcionalidades complejas.
- Integrar Hilt cuando aparezcan repositorios y servicios con dependencias compartidas.
- No incorporar permisos de micrófono, cámara o ubicación hasta que una función los necesite.
