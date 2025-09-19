import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GrafoPrecedencia extends JPanel {
    private Map<String, Boolean> ejecutado;
    private Map<String, Point> posiciones;
    private Map<String, String> operaciones;

    // Variables compartidas
    private volatile int a, b = 1, c = 2, d = 3, e, f = 4, g = 5, h, i = 6, j = 7;
    private int t_a, t_b = 1, t_c = 2, t_d = 3, t_e, t_f = 4, t_g = 5, t_h, t_i = 6, t_j = 7;
    private final Object lock = new Object();

    public GrafoPrecedencia() {
        ejecutado = new HashMap<>();
        posiciones = new HashMap<>();
        operaciones = new HashMap<>();
        inicializarPosiciones();
        inicializarOperaciones();

        setPreferredSize(new Dimension(600, 400));
        setBackground(Color.WHITE);
    }

    private void inicializarPosiciones() {
        posiciones.put("S1", new Point(150, 100));
        posiciones.put("S2", new Point(250, 200));
        posiciones.put("S3", new Point(350, 100));
        posiciones.put("S4", new Point(350, 200));
        posiciones.put("S5", new Point(50, 200));
    }

    private void inicializarOperaciones() {
        operaciones.put("S1", "a = b + c");
        operaciones.put("S2", "b = a + d");
        operaciones.put("S3", "e = c + f");
        operaciones.put("S4", "c = e + g");
        operaciones.put("S5", "h = i + j");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        synchronized (lock) {
            // Dibujar dependencias
            g2d.setStroke(new BasicStroke(2));
            g2d.setColor(Color.BLUE);
            drawArrow(g2d, "S1", "S2");
            drawArrow(g2d, "S3", "S4");

            // Dibujar nodos
            for (Map.Entry<String, Point> entry : posiciones.entrySet()) {
                String nodo = entry.getKey();
                Point pos = entry.getValue();

                // Color según estado de ejecución
                if (ejecutado.getOrDefault(nodo, false)) {
                    g2d.setColor(Color.GREEN);
                } else {
                    g2d.setColor(Color.LIGHT_GRAY);
                }

                g2d.fillOval(pos.x - 25, pos.y - 25, 50, 50);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(pos.x - 25, pos.y - 25, 50, 50);

                // Dibujar nombre de la tarea
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fm = g2d.getFontMetrics();
                int stringWidth = fm.stringWidth(nodo);
                g2d.drawString(nodo, pos.x - stringWidth / 2, pos.y - 5);

                // Dibujar operación y resultado
                g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                String operacion = operaciones.get(nodo);
                String resultado = "";
                if (ejecutado.getOrDefault(nodo, false)) {
                    switch (nodo) {
                        case "S1":
                            resultado = "a=" + a;
                            break;
                        case "S2":
                            resultado = "b=" + b;
                            break;
                        case "S3":
                            resultado = "e=" + e;
                            break;
                        case "S4":
                            resultado = "c=" + c;
                            break;
                        case "S5":
                            resultado = "h=" + h;
                            break;
                    }
                }
                stringWidth = g2d.getFontMetrics().stringWidth(operacion);
                g2d.drawString(operacion, pos.x - stringWidth / 2, pos.y + 10);
                if (!resultado.isEmpty()) {
                    stringWidth = g2d.getFontMetrics().stringWidth(resultado);
                    g2d.drawString(resultado, pos.x - stringWidth / 2, pos.y + 20);
                }
            }
            g2d.setColor(Color.BLACK);
            g2d.drawString("Valores iniciales: a=" + t_a + ", b=" + t_b + ", c=" + t_c + ", d= " + t_d +
                    ", e=" + t_e + ", f=" + t_f + ", g=" + t_g + ", h=" + t_h + ", i=" + t_i + ", j=" + t_j, 10, 350);

        }
    }

    private void drawArrow(Graphics2D g2d, String from, String to) {
        Point p1 = posiciones.get(from);
        Point p2 = posiciones.get(to);

        int x1 = p1.x;
        int y1 = p1.y + 25;
        int x2 = p2.x;
        int y2 = p2.y - 25;

        g2d.drawLine(x1, y1, x2, y2);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowLength = 10;
        int x3 = (int) (x2 - arrowLength * Math.cos(angle - Math.PI / 6));
        int y3 = (int) (y2 - arrowLength * Math.sin(angle - Math.PI / 6));
        int x4 = (int) (x2 - arrowLength * Math.cos(angle + Math.PI / 6));
        int y4 = (int) (y2 - arrowLength * Math.sin(angle + Math.PI / 6));

        g2d.drawLine(x2, y2, x3, y3);
        g2d.drawLine(x2, y2, x4, y4);
    }

    private void updateStateAndRepaint(String task) {
        synchronized (lock) {
            ejecutado.put(task, true);
        }
        SwingUtilities.invokeLater(() -> repaint());
    }

    public void ejecutarGrafo() {
        // Tareas con hilos
        Thread s1Thread = new Thread(() -> {
            try {
                Thread.sleep(2000);
                synchronized (lock) {
                    a = b + c;
                }
                updateStateAndRepaint("S1");
                System.out.println("S1 ejecutado: a = " + a);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread s3Thread = new Thread(() -> {
            try {
                Thread.sleep(2000);
                synchronized (lock) {
                    e = c + f;
                }
                updateStateAndRepaint("S3");
                System.out.println("S3 ejecutado: e = " + e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread s5Thread = new Thread(() -> {
            try {
                Thread.sleep(2000);
                synchronized (lock) {
                    h = i + j;
                }
                updateStateAndRepaint("S5");
                System.out.println("S5 ejecutado: h = " + h);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread s2Thread = new Thread(() -> {
            try {
                s1Thread.join(); // Espera a que S1 termine
                Thread.sleep(2000);
                synchronized (lock) {
                    b = a + d;
                }
                updateStateAndRepaint("S2");
                System.out.println("S2 ejecutado: b = " + b);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread s4Thread = new Thread(() -> {
            try {
                s3Thread.join(); // Espera a que S3 termine
                Thread.sleep(2000);
                synchronized (lock) {
                    c = e + g;
                }
                updateStateAndRepaint("S4");
                System.out.println("S4 ejecutado: c = " + c);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 1. Iniciar todas las tareas que no tienen dependencias
        s1Thread.start();
        s3Thread.start();
        s5Thread.start();

        // 2. Iniciar las tareas que tienen dependencias
        s2Thread.start();
        s4Thread.start();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Grafo de Precedencia - Concurrencia");
        GrafoPrecedencia panel = new GrafoPrecedencia();

        frame.add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(1000);
                panel.ejecutarGrafo();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
