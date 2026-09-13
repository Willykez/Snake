import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

/**
 * Snake with a title/menu screen, levels, obstacle mazes that get denser
 * as you progress, increasing speed, a 3-life system, bonus food, and
 * optional sound cues (silently disabled if the phone lacks MMAPI).
 */
public class SnakeCanvas extends Canvas implements Runnable, CommandListener {

    private static final int CELL = 10;
    private static final int MAX_LENGTH = 300;
    private static final int FOOD_PER_LEVEL = 5;
    private static final int START_DELAY = 180;
    private static final int MIN_DELAY = 70;
    private static final int DELAY_STEP = 12;
    private static final int BONUS_LIFETIME_TICKS = 40;
    private static final int START_LIVES = 3;
    private static final String HS_STORE = "snake_hs";

    private final SnakeMIDlet midlet;
    private final SoundManager sound = new SoundManager();

    private int cols, rows;
    private final int topOffset = 20;

    private boolean[] obstacle;

    private int[] snakeX = new int[MAX_LENGTH];
    private int[] snakeY = new int[MAX_LENGTH];
    private int length;

    private int dirX, dirY;
    private int pendingDirX, pendingDirY;

    private int foodX, foodY;
    private int foodEatenThisLevel;

    private boolean bonusActive;
    private int bonusX, bonusY;
    private int bonusTicksLeft;

    private int score;
    private int level;
    private int lives;
    private int highScore;
    private int highLevel;

    private boolean showTitle = true;
    private int titleSelection;
    private final String[] titleOptions = { "Start game", "Exit" };

    private boolean gameOver;
    private boolean paused;
    private boolean running;
    private Thread loopThread;

    private final Command exitCmd = new Command("Exit", Command.EXIT, 1);
    private final Command restartCmd = new Command("New game", Command.SCREEN, 1);

    public SnakeCanvas(SnakeMIDlet midlet) {
        this.midlet = midlet;
        setFullScreenMode(true);
        addCommand(exitCmd);
        addCommand(restartCmd);
        setCommandListener(this);
        loadHighScores();
    }

    // ---- lifecycle ----

    public void startGame() {
        if (obstacle == null) {
            cols = getWidth() / CELL;
            rows = (getHeight() - topOffset) / CELL;
            obstacle = new boolean[cols * rows];
        }
        showTitle = true;
        gameOver = false;
        paused = false;
        running = true;
        loopThread = new Thread(this);
        loopThread.start();
    }

    public void pauseGame() { running = false; }
    public void stopGame() { running = false; }

    private void startNewRun() {
        score = 0;
        level = 1;
        lives = START_LIVES;
        foodEatenThisLevel = 0;
        gameOver = false;
        paused = false;
        generateLevel(level);
        spawnSnake();
        placeFood();
        bonusActive = false;
    }

    private void spawnSnake() {
        length = 3;
        int startX = cols / 2;
        int startY = rows / 2;
        for (int i = 0; i < length; i++) {
            snakeX[i] = startX - i;
            snakeY[i] = startY;
        }
        dirX = 1; dirY = 0;
        pendingDirX = 1; pendingDirY = 0;
    }

    // ---- level generation ----

    private boolean isNearCenter(int x, int y) {
        int cx = cols / 2, cy = rows / 2;
        return Math.abs(x - cx) <= 3 && Math.abs(y - cy) <= 2;
    }

    private void generateLevel(int lvl) {
        for (int i = 0; i < obstacle.length; i++) {
            obstacle[i] = false;
        }
        int segments = Math.min(lvl * 2, (cols * rows) / 20 + 2);
        int placed = 0;
        int attempts = 0;
        while (placed < segments && attempts < segments * 25) {
            attempts++;
            boolean horizontal = Math.random() < 0.5;
            int len = 2 + (int) (Math.random() * 3);
            int x = (int) (Math.random() * cols);
            int y = (int) (Math.random() * rows);
            boolean ok = true;
            for (int i = 0; i < len && ok; i++) {
                int cx = horizontal ? x + i : x;
                int cy = horizontal ? y : y + i;
                if (cx < 0 || cx >= cols || cy < 0 || cy >= rows || isNearCenter(cx, cy)) {
                    ok = false;
                }
            }
            if (!ok) {
                continue;
            }
            for (int i = 0; i < len; i++) {
                int cx = horizontal ? x + i : x;
                int cy = horizontal ? y : y + i;
                obstacle[cy * cols + cx] = true;
            }
            placed++;
        }
    }

    private boolean isBlocked(int x, int y) {
        return obstacle[y * cols + x];
    }

