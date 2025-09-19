import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

// Clase principal del juego (sin cambios)
public class SnakeConcurrente extends JFrame {
    private GamePanel gamePanel;
    private JLabel scoreLabel;
    private JLabel livesLabel;
    private JLabel levelLabel;

    public SnakeConcurrente() {
        setTitle("Snake Concurrente - Programación Paralela");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(Color.DARK_GRAY);
        infoPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 30, 10));

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 18));

        livesLabel = new JLabel("Vidas: 3");
        livesLabel.setForeground(Color.WHITE);
        livesLabel.setFont(new Font("Arial", Font.BOLD, 18));

        levelLabel = new JLabel("Nivel: 1");
        levelLabel.setForeground(Color.WHITE);
        levelLabel.setFont(new Font("Arial", Font.BOLD, 18));

        infoPanel.add(scoreLabel);
        infoPanel.add(livesLabel);
        infoPanel.add(levelLabel);

        gamePanel = new GamePanel(this);

        JPanel instructionsPanel = new JPanel();
        instructionsPanel.setBackground(Color.DARK_GRAY);
        JLabel instructions = new JLabel("Flechas: Mover | Espacio: Pausar | R: Reiniciar (cuando Game Over)");
        instructions.setForeground(Color.LIGHT_GRAY);
        instructions.setFont(new Font("Arial", Font.PLAIN, 12));
        instructionsPanel.add(instructions);

        setLayout(new BorderLayout());
        add(infoPanel, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);
        add(instructionsPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        gamePanel.iniciarJuego();
    }

    public void actualizarScore(int score) {
        scoreLabel.setText("Score: " + score);
    }

    public void actualizarVidas(int vidas) {
        livesLabel.setText("Vidas: " + vidas);
    }

    public void actualizarNivel(int nivel) {
        levelLabel.setText("Nivel: " + nivel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SnakeConcurrente());
    }
}

// Clase para representar el estado compartido del juego
class GameState {
    private volatile int score = 0;
    private volatile int vidas = 3;
    private volatile boolean gameOver = false;
    private volatile boolean pausado = false;
    private volatile int nivel = 1;

    // Listas de juego, ahora privadas
    public final List<Point> serpiente;
    public final List<Point> comida;
    public final List<Point> obstaculos;
    private volatile String direccion = "DERECHA";
    private volatile String proximaDireccion = "DERECHA";

    private final int ANCHO = 30;
    private final int ALTO = 20;

    public GameState() {
        serpiente = new ArrayList<>();
        comida = new ArrayList<>();
        obstaculos = new ArrayList<>();
        inicializarSerpiente();
        generarObstaculos();
    }

    private void inicializarSerpiente() {
        serpiente.clear();
        serpiente.add(new Point(5, 10));
        serpiente.add(new Point(4, 10));
        serpiente.add(new Point(3, 10));
        direccion = "DERECHA";
        proximaDireccion = "DERECHA";
    }

    private synchronized void generarObstaculos() {
        obstaculos.clear();
        Random rand = new Random();
        int numObstaculos = 3 + nivel * 2;

        for (int i = 0; i < numObstaculos; i++) {
            Point nuevoObstaculo;
            do {
                nuevoObstaculo = new Point(
                        rand.nextInt(ANCHO - 2) + 1,
                        rand.nextInt(ALTO - 2) + 1);
            } while (colisionConSerpiente(nuevoObstaculo));

            obstaculos.add(nuevoObstaculo);
        }
    }

    // Método privado para evitar acceso directo fuera de la clase
    private boolean colisionConSerpiente(Point punto) {
        for (Point parte : serpiente) {
            if (punto.equals(parte)) {
                return true;
            }
        }
        return false;
    }

