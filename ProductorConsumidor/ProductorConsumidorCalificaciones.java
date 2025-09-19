import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Clase principal
public class ProductorConsumidorCalificaciones {

    // Clase Buffer para sincronización manual entre productor y consumidor
    // Ahora es una clase estática anidada.
    static class BufferCalificaciones {
        private final List<Float> buffer;
        private final int capacidad;
        private boolean produccionTerminada = false;

        public BufferCalificaciones(int capacidad) {
            this.buffer = new ArrayList<>();
            this.capacidad = capacidad;
        }

        // Método para que el productor añada una calificación al buffer
        public synchronized void producir(Float calificacion) throws InterruptedException {
            // Esperar si el buffer está lleno
            while (buffer.size() == capacidad) {
                wait();
            }
            buffer.add(calificacion);
            // Notificar a los hilos en espera (especialmente al consumidor)
            notifyAll();
        }

        // Método para que el consumidor obtenga una calificación del buffer
        public synchronized Float consumir() throws InterruptedException {
            // Esperar si el buffer está vacío y la producción no ha terminado
            while (buffer.isEmpty() && !produccionTerminada) {
                wait();
            }

            // Si la producción ha terminado y el buffer está vacío, retornar null
            if (buffer.isEmpty() && produccionTerminada) {
                return null;
            }

            // Consumir el primer elemento del buffer
            Float calificacion = buffer.remove(0);
            // Notificar a los hilos en espera (especialmente al productor)
            notifyAll();
            return calificacion;
        }

        // Método para notificar al consumidor que el productor ha terminado
        public synchronized void terminarProduccion() {
            this.produccionTerminada = true;
            // Despertar a cualquier hilo consumidor que esté esperando
            notifyAll();
        }
    }

    // Clase Productor que lee calificaciones del archivo
    // Ahora es una clase estática anidada.
    static class Productor extends Thread {
        private BufferCalificaciones buffer;
        private String archivoEntrada;

        public Productor(BufferCalificaciones buffer, String archivoEntrada) {
            this.buffer = buffer;
            this.archivoEntrada = archivoEntrada;
        }