    private void placeFood() {
        boolean bad;
        do {
            foodX = (int) (Math.random() * cols);
            foodY = (int) (Math.random() * rows);
            bad = isBlocked(foodX, foodY);
            if (!bad) {
                for (int i = 0; i < length; i++) {
                    if (snakeX[i] == foodX && snakeY[i] == foodY) { bad = true; break; }
                }
            }
        } while (bad);
    }

    private void maybeSpawnBonus() {
        if (bonusActive || Math.random() > 0.3) {
            return;
        }
        boolean bad;
        int tries = 0;
        do {
            bonusX = (int) (Math.random() * cols);
            bonusY = (int) (Math.random() * rows);
            bad = isBlocked(bonusX, bonusY) || (bonusX == foodX && bonusY == foodY);
            if (!bad) {
                for (int i = 0; i < length; i++) {
                    if (snakeX[i] == bonusX && snakeY[i] == bonusY) { bad = true; break; }
                }
            }
            tries++;
        } while (bad && tries < 20);
        if (!bad) {
            bonusActive = true;
            bonusTicksLeft = BONUS_LIFETIME_TICKS;
        }
    }

    // ---- game loop ----

    public void run() {
        while (running) {
            if (!showTitle && !gameOver && !paused) {
                step();
            }
            repaint();
            serviceRepaints();
            try {
                Thread.sleep(currentDelay());
            } catch (InterruptedException e) {
                // loop checks `running` again next pass
            }
        }
    }

    private int currentDelay() {
        int delay = START_DELAY - (level - 1) * DELAY_STEP;
        return Math.max(MIN_DELAY, delay);
    }

    private void step() {
        dirX = pendingDirX;
        dirY = pendingDirY;

        int newHeadX = snakeX[0] + dirX;
        int newHeadY = snakeY[0] + dirY;

        if (newHeadX < 0 || newHeadX >= cols || newHeadY < 0 || newHeadY >= rows
                || isBlocked(newHeadX, newHeadY)) {
            onDeath();
            return;
        }
        for (int i = 0; i < length; i++) {
            if (snakeX[i] == newHeadX && snakeY[i] == newHeadY) {
                onDeath();
                return;
            }
        }

        boolean ateFood = (newHeadX == foodX && newHeadY == foodY);
        boolean ateBonus = bonusActive && newHeadX == bonusX && newHeadY == bonusY;

        int newLength = ateFood ? Math.min(length + 1, MAX_LENGTH) : length;
        for (int i = newLength - 1; i > 0; i--) {
            snakeX[i] = snakeX[i - 1];
            snakeY[i] = snakeY[i - 1];
        }
        snakeX[0] = newHeadX;
        snakeY[0] = newHeadY;
        length = newLength;

        if (ateBonus) {
            score += 30;
            bonusActive = false;
            sound.playBonus();
        }

        if (ateFood) {
            score += 10;
            foodEatenThisLevel++;
            placeFood();
            maybeSpawnBonus();
            sound.playEat();
            if (foodEatenThisLevel >= FOOD_PER_LEVEL) {
                advanceLevel();
            }
        }

        if (bonusActive) {
            bonusTicksLeft--;
            if (bonusTicksLeft <= 0) {
                bonusActive = false;
            }
        }
    }

    private void advanceLevel() {
        level++;
        foodEatenThisLevel = 0;
        generateLevel(level);
        spawnSnake();
        placeFood();
        bonusActive = false;
        sound.playLevelUp();
    }

    private void onDeath() {
        lives--;
        if (lives > 0) {
            sound.playHit();
            spawnSnake();
            bonusActive = false;
        } else {
            gameOver = true;
            sound.playGameOver();
            boolean improved = false;
            if (score > highScore) { highScore = score; improved = true; }
            if (level > highLevel) { highLevel = level; improved = true; }
            if (improved) {
                saveHighScores();
            }
        }
    }