    // Método principal de movimiento
    public synchronized void moverSerpiente() {
        if (gameOver || pausado)
            return;

        direccion = proximaDireccion;
        Point cabeza = new Point(serpiente.get(0));
        Point nuevaCabeza = new Point(cabeza);

        switch (direccion) {
            case "ARRIBA":
                nuevaCabeza.y--;
                break;
            case "ABAJO":
                nuevaCabeza.y++;
                break;
            case "IZQUIERDA":
                nuevaCabeza.x--;
                break;
            case "DERECHA":
                nuevaCabeza.x++;
                break;
        }

        if (nuevaCabeza.x < 0 || nuevaCabeza.x >= ANCHO ||
                nuevaCabeza.y < 0 || nuevaCabeza.y >= ALTO) {
            perderVida();
            return;
        }

        for (int i = 0; i < serpiente.size(); i++) {
            if (nuevaCabeza.equals(serpiente.get(i))) {
                perderVida();
                return;
            }
        }

        for (Point obstaculo : obstaculos) {
            if (nuevaCabeza.equals(obstaculo)) {
                perderVida();
                return;
            }
        }

        serpiente.add(0, nuevaCabeza);

        boolean comio = false;
        Iterator<Point> it = comida.iterator();
        while (it.hasNext()) {
            Point c = it.next();
            if (nuevaCabeza.equals(c)) {
                it.remove();
                incrementarScore(10);
                comio = true;

                if (score > 0 && score % 50 == 0) {
                    subirNivel();
                }
                break;
            }
        }

        if (!comio && serpiente.size() > 0) {
            serpiente.remove(serpiente.size() - 1);
        }
    }

    private synchronized void perderVida() {
        vidas--;
        if (vidas <= 0) {
            gameOver = true;
        } else {
            inicializarSerpiente();
        }
    }

    private synchronized void subirNivel() {
        nivel++;
        generarObstaculos();
    }

    public synchronized void incrementarScore(int puntos) {
        score += puntos;
    }

    public synchronized void cambiarDireccion(String nuevaDireccion) {
        if ((direccion.equals("ARRIBA") && !nuevaDireccion.equals("ABAJO")) ||
                (direccion.equals("ABAJO") && !nuevaDireccion.equals("ARRIBA")) ||
                (direccion.equals("IZQUIERDA") && !nuevaDireccion.equals("DERECHA")) ||
                (direccion.equals("DERECHA") && !nuevaDireccion.equals("IZQUIERDA"))) {
            proximaDireccion = nuevaDireccion;
        }
    }

    public synchronized void reiniciar() {
        score = 0;
        vidas = 3;
        nivel = 1;
        gameOver = false;
        pausado = false;
        inicializarSerpiente();
        generarObstaculos();
        comida.clear();
    }

    // Getters de estado que no devuelven listas
    public synchronized int getScore() {
        return score;
    }

    public synchronized int getVidas() {
        return vidas;
    }

    public synchronized int getNivel() {
        return nivel;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public synchronized boolean isPausado() {
        return pausado;
    }

    public synchronized void togglePausa() {
        pausado = !pausado;
    }

    public int getAncho() {
        return ANCHO;
    }

    public int getAlto() {
        return ALTO;
    }

    // Métodos para acceder a las listas de forma segura
    public synchronized List<Point> getSerpiente() {
        return new ArrayList<>(serpiente);
    }

    public synchronized List<Point> getComida() {
        return new ArrayList<>(comida);
    }

    public synchronized List<Point> getObstaculos() {
        return new ArrayList<>(obstaculos);
    }
}

// Panel del juego con corrección en la lógica de hilos
class GamePanel extends JPanel {
    private static final int CELL_SIZE = 25;
    private GameState estado;
    private SnakeConcurrente frame;

    private Thread gameLoopThread;
    private Thread comidaThread;
    private Thread enemyThread;

    public GamePanel(SnakeConcurrente frame) {
        this.frame = frame;
        this.estado = new GameState();

        setPreferredSize(new Dimension(
                estado.getAncho() * CELL_SIZE,
                estado.getAlto() * CELL_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                procesarInput(e);
            }
        });
    }