        @Override
        public void run() {
            try {
                // Generar archivo de calificaciones si no existe
                generarArchivoCalificaciones();

                // Leer calificaciones del archivo
                BufferedReader reader = new BufferedReader(new FileReader(archivoEntrada));
                String linea;
                int contador = 0;

                System.out.println("PRODUCTOR: Iniciando lectura de calificaciones...\n");

                while ((linea = reader.readLine()) != null && contador < 20) {
                    Float calificacion = Float.parseFloat(linea.trim());
                    buffer.producir(calificacion);
                    System.out.println("PRODUCTOR: Enviando calificación #" + (contador + 1) + " = " + calificacion);
                    Thread.sleep(100); // Simular tiempo de producción
                    contador++;
                }

                reader.close();
                buffer.terminarProduccion();
                System.out.println("\nPRODUCTOR: Producción terminada. Total enviadas: " + contador);

            } catch (Exception e) {
                System.err.println("Error en Productor: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void generarArchivoCalificaciones() throws IOException {
            File archivo = new File(archivoEntrada);
            if (!archivo.exists()) {
                PrintWriter writer = new PrintWriter(new FileWriter(archivo));
                Random random = new Random();

                System.out.println("Generando archivo de calificaciones...");

                for (int i = 0; i < 20; i++) {
                    float calificacion = 4.0f + random.nextFloat() * 6.0f;
                    calificacion = Math.round(calificacion * 10) / 10.0f;
                    writer.println(calificacion);
                }

                writer.close();
                System.out.println("Archivo '" + archivoEntrada + "' generado con 20 calificaciones.\n");
            }
        }
    }

    // Clase Consumidor que procesa solo calificaciones aprobatorias
    // Ahora es una clase estática anidada.
    static class Consumidor extends Thread {
        private BufferCalificaciones buffer;
        private String archivoSalida;
        private List<Float> aprobados;
        private List<Float> reprobados;

        public Consumidor(BufferCalificaciones buffer, String archivoSalida) {
            this.buffer = buffer;
            this.archivoSalida = archivoSalida;
            this.aprobados = new ArrayList<>();
            this.reprobados = new ArrayList<>();
        }

        @Override
        public void run() {
            try {
                PrintWriter writer = new PrintWriter(new FileWriter(archivoSalida));
                writer.println("=== CALIFICACIONES APROBATORIAS ===\n");

                System.out.println("\nCONSUMIDOR: Iniciando procesamiento...\n");

                int contador = 0;
                Float calificacion;

                // Bucle principal del consumidor
                while ((calificacion = buffer.consumir()) != null) {
                    contador++;
                    System.out.print("CONSUMIDOR: Procesando calificación #" + contador + " = " + calificacion);

                    if (calificacion >= 6.0f) {
                        aprobados.add(calificacion);
                        writer.println("Alumno " + contador + ": " + calificacion + " - APROBADO");
                        writer.flush();
                        System.out.println(" -> ✓ APROBADO");
                    } else {
                        reprobados.add(calificacion);
                        System.out.println(" -> ✗ Reprobado");
                    }
                    Thread.sleep(150); // Simular tiempo de procesamiento
                }

                // Escribir resumen final
                writer.println("\n\n===== RESUMEN FINAL =====");
                writer.println("Total de alumnos procesados: " + contador);
                writer.println("Total de aprobados: " + aprobados.size());
                writer.println("Total de reprobados: " + reprobados.size());
                writer.println("\nCalificaciones aprobatorias:");

                for (int i = 0; i < aprobados.size(); i++) {
                    writer.println("  " + (i + 1) + ". " + aprobados.get(i));
                }

                // Calcular estadísticas
                if (!aprobados.isEmpty()) {
                    float suma = 0;
                    float max = aprobados.get(0);
                    float min = aprobados.get(0);

                    for (Float cal : aprobados) {
                        suma += cal;
                        if (cal > max)
                            max = cal;
                        if (cal < min)
                            min = cal;
                    }

                    float promedio = suma / aprobados.size();

                    writer.println("\n===== ESTADÍSTICAS DE APROBADOS =====");
                    writer.println("Promedio: " + String.format("%.2f", promedio));
                    writer.println("Calificación más alta: " + max);
                    writer.println("Calificación más baja: " + min);
                    writer.println("Porcentaje de aprobación: " +
                            String.format("%.1f%%", (aprobados.size() * 100.0 / contador)));
                }

                writer.close();

                // Mostrar resumen en consola
                System.out.println("\n========================================");
                System.out.println("CONSUMIDOR: Procesamiento terminado");
                System.out.println("========================================");
                System.out.println("Total procesados: " + contador);
                System.out.println("Aprobados: " + aprobados.size() + " (" +
                        String.format("%.1f%%", (aprobados.size() * 100.0 / contador)) + ")");
                System.out.println("Reprobados: " + reprobados.size() + " (" +
                        String.format("%.1f%%", (reprobados.size() * 100.0 / contador)) + ")");

                if (!aprobados.isEmpty()) {
                    float promedio = (float) aprobados.stream()
                            .mapToDouble(Float::doubleValue)
                            .average()
                            .orElse(0.0);
                    System.out.println("Promedio de aprobados: " + String.format("%.2f", promedio));
                }

                System.out.println("\nArchivo de salida generado: '" + archivoSalida + "'");

            } catch (Exception e) {
                System.err.println("Error en Consumidor: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  SISTEMA PRODUCTOR-CONSUMIDOR DE CALIFICACIONES      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        // Crear buffer compartido con capacidad de 5
        BufferCalificaciones buffer = new BufferCalificaciones(5);

        // Crear hilos productor y consumidor
        Productor productor = new Productor(buffer, "calificaciones.txt");
        Consumidor consumidor = new Consumidor(buffer, "aprobados.txt");

        // Iniciar hilos
        productor.start();
        consumidor.start();

        try {
            // Esperar a que terminen ambos hilos
            productor.join();
            consumidor.join();

            System.out.println("\n╔══════════════════════════════════════════════════════╗");
            System.out.println("║              PROCESO COMPLETADO EXITOSAMENTE         ║");
            System.out.println("╚══════════════════════════════════════════════════════╝");

        } catch (InterruptedException e) {
            System.err.println("Error: Proceso interrumpido");
            e.printStackTrace();
        }
    }
}