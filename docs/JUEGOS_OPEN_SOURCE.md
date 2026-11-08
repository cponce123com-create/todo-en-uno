# Juegos offline: fuentes y adaptaciones

Revisión: 7 de noviembre de 2026. 2048, Breakout y Fifteen se adaptan a motores Kotlin y pantallas Compose. Los avisos de las fuentes se incluyen en assets/licenses y se consultan desde cada juego. No se incluyen los recursos gráficos, fuentes ni audios de los proyectos originales.

## Comparación

| Proyecto | Licencia y código | Peso de archivos del snapshot | Integración y decisión |
| --- | --- | --- | --- |
| [2048 — Gabriele Cirulli](https://github.com/gabrielecirulli/2048) | [MIT](https://github.com/gabrielecirulli/2048/blob/478b6ec346e3787f589e4af751378d06ded4cbbc/LICENSE.txt). JavaScript/HTML/CSS. | 603 765 bytes en todos los archivos; 49 194 bytes de JS/CSS/HTML. Commit `478b6ec346e3787f589e4af751378d06ded4cbbc`. | Primera opción: tablero pequeño y sin motor gráfico externo. Adaptar a Kotlin/Compose o empaquetar una copia local revisada. Usar tipografía del sistema para evitar importar fuentes adicionales. |
| [Breakout — Jake Gordon](https://github.com/jakesgordon/javascript-breakout) | [Código MIT; audio con licencia separada CC BY-ND 2.0](https://github.com/jakesgordon/javascript-breakout/blob/eed59e2affa9423b93d2ac8ff93061bb88b33284/LICENSE). JavaScript/Canvas. | 11 159 989 bytes del snapshot completo, incluyendo paquetes duplicados y medios; 670 981 bytes de JS/CSS/HTML, también con duplicados. Commit `eed59e2affa9423b93d2ac8ff93061bb88b33284`. | Opción arcade: adaptar lógica y dibujar gráficos propios, con efectos sintetizados. No importar todo el repositorio ni asumir que los sonidos están bajo MIT. |
| [Simon Tatham / port Android de Chris Boyle](https://github.com/chrisboyle/sgtpuzzles) | [Licencia upstream MIT](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/doc/licence.html). Motores C; interfaz Android Kotlin/JNI. | 15 483 052 bytes del snapshot completo del port. Commit `d1e10eb57dd80af4999e698a9ab31f546e9a0b9d`. | Seleccionar puzzles concretos como Mines, Solo o Fifteen. No es una biblioteca Compose que se importe directamente: requiere NDK/JNI, adaptar el frontend y revisar avisos del port. Prioridad posterior a 2048. |

Los tamaños se obtuvieron sumando `size` de blobs del árbol Git recursivo de cada commit mediante la API GitHub. No son tamaño comprimido, descarga de APK, memoria ni incremento medido de esta app. Antes de integrar, construir una variante y comparar APK, arranque y fluidez en un Android modesto.

## Condiciones de integración

- Fijar un commit, incluir avisos de copyright/licencia de los componentes que se adapten y revisar por separado recursos, tipografías y sonidos.
- Mantener toda la lógica y recursos necesarios dentro del APK; quitar enlaces de analítica, donaciones y cargas remotas de una eventual versión web incorporada.
- Si se usa WebView: servir recursos locales con un origen controlado, bloquear navegación y solicitudes externas, sin puentes JavaScript a funciones Android y sin acceso general al sistema de archivos. JavaScript se habilitaría solo para el juego local que lo necesite.
- Probar en modo avión, guardar progreso de forma local, usar controles accesibles, pausa por ciclo de vida y disponer de opciones para sonido/efectos.
- Medir antes de llamar «liviano» al paquete integrado. La preferencia para esta app es adaptar una mecánica pequeña al módulo nativo existente.

## Cambios de esta ronda

- Controles con acción al apoyar el dedo, área mínima 64 × 58 dp, estado de pulsación, etiquetas de accesibilidad y repetición opcional al mantener pulsado.
- Snake: deslizar, cola limitada de dos giros y prevención de inversión.
- Bloques: sombra de caída y ajustes de rotación junto a paredes.
- Nave: mover y disparar con controles independientes, repetición sostenida, estela, propulsión e impactos. Corregida la colisión de proyectiles que cruzaban un enemigo entre pasos.
- Inicio explícito, pausa al ir a segundo plano y reinicio fiable tras perder; récords y opciones guardados localmente.
- Sonido sintetizado opcional, sin archivos de audio ni motor externo; efectos/vibración se pueden desactivar.

## Integración realizada

| Juego | Implementación | Progreso y controles |
| --- | --- | --- |
| 2048 | Port de las reglas de recorrido dirigido y fusión única de Gabriele Cirulli a `ImportedPuzzleEngines.kt`. | Deslizar o flechas; un deshacer; objetivo 2048; tablero y puntuación locales. |
| Breakout | Adaptación de colisiones y dirección por impacto con pala de Jake Gordon a `BreakoutEngine.kt`, con gráficos y nivel propios. | Pala por arrastre o botones sostenidos, lanzamiento explícito, 3 vidas, 30 bloques. Récord local; la partida arcade se reinicia al salir del juego. |
| Fifteen / Puzzle 15 | Port del deslizamiento alineado de `interpret_move` / `execute_move` de `fifteen.c` a `ImportedPuzzleEngines.kt`. | Tocar fila/columna del hueco; generación siempre resoluble; deshacer; tablero y movimientos locales. |

No se importa toda la colección de Simon Tatham: esta integración selecciona Fifteen. El port nativo evita JNI, NDK y WebView. Los otros puzzles de la colección siguen siendo candidatos para una futura integración.

### Fuentes consultadas y modificaciones

- `2048/js/game_manager.js` en commit `478b6ec346e3787f589e4af751378d06ded4cbbc`: unificar una sola vez por movimiento, orden según dirección, puntuación y nueva ficha solo si hubo cambio. Se reemplazan DOM y almacenamiento web por Compose y preferencias Android; se detiene al llegar a 2048 y se añade deshacer.
- `javascript-breakout/breakout.js` en commit `eed59e2affa9423b93d2ac8ff93061bb88b33284`: reflexión de bola y orientación según punto de impacto con pala. El port usa subpasos acotados, coordenadas normalizadas, nivel propio y audio sintetizado; no incluye el motor JavaScript, imágenes ni sonidos originales.
- `sgtpuzzles/app/src/main/jni/fifteen.c` en commit `d1e10eb57dd80af4999e698a9ab31f546e9a0b9d`: deslizamiento de fila/columna alineada y recuento por ficha desplazada. Se genera por movimientos legales aleatorios, se validan partidas guardadas por paridad y se sustituye el frontend C/JNI por Compose. No se portan el solucionador ni las pistas upstream.

Pruebas añadidas: 18 casos JVM para fusiones, movimientos inválidos, restauración, paridad de Fifteen y física de Breakout, más 3 casos instrumentados de entrada, pausa y licencias. El tamaño del APK y el consumo en dispositivo se deben medir tras compilar; los tamaños del snapshot de la tabla anterior no representan el incremento de esta app.

### Validación de motores

Los tres archivos de motores y sus pruebas se compilaron con Kotlin 2.2.21/JVM 17 y se ejecutaron con JUnit 4.13.2: **30 pruebas aprobadas** (18 nuevas y 12 existentes). Esta ejecución independiente de Gradle valida la lógica pura; no sustituye la compilación Android, Lint ni las pruebas instrumentadas en emulador.
