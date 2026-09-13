import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

/**
 * Length / weight / temperature converter. All conversion factors are
 * fixed physical constants (not exchange-rate style data that goes stale),
 * so this works fully offline forever.
 */
public class ConverterCanvas extends Canvas implements CommandListener {

    private static final int FOCUS_CATEGORY = 0;
    private static final int FOCUS_FROM_UNIT = 1;
    private static final int FOCUS_VALUE = 2;
    private static final int FOCUS_TO_UNIT = 3;

    private static final String[] CATEGORIES = { "Length", "Weight", "Temperature" };
    private static final String[][] UNITS = {
        { "m", "km", "ft", "mi" },
        { "kg", "g", "lb" },
        { "\u00B0C", "\u00B0F" }
    };
    // meters per unit / kilograms per unit; unused for Temperature (special-cased)
    private static final double[][] FACTORS = {
        { 1.0, 1000.0, 0.3048, 1609.344 },
        { 1.0, 0.001, 0.45359237 },
        { 1, 1 }
    };

    private final ToolkitMIDlet midlet;
    private int category = 0;
    private int fromUnit = 0;
    private int toUnit = 1;
    private String valueText = "1";
    private int focus = FOCUS_VALUE;

    private final Command backCmd = new Command("Back", Command.BACK, 1);

    public ConverterCanvas(ToolkitMIDlet midlet) {
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
        if (focus == FOCUS_VALUE && keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9) {
            char digit = (char) ('0' + (keyCode - KEY_NUM0));
            if (valueText.equals("0")) {
                valueText = String.valueOf(digit);
            } else {
                valueText = valueText + digit;
            }
            repaint();
            return;
        }
        if (focus == FOCUS_VALUE && keyCode == KEY_STAR) {
            if (valueText.indexOf('.') < 0) {
                valueText = valueText + ".";
            }
            repaint();
            return;
        }
        if (focus == FOCUS_VALUE && keyCode == KEY_POUND) {
            if (valueText.length() > 1) {
                valueText = valueText.substring(0, valueText.length() - 1);
            } else {
                valueText = "0";
            }
            repaint();
            return;
        }

        int action = getGameAction(keyCode);
        switch (action) {
            case Canvas.UP:
                if (focus > FOCUS_CATEGORY) { focus--; midlet.sound().playNav(); }
                break;
            case Canvas.DOWN:
                if (focus < FOCUS_TO_UNIT) { focus++; midlet.sound().playNav(); }
                break;
            case Canvas.LEFT:
                cycle(-1);
                break;
            case Canvas.RIGHT:
                cycle(1);
                break;
            default:
                break;
        }
        repaint();
    }

    private void cycle(int dir) {
        int unitCount = UNITS[category].length;
        if (focus == FOCUS_CATEGORY) {
            category = wrap(category + dir, CATEGORIES.length);
            fromUnit = 0;
            toUnit = UNITS[category].length > 1 ? 1 : 0;
            midlet.sound().playNav();
        } else if (focus == FOCUS_FROM_UNIT) {
            fromUnit = wrap(fromUnit + dir, unitCount);
            midlet.sound().playNav();
        } else if (focus == FOCUS_TO_UNIT) {
            toUnit = wrap(toUnit + dir, unitCount);
            midlet.sound().playNav();
        }
    }

    private int wrap(int v, int mod) {
        int r = v % mod;
        return r < 0 ? r + mod : r;
    }

    private double convert() {
        double input = NumberUtil.parse(valueText);
        if (category == 2) {
            boolean fromC = fromUnit == 0;
            boolean toC = toUnit == 0;
            double celsius = fromC ? input : (input - 32) * 5.0 / 9.0;
            if (toC) {
                return celsius;
            }
            return celsius * 9.0 / 5.0 + 32;
        }
        double base = input * FACTORS[category][fromUnit];
        return base / FACTORS[category][toUnit];
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        g.setColor(Theme.BG);
        g.fillRect(0, 0, w, h);

        g.setColor(Theme.TEXT);
        g.drawString("Converter", 8, 4, Graphics.TOP | Graphics.LEFT);

        int rowH = 40;
        int top = 28;

        drawRow(g, top, "Category", CATEGORIES[category], focus == FOCUS_CATEGORY);
        drawRow(g, top + rowH, "From unit", UNITS[category][fromUnit], focus == FOCUS_FROM_UNIT);
        drawRow(g, top + rowH * 2, "Value", valueText, focus == FOCUS_VALUE);
        drawRow(g, top + rowH * 3, "To unit", UNITS[category][toUnit], focus == FOCUS_TO_UNIT);

        int resultTop = top + rowH * 4 + 10;
        g.setColor(Theme.PANEL);
        g.fillRoundRect(8, resultTop, w - 16, 46, 8, 8);
        String resultStr = NumberUtil.format(convert(), category == 2 ? 1 : 3) + " " + UNITS[category][toUnit];
        g.setColor(Theme.SUCCESS);
        int rw = g.getFont().stringWidth(resultStr);
        g.drawString(resultStr, w / 2 - rw / 2, resultTop + 14, Graphics.TOP | Graphics.LEFT);

        g.setColor(Theme.TEXT_DIM);
        String hint = "UP/DOWN row, LEFT/RIGHT change, digits to type";
        g.drawString(hint, 8, h - 16, Graphics.TOP | Graphics.LEFT);
    }

    private void drawRow(Graphics g, int y, String label, String value, boolean isFocused) {
        int w = getWidth();
        int x = 8;
        int rowW = w - 16;
        int rowH = 34;

        g.setColor(isFocused ? Theme.PANEL_FOCUS : Theme.PANEL);
        g.fillRoundRect(x, y, rowW, rowH, 6, 6);
        if (isFocused) {
            g.setColor(Theme.ACCENT);
            g.drawRoundRect(x, y, rowW, rowH, 6, 6);
        }

        g.setColor(Theme.TEXT_DIM);
        g.drawString(label, x + 8, y + 9, Graphics.TOP | Graphics.LEFT);

        g.setColor(isFocused ? Theme.ACCENT : Theme.TEXT);
        int vw = g.getFont().stringWidth(value);
        g.drawString(value, x + rowW - 8 - vw, y + 9, Graphics.TOP | Graphics.LEFT);
    }
}
