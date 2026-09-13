import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

/**
 * App launcher: a themed grid of module icons, drawn entirely with
 * Graphics primitives (rounded cards + simple glyphs) rather than a plain
 * MIDP List, so the app has one consistent, designed look instead of the
 * phone's default gray menu style.
 */
public class LauncherCanvas extends Canvas implements Runnable, CommandListener {

    private static final int COLS = 2;
    private static final String[] LABELS = {
        "Calculator", "Converter", "Notes", "Dictionary", "BMI", "About"
    };
    // single-character glyphs drawn large inside each card - kept ASCII-only
    // for maximum font compatibility across phones
    private static final String[] GLYPHS = {
        "=", "~", "N", "D", "B", "i"
    };

    private final ToolkitMIDlet midlet;
    private int selected;
    private boolean pulseOn;
    private boolean running;
    private Thread pulseThread;

    private final Command exitCmd = new Command("Exit", Command.EXIT, 1);

    public LauncherCanvas(ToolkitMIDlet midlet) {
        this.midlet = midlet;
        setFullScreenMode(true);
        addCommand(exitCmd);
        setCommandListener(this);
    }

    // Canvas lifecycle hooks - the right place to start/stop a screen-local
    // animation thread, rather than a manually managed flag from outside.
    protected void showNotify() {
        running = true;
        pulseThread = new Thread(this);
        pulseThread.start();
    }

    protected void hideNotify() {
        running = false;
    }

    public void run() {
        while (running) {
            pulseOn = !pulseOn;
            repaint();
            serviceRepaints();
            try {
                Thread.sleep(450);
            } catch (InterruptedException e) {
                // loop checks `running` again next pass
            }
        }
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        int cols = COLS;
        int rows = (LABELS.length + cols - 1) / cols;
        int row = selected / cols;
        int col = selected % cols;
        switch (action) {
            case Canvas.LEFT:
                if (col > 0) { selected--; midlet.sound().playNav(); }
                break;
            case Canvas.RIGHT:
                if (col < cols - 1 && selected + 1 < LABELS.length) { selected++; midlet.sound().playNav(); }
                break;
            case Canvas.UP:
                if (row > 0) { selected -= cols; midlet.sound().playNav(); }
                break;
            case Canvas.DOWN:
                if (row < rows - 1 && selected + cols < LABELS.length) { selected += cols; midlet.sound().playNav(); }
                break;
            case Canvas.FIRE:
                launch(selected);
                break;
            default:
                break;
        }
        repaint();
    }

    private void launch(int index) {
        midlet.sound().playSelect();
        switch (index) {
            case 0: midlet.showCalculator(); break;
            case 1: midlet.showConverter(); break;
            case 2: midlet.showNotes(); break;
            case 3: midlet.showDictionary(); break;
            case 4: midlet.showBmi(); break;
            case 5: midlet.showAbout(); break;
            default: break;
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == exitCmd) {
            midlet.exitApp();
        }
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        g.setColor(Theme.BG);
        g.fillRect(0, 0, w, h);

        // header
        g.setColor(Theme.TEXT);
        g.drawString("Village Toolkit", 8, 6, Graphics.TOP | Graphics.LEFT);
        int titleWidth = g.getFont().stringWidth("Village Toolkit");
        g.setColor(pulseOn ? Theme.ACCENT : Theme.ACCENT_DIM);
        g.fillRect(8, 24, titleWidth, 2);

        int top = 36;
        int cols = COLS;
        int rows = (LABELS.length + cols - 1) / cols;
        int gridH = h - top - 6;
        int cellW = w / cols;
        int cellH = gridH / rows;

        for (int i = 0; i < LABELS.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = col * cellW + 6;
            int y = top + row * cellH + 6;
            int cw = cellW - 12;
            int ch = cellH - 12;

            boolean isSelected = (i == selected);
            int bg = isSelected ? Theme.PANEL_FOCUS : Theme.PANEL;
            g.setColor(bg);
            fillRoundRect(g, x, y, cw, ch, 8);

            if (isSelected) {
                g.setColor(pulseOn ? Theme.ACCENT : Theme.ACCENT_DIM);
                drawRoundRectBorder(g, x, y, cw, ch, 8);
            }

            // glyph
            g.setColor(isSelected ? Theme.ACCENT : Theme.TEXT_DIM);
            String glyph = GLYPHS[i];
            int gw = g.getFont().stringWidth(glyph);
            g.drawString(glyph, x + cw / 2 - gw / 2, y + 6, Graphics.TOP | Graphics.LEFT);

            // label
            g.setColor(Theme.TEXT);
            String label = LABELS[i];
            int lw = g.getFont().stringWidth(label);
            g.drawString(label, x + cw / 2 - lw / 2, y + ch - 18, Graphics.TOP | Graphics.LEFT);
        }

        g.setColor(Theme.TEXT_DIM);
        String hint = "Arrows to move, FIRE to open";
        int hw = g.getFont().stringWidth(hint);
        g.drawString(hint, (w - hw) / 2, h - 16, Graphics.TOP | Graphics.LEFT);
    }

    private void fillRoundRect(Graphics g, int x, int y, int w, int h, int arc) {
        g.fillRoundRect(x, y, w, h, arc, arc);
    }

    private void drawRoundRectBorder(Graphics g, int x, int y, int w, int h, int arc) {
        g.drawRoundRect(x, y, w, h, arc, arc);
    }
}
