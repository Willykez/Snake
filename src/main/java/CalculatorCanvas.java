import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

/**
 * A four-function calculator with a custom-drawn button grid. Two ways to
 * operate it: arrow keys move a cursor over the visible buttons (FIRE
 * presses one), or the phone's own digit keys type straight into the
 * display for speed - both drive the same input handling underneath.
 */
public class CalculatorCanvas extends Canvas implements CommandListener {

    private static final int COLS = 4;
    private static final String[] LABELS = {
        "7", "8", "9", "\u00F7",
        "4", "5", "6", "\u00D7",
        "1", "2", "3", "\u2212",
        "C", "0", ".", "+",
        "\u2190", "", "", "="
    };

    private final ToolkitMIDlet midlet;
    private int cursor = 13; // starts on "0"

    private String display = "0";
    private double accumulator = 0;
    private char pendingOp = 0;
    private boolean freshInput = true;
    private boolean errorState = false;

    private final Command backCmd = new Command("Back", Command.BACK, 1);

    public CalculatorCanvas(ToolkitMIDlet midlet) {
        this.midlet = midlet;
        setFullScreenMode(true);
        addCommand(backCmd);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            midlet.showLauncher();
        }
    }

    protected void keyPressed(int keyCode) {
        // direct digit shortcuts, independent of cursor position
        if (keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9) {
            inputDigit((char) ('0' + (keyCode - KEY_NUM0)));
            repaint();
            return;
        }
        if (keyCode == KEY_STAR) {
            inputDecimalPoint();
            repaint();
            return;
        }
        if (keyCode == KEY_POUND) {
            equals();
            repaint();
            return;
        }

        int action = getGameAction(keyCode);
        switch (action) {
            case Canvas.LEFT:
                if (cursor % COLS > 0) { cursor--; midlet.sound().playNav(); }
                break;
            case Canvas.RIGHT:
                if (cursor % COLS < COLS - 1 && cursor + 1 < LABELS.length) { cursor++; midlet.sound().playNav(); }
                break;
            case Canvas.UP:
                if (cursor - COLS >= 0) { cursor -= COLS; midlet.sound().playNav(); }
                break;
            case Canvas.DOWN:
                if (cursor + COLS < LABELS.length) { cursor += COLS; midlet.sound().playNav(); }
                break;
            case Canvas.FIRE:
                pressButton(LABELS[cursor]);
                break;
            default:
                break;
        }
        repaint();
    }

    private void pressButton(String label) {
        if (label.length() == 0) {
            return;
        }
        char c = label.charAt(0);
        if (c >= '0' && c <= '9') {
            inputDigit(c);
        } else if (label.equals(".")) {
            inputDecimalPoint();
        } else if (label.equals("C")) {
            clearAll();
        } else if (label.equals("\u2190")) {
            backspace();
        } else if (label.equals("=")) {
            equals();
        } else {
            // one of the four operators
            setOperator(c);
        }
    }

    private void inputDigit(char digit) {
        if (errorState) {
            clearAll();
        }
        if (freshInput) {
            display = String.valueOf(digit);
            freshInput = false;
        } else if (!display.equals("0")) {
            display = display + digit;
        } else {
            display = String.valueOf(digit);
        }
    }

    private void inputDecimalPoint() {
        if (errorState) {
            clearAll();
        }
        if (freshInput) {
            display = "0.";
            freshInput = false;
            return;
        }
        if (display.indexOf('.') < 0) {
            display = display + ".";
        }
    }

    private void backspace() {
        if (errorState || freshInput) {
            clearAll();
            return;
        }
        if (display.length() > 1) {
            display = display.substring(0, display.length() - 1);
        } else {
            display = "0";
            freshInput = true;
        }
    }

    private void clearAll() {
        display = "0";
        accumulator = 0;
        pendingOp = 0;
        freshInput = true;
        errorState = false;
    }

    private void setOperator(char op) {
        if (errorState) {
            return;
        }
        if (pendingOp != 0 && !freshInput) {
            applyPending();
        } else {
            accumulator = NumberUtil.parse(display);
        }
        pendingOp = op;
        freshInput = true;
    }

    private void equals() {
        if (errorState) {
            return;
        }
        applyPending();
        pendingOp = 0;
        freshInput = true;
    }

    private void applyPending() {
        double operand = NumberUtil.parse(display);
        double result = accumulator;
        switch (pendingOp) {
            case '+':
                result = accumulator + operand;
                break;
            case '\u2212':
                result = accumulator - operand;
                break;
            case '\u00D7':
                result = accumulator * operand;
                break;
            case '\u00F7':
                if (operand == 0) {
                    display = "Error";
                    errorState = true;
                    midlet.sound().playError();
                    return;
                }
                result = accumulator / operand;
                break;
            default:
                result = operand;
                break;
        }
        accumulator = result;
        display = NumberUtil.format(result, 4);
        display = trimTrailingZeros(display);
        midlet.sound().playSuccess();
    }

    private String trimTrailingZeros(String s) {
        if (s.indexOf('.') < 0) {
            return s;
        }
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && s.charAt(end - 1) == '.') {
            end--;
        }
        return s.substring(0, end);
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        g.setColor(Theme.BG);
        g.fillRect(0, 0, w, h);

        g.setColor(Theme.TEXT);
        g.drawString("Calculator", 8, 4, Graphics.TOP | Graphics.LEFT);

        // display panel
        int dispTop = 24;
        int dispHeight = 40;
        g.setColor(Theme.PANEL);
        g.fillRoundRect(8, dispTop, w - 16, dispHeight, 8, 8);
        g.setColor(errorState ? Theme.DANGER : Theme.ACCENT);
        int dw = g.getFont().stringWidth(display);
        g.drawString(display, w - 16 - dw, dispTop + dispHeight / 2 - 6, Graphics.TOP | Graphics.LEFT);

        // button grid
        int gridTop = dispTop + dispHeight + 8;
        int rows = LABELS.length / COLS;
        int cellW = w / COLS;
        int cellH = (h - gridTop - 4) / rows;

        for (int i = 0; i < LABELS.length; i++) {
            String label = LABELS[i];
            if (label.length() == 0) {
                continue;
            }
            int col = i % COLS;
            int row = i / COLS;
            int x = col * cellW + 3;
            int y = gridTop + row * cellH + 3;
            int cw = cellW - 6;
            int ch = cellH - 6;

            boolean isCursor = (i == cursor);
            boolean isOperator = "\u00F7\u00D7\u2212+=".indexOf(label) >= 0 && label.length() == 1;

            int bg = isCursor ? Theme.PANEL_FOCUS : Theme.PANEL;
            g.setColor(bg);
            g.fillRoundRect(x, y, cw, ch, 6, 6);
            if (isCursor) {
                g.setColor(Theme.ACCENT);
                g.drawRoundRect(x, y, cw, ch, 6, 6);
            }

            g.setColor(isOperator ? Theme.ACCENT : Theme.TEXT);
            int lw = g.getFont().stringWidth(label);
            g.drawString(label, x + cw / 2 - lw / 2, y + ch / 2 - 6, Graphics.TOP | Graphics.LEFT);
        }
    }
}