    // ---- input ----

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (showTitle) {
            handleTitleKey(action);
            return;
        }
        if (gameOver) {
            if (action == Canvas.FIRE) {
                showTitle = true;
            }
            return;
        }
        switch (action) {
            case Canvas.UP:
                if (dirY == 0) { pendingDirX = 0; pendingDirY = -1; }
                break;
            case Canvas.DOWN:
                if (dirY == 0) { pendingDirX = 0; pendingDirY = 1; }
                break;
            case Canvas.LEFT:
                if (dirX == 0) { pendingDirX = -1; pendingDirY = 0; }
                break;
            case Canvas.RIGHT:
                if (dirX == 0) { pendingDirX = 1; pendingDirY = 0; }
                break;
            case Canvas.FIRE:
                paused = !paused;
                break;
            default:
                break;
        }
    }

    private void handleTitleKey(int action) {
        switch (action) {
            case Canvas.UP:
            case Canvas.DOWN:
                titleSelection = 1 - titleSelection;
                break;
            case Canvas.FIRE:
                if (titleSelection == 0) {
                    showTitle = false;
                    startNewRun();
                } else {
                    midlet.exitApp();
                }
                break;
            default:
                break;
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == exitCmd) {
            midlet.exitApp();
        } else if (c == restartCmd) {
            showTitle = false;
            startNewRun();
        }
    }

    // ---- rendering ----

    protected void paint(Graphics g) {
        g.setColor(0xFFFFFF);
        g.fillRect(0, 0, getWidth(), getHeight());

        if (showTitle) {
            drawTitleScreen(g);
            return;
        }

        g.setColor(0x000000);
        g.drawString("Lv " + level + "  Sc " + score + "  x" + lives, 4, 2, Graphics.TOP | Graphics.LEFT);
        g.drawLine(0, topOffset, getWidth(), topOffset);

        g.setColor(0x555555);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (isBlocked(x, y)) {
                    g.fillRect(x * CELL, topOffset + y * CELL, CELL - 1, CELL - 1);
                }
            }
        }

        g.setColor(0xCC3333);
        g.fillRect(foodX * CELL, topOffset + foodY * CELL, CELL - 1, CELL - 1);

        if (bonusActive && (bonusTicksLeft % 2 == 0)) {
            g.setColor(0xE6A817);
            g.fillRect(bonusX * CELL, topOffset + bonusY * CELL, CELL - 1, CELL - 1);
        }

        g.setColor(0x2E7D32);
        for (int i = 0; i < length; i++) {
            g.fillRect(snakeX[i] * CELL, topOffset + snakeY[i] * CELL, CELL - 1, CELL - 1);
        }

        if (paused && !gameOver) {
            drawCenteredMessage(g, "Paused - FIRE to resume", getHeight() / 2);
        }
        if (gameOver) {
            drawCenteredMessage(g, "Game over - FIRE for menu", getHeight() / 2);
            drawCenteredMessage(g, "Reached level " + level + ", score " + score, getHeight() / 2 + 16);
        }
    }

    private void drawTitleScreen(Graphics g) {
        g.setColor(0x000000);
        int cx = getWidth() / 2;

        drawCenteredMessage(g, "SNAKE", 40);
        drawCenteredMessage(g, "Best: Lv " + highLevel + "  Sc " + highScore, 70);

        int menuY = 140;
        for (int i = 0; i < titleOptions.length; i++) {
            String label = (i == titleSelection ? "> " : "  ") + titleOptions[i];
            drawCenteredMessage(g, label, menuY + i * 24);
        }

        drawCenteredMessage(g, "UP/DOWN to choose, FIRE to select", getHeight() - 30);
    }

    private void drawCenteredMessage(Graphics g, String msg, int y) {
        Graphics gr = g;
        int w = gr.getFont().stringWidth(msg);
        gr.drawString(msg, (getWidth() - w) / 2, y, Graphics.TOP | Graphics.LEFT);
    }

    // ---- persistence: high score + highest level reached, packed into 3 bytes ----

    private void loadHighScores() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(HS_STORE, true);
            if (rs.getNumRecords() == 0) {
                highScore = 0;
                highLevel = 1;
                return;
            }
            byte[] data = rs.getRecord(1);
            highScore = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
            highLevel = data[2] & 0xFF;
        } catch (RecordStoreException e) {
            highScore = 0;
            highLevel = 1;
        } finally {
            closeQuietly(rs);
        }
    }

    private void saveHighScores() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(HS_STORE, true);
            byte[] data = new byte[] {
                (byte) ((highScore >> 8) & 0xFF),
                (byte) (highScore & 0xFF),
                (byte) (highLevel & 0xFF)
            };
            if (rs.getNumRecords() == 0) {
                rs.addRecord(data, 0, data.length);
            } else {
                rs.setRecord(1, data, 0, data.length);
            }
        } catch (RecordStoreException e) {
            // non-fatal: game still works, just won't remember the best run
        } finally {
            closeQuietly(rs);
        }
    }

    private void closeQuietly(RecordStore rs) {
        if (rs != null) {
            try { rs.closeRecordStore(); } catch (RecordStoreException e) { /* ignore */ }
        }
    }
}
