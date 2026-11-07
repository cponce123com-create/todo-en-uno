# Hoja de ruta — Todo en Uno

## 0.1.0: estructura y primeras utilidades

- [x] Repositorio Android Kotlin / Compose y modularización inicial.
- [x] Catálogo central de herramientas.
- [x] Calculadora, conversor de unidades, linterna, cronómetro, temporizador y reloj mundial.
- [x] Favoritos locales, búsqueda y temas de sistema.
- [x] GitHub Actions: pruebas unitarias y APK debug.
- [x] Confirmar compilación y pruebas en GitHub Actions (7 de noviembre de 2026).
- [ ] Probar en dispositivos Android físicos.

## 0.2.0: audio y utilidades

- [x] Reproductor de audio mediante Media3, con sesión multimedia y notificación de segundo plano.
- [x] Brújula con sensor de rotación y fallback a acelerómetro/magnetómetro.
- [x] Nivel de burbuja con acelerómetro.
- [x] Generador QR sin Internet.
- [x] Generador de contraseñas con SecureRandom.
- [x] Iconos visuales con profundidad y relieve, sin añadir imágenes de gran tamaño al APK.
- [ ] Pruebas reales de segundo plano en Android 8, 13 y 15.
- [ ] Integrar paquete de iconos ilustrados 3D como recursos raster si se aprueban los diseños.

## 0.2.1: productividad

- Grabadora con permisos solicitados al usar y gestión de archivos mediante SAF/MediaStore.
- Mejorar calibración visual y precisión de brújula.
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

## 0.3.0 — Segunda expansión

- [x] Grabadora de voz en primer plano con exportación SAF.
- [x] Tipo de cambio con actualización manual y caché con fecha.
- [x] GPS con solicitud de ubicación al entrar.
- [x] Regla calibrable en pantalla, Pomodoro, herramientas de texto, RGB/HEX, cálculos financieros, Morse y datos del equipo.
- [x] Espejo, lupa, escáner QR, medidor relativo de sonido.
- [x] Metrónomo, IMC, edad y red local.
- [x] Calculadora científica y conversión ampliada.
- [ ] Comprobar CI final y testear en teléfono real micrófono, GPS, red y cámara.

## Prioridades 0.4.0

- Escáner QR usando cámara, lupa/espejo, sonómetro orientativo, alarma y cronómetro de vueltas.
- Mapas y mediciones con proveedor cartográfico y soporte offline legítimo.
- Reproducción y grabación avanzada con servicio en primer plano y controles persistentes.
