# Todo en Uno 0.8.0 — Juegos, documentos y biblioteca

## Módulos nuevos

- `feature:games`: Snake, Bloques y Nave espacial, motores Kotlin sin acceso a red. Controles táctiles básicos, pausa, reinicio y puntuación. Bloques es un juego original de piezas descendentes; no usa marca, arte ni código de Tetris.
- `feature:personal-tools`: notas locales, mezcla digital de colores y bóveda DNI cifrada.
- `feature:peru-tools`: último evento sísmico oficial del IGP/CENSIS.
- `feature:readers`: Biblia RV1909 completa offline (66 libros), lector textual de recetarios EPUB y lector PDF offline para Constitución.
- `feature:document-tools`: editor de composición de hasta 10 imágenes para exportación PDF A4.

## Privacidad y límites

- DNI: imágenes copiadas cifradas AES-256-GCM con clave no exportable del Android Keystore, archivos en `noBackupFilesDir`, protección de capturas y verificación mediante bloqueo de pantalla antes de acceder. No hay transmisión a servidores. El modelo no puede garantizar seguridad en un dispositivo rooteado, infectado o con sistema comprometido.
- Notas: guardadas en JSON dentro del almacenamiento privado de la app, no cifradas; no guardar datos de alto riesgo.
- EPUB/Constitución: se copian al directorio privado para consulta offline, sin subirlos. El lector EPUB es textual y puede no representar maquetaciones o EPUB con DRM.
- Editor PDF: imagen por página o 2/4 por página, controles de escala, desplazamiento y rotación, máximo 10 fotos. Editor ligero, no todas las funciones de Canva.
- Color: mezcla RGB digital aproximada; no reproduce fielmente pigmentos físicos.
- Sismos: consulta a IGP, no alerta temprana ni predicción.

## Fuentes y licencias

- **Biblia**: Reina-Valera 1909, dominio público/CC0, fuente [BibleAquifer/ReinaValera1909](https://github.com/BibleAquifer/ReinaValera1909). Distribuye los 66 JSON en `feature/readers/src/main/assets/rv1909/` (1–66). RV1960 no se distribuye sin licencia.
- **Constitución**: el usuario descarga la edición oficial [Biblioteca del Congreso](https://www3.congreso.gob.pe/biblioteca/constituciones-peru/) y la importa para leerla offline. La app muestra que el texto puede quedar desactualizado; no empaqueta por defecto un texto legal de edición incierta.
- **Sismos**: [IGP/CENSIS](https://censis.igp.gob.pe/) y su [servicio ArcGIS](https://ide.igp.gob.pe/arcgis/rest/services/monitoreocensis/UltimoSismo/MapServer/0?f=pjson). Se consulta únicamente el último evento geográfico publicado; el historial completo se consulta en [Reportes sísmicos](https://ultimosismo.igp.gob.pe/productos/reportes-sismicos).
- Juegos y gráficos implementados como código propio, offline, sin paquetes de assets comerciales.

## Pruebas pendientes antes de lanzar

- Emulador CI, pruebas físicas de Snake/Bloques/Nave y suspensiones de actividad.
- Fotos grandes y rotación/corte del editor PDF, vértices en impresión A4.
- Android KeyStore y verificación de bloqueo en versiones 8, 13 y 15.
- EPUB con capítulos XHTML complejos y PDFs oficiales largos.
- Validar respuesta del servicio IGP y reintentos de red en dispositivos reales.