    private void procesarInput(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                estado.cambiarDireccion("ARRIBA");
                break;
            case KeyEvent.VK_DOWN:
                estado.cambiarDireccion("ABAJO");
                break;
            case KeyEvent.VK_LEFT:
                estado.cambiarDireccion("IZQUIERDA");
                break;
            case KeyEvent.VK_RIGHT:
                estado.cambiarDireccion("DERECHA");
                break;
            case KeyEvent.VK_SPACE:
                estado.togglePausa();
                break;
            case KeyEvent.VK_R:
                if (estado.isGameOver()) {
                    reiniciarJuego();
                }
                break;
        }
    }

    public void iniciarJuego() {
        gameLoopThread = new Thread(() -> {
            while (!estado.isGameOver()) {
                if (!estado.isPausado()) {
                    estado.moverSerpiente();

                    SwingUtilities.invokeLater(() -> {
                        frame.actualizarScore(estado.getScore());
                        frame.actualizarVidas(estado.getVidas());
                        frame.actualizarNivel(estado.getNivel());
                        repaint();
                    });
                }

                try {
                    int delay = Math.max(50, 150 - (estado.getNivel() - 1) * 20);
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            SwingUtilities.invokeLater(() -> repaint());
        });

        comidaThread = new Thread(() -> {
            Random rand = new Random();
            while (!estado.isGameOver()) {
                synchronized (estado) {
                    // Ahora se comprueba y se añade comida a la lista directamente
                    if (!estado.isPausado() && estado.comida.size() < 3) {
                        Point nuevaComida;
                        int intentos = 0;
                        do {
                            nuevaComida = new Point(
                                    rand.nextInt(estado.getAncho()),
                                    rand.nextInt(estado.getAlto()));
                            intentos++;
                        } while (colisionConElementos(nuevaComida) && intentos < 100);

                        if (intentos < 100) {
                            estado.comida.add(nuevaComida);
                        }
                    }
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        enemyThread = new Thread(() -> {
            Random rand = new Random();
            while (!estado.isGameOver()) {
                synchronized (estado) {
                    if (!estado.isPausado() && estado.getNivel() >= 3) {
                        List<Point> nuevosObstaculos = new ArrayList<>();
                        for (Point obs : estado.obstaculos) {
                            if (rand.nextDouble() < 0.1) {
                                Point nuevoObs = new Point(obs);
                                int dir = rand.nextInt(4);
                                switch (dir) {
                                    case 0:
                                        nuevoObs.y--;
                                        break;
                                    case 1:
                                        nuevoObs.y++;
                                        break;
                                    case 2:
                                        nuevoObs.x--;
                                        break;
                                    case 3:
                                        nuevoObs.x++;
                                        break;
                                }

                                if (nuevoObs.x >= 0 && nuevoObs.x < estado.getAncho() &&
                                        nuevoObs.y >= 0 && nuevoObs.y < estado.getAlto() &&
                                        !colisionConElementos(nuevoObs)) {
                                    nuevosObstaculos.add(nuevoObs);
                                } else {
                                    nuevosObstaculos.add(obs);
                                }
                            } else {
                                nuevosObstaculos.add(obs);
                            }
                        }
                        estado.obstaculos.clear();
                        estado.obstaculos.addAll(nuevosObstaculos);
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        gameLoopThread.start();
        comidaThread.start();
        enemyThread.start();
    }

    private synchronized boolean colisionConElementos(Point punto) {
        synchronized (estado) {
            for (Point s : estado.serpiente) {
                if (punto.equals(s))
                    return true;
            }

            for (Point o : estado.obstaculos) {
                if (punto.equals(o))
                    return true;
            }

            for (Point c : estado.comida) {
                if (punto.equals(c))
                    return true;
            }
        }
        return false;
    }

    private void reiniciarJuego() {
        if (gameLoopThread != null)
            gameLoopThread.interrupt();
        if (comidaThread != null)
            comidaThread.interrupt();
        if (enemyThread != null)
            enemyThread.interrupt();

        estado.reiniciar();
        frame.actualizarScore(0);
        frame.actualizarVidas(3);
        frame.actualizarNivel(1);

        iniciarJuego();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 20, 20),
                getWidth(), getHeight(), new Color(40, 40, 40));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(new Color(50, 50, 50, 50));
        for (int i = 0; i <= estado.getAncho(); i++) {
            g2d.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, estado.getAlto() * CELL_SIZE);
        }
        for (int i = 0; i <= estado.getAlto(); i++) {
            g2d.drawLine(0, i * CELL_SIZE, estado.getAncho() * CELL_SIZE, i * CELL_SIZE);
        }

        // Obtener copias seguras de las listas para dibujarlas
        List<Point> serpiente;
        List<Point> comida;
        List<Point> obstaculos;

        synchronized (estado) {
            serpiente = new ArrayList<>(estado.serpiente);
            comida = new ArrayList<>(estado.comida);
            obstaculos = new ArrayList<>(estado.obstaculos);
        }

        for (Point obstaculo : obstaculos) {
            if (estado.getNivel() >= 3) {
                g2d.setColor(new Color(255, 100, 100));
            } else {
                g2d.setColor(Color.GRAY);
            }
            g2d.fillRect(obstaculo.x * CELL_SIZE + 2, obstaculo.y * CELL_SIZE + 2,
                    CELL_SIZE - 4, CELL_SIZE - 4);
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawRect(obstaculo.x * CELL_SIZE + 2, obstaculo.y * CELL_SIZE + 2,
                    CELL_SIZE - 4, CELL_SIZE - 4);
        }

        for (Point c : comida) {
            RadialGradientPaint rgp = new RadialGradientPaint(
                    c.x * CELL_SIZE + CELL_SIZE / 2,
                    c.y * CELL_SIZE + CELL_SIZE / 2,
                    CELL_SIZE / 2,
                    new float[] { 0f, 1f },
                    new Color[] { Color.YELLOW, Color.RED });
            g2d.setPaint(rgp);
            g2d.fillOval(c.x * CELL_SIZE + 3, c.y * CELL_SIZE + 3,
                    CELL_SIZE - 6, CELL_SIZE - 6);
        }

        int segmento = 0;
        for (Point parte : serpiente) {
            if (segmento == 0) {
                g2d.setColor(new Color(0, 255, 0));
                g2d.fillRoundRect(parte.x * CELL_SIZE + 1, parte.y * CELL_SIZE + 1,
                        CELL_SIZE - 2, CELL_SIZE - 2, 5, 5);
                g2d.setColor(Color.WHITE);
                int eyeSize = 4;
                int eyeOffset = 5;
                g2d.fillOval(parte.x * CELL_SIZE + eyeOffset,
                        parte.y * CELL_SIZE + eyeOffset, eyeSize, eyeSize);
                g2d.fillOval(parte.x * CELL_SIZE + CELL_SIZE - eyeOffset - eyeSize,
                        parte.y * CELL_SIZE + eyeOffset, eyeSize, eyeSize);
            } else {
                int green = Math.max(100, 200 - segmento * 10);
                g2d.setColor(new Color(0, green, 0));
                g2d.fillRoundRect(parte.x * CELL_SIZE + 2, parte.y * CELL_SIZE + 2,
                        CELL_SIZE - 4, CELL_SIZE - 4, 3, 3);
            }
            segmento++;
        }

        if (estado.getNivel() >= 2) {
            g2d.setColor(new Color(255, 255, 0, 30));
            g2d.setFont(new Font("Arial", Font.BOLD, 100));
            g2d.drawString("NIVEL " + estado.getNivel(), getWidth() / 2 - 150, getHeight() / 2);
        }

        if (estado.isPausado()) {
            mostrarMensaje(g2d, "PAUSADO", "Presiona ESPACIO para continuar", Color.YELLOW);
        } else if (estado.isGameOver()) {
            mostrarMensaje(g2d, "GAME OVER",
                    "Score Final: " + estado.getScore() +
                            " | Nivel: " + estado.getNivel() +
                            " | Presiona R para reiniciar",
                    Color.RED);
        }
    }

    private void mostrarMensaje(Graphics2D g2d, String titulo, String subtitulo, Color color) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(color);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(titulo)) / 2;
        int y = getHeight() / 2 - 20;
        g2d.drawString(titulo, x, y);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        fm = g2d.getFontMetrics();
        x = (getWidth() - fm.stringWidth(subtitulo)) / 2;
        y += 50;
        g2d.drawString(subtitulo, x, y);
    }
}
