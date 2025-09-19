# Ejercicios de Programación Concurrente

Este repositorio contiene la solución a una serie de ejercicios de programación concurrente en Java, enfocados en el uso de hilos y mecanismos de sincronización como `synchronized`, `wait()`, `notifyAll()`, y `Thread.join()`.

---

### 1. Sistema Productor-Consumidor

Este ejercicio implementa un clásico problema de productor-consumidor para procesar calificaciones.

* **Archivo Principal:** `ProductorConsumidor/ProductorConsumidorCalificaciones.java`
* **Archivos de Datos:** `ProductorConsumidor/calificaciones.txt` (generado por el productor) y `ProductorConsumidor/aprobados.txt` (generado por el consumidor).

#### Descripción
El programa simula un sistema donde un hilo **Productor** lee calificaciones de un archivo y las coloca en un búfer compartido, y un hilo **Consumidor** toma esas calificaciones, procesa las aprobatorias (>= 6.0) y escribe un reporte en un archivo de salida.

#### Enfoque de Concurrencia
La sincronización se logra mediante el uso de:
* **`synchronized`:** Los métodos `producir()`, `consumir()` y `terminarProduccion()` están sincronizados para garantizar el acceso exclusivo al búfer compartido por un solo hilo a la vez.
* **`wait()` y `notifyAll()`:** Se utilizan para la comunicación entre hilos.
    * El productor entra en estado `wait()` cuando el búfer está lleno.
    * El consumidor entra en estado `wait()` cuando el búfer está vacío.
    * Ambos hilos usan `notifyAll()` para despertar a los hilos en espera cuando se añade o se consume un elemento del búfer.

---

### 2. Grafo de Precedencia de Tareas

Este ejercicio visualiza la ejecución de un grafo de tareas concurrentes, respetando las dependencias entre ellas.

* **Archivo Principal:** `Grafo/GrafoPrecedencia.java`

#### Descripción
El programa utiliza una interfaz gráfica (Swing) para representar un grafo de tareas (`S1` a `S5`). Cada tarea es ejecutada por un hilo separado, y se visualiza cómo los nodos cambian de color (de gris a verde) a medida que se completan. La visualización incluye las operaciones realizadas y sus resultados.

#### Enfoque de Concurrencia
La correcta ejecución en paralelo y secuencial se asegura de la siguiente manera:
* **`Thread.start()`:** Los hilos que no tienen dependencias (`S1`, `S3`, `S5`) se inician simultáneamente para que se ejecuten en paralelo.
* **`Thread.join()`:** Se usa para forzar la espera de una tarea hasta que su predecesora termine. Por ejemplo, `S2` no se ejecuta hasta que `S1.join()` es completado.
* **`synchronized`:** Aunque se utiliza `Thread.join()` para la secuenciación, se usa un bloque `synchronized (lock)` en el método `paintComponent` y en los hilos de ejecución para garantizar la coherencia de los datos compartidos (las variables `a`, `b`, `c`, etc. y el estado de `ejecutado`).

---

### 3. Juego de la Serpiente Concurrente

Este ejercicio es una implementación del clásico juego de la serpiente, donde múltiples tareas se ejecutan en hilos separados para un rendimiento óptimo.

* **Archivo Principal:** `Juego/SnakeConcurrente.java`

#### Descripción
El juego de la serpiente utiliza hilos para:
* **Bucle principal del juego:** Maneja el movimiento de la serpiente y las colisiones.
* **Generador de comida:** Crea nuevas piezas de comida de forma asíncrona.
* **Hilo de enemigos (opcional):** En niveles avanzados, mueve obstáculos de forma independiente.

#### Enfoque de Concurrencia
El estado del juego es gestionado por la clase `GameState`, que centraliza todas las variables compartidas. La sincronización se realiza mediante:
* **`synchronized`:** Se utilizan bloques y métodos sincronizados en la clase `GameState` para proteger el acceso a las variables compartidas (puntuación, vidas, listas de posiciones, etc.). Esto evita condiciones de carrera y asegura la consistencia de los datos.
* **`volatile`:** Se usa en variables primitivas para garantizar que los cambios realizados por un hilo sean inmediatamente visibles para todos los demás, lo cual es útil para variables como `gameOver` y `pausado`.
* **Manejo de colecciones:** Se utilizan copias de las listas (`ArrayList`) dentro de los métodos `synchronized` para evitar `ConcurrentModificationException` cuando el hilo de dibujo (`paintComponent`) itera sobre ellas.