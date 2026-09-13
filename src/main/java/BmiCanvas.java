import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

/**
 * BMI calculator. Purely computational - nothing entered here is saved
 * anywhere, so there's no persistent record of anyone's measurements.
 */
public class BmiCanvas extends Canvas implements CommandListener {

    private static final int FOCUS_WEIGHT = 0;
    private static final int FOCUS_HEIGHT = 1;

    private final ToolkitMIDlet midlet;
    private String weightText = "60";
    private String heightText = "170";
    private int focus = FOCUS_WEIGHT;

    private final Command backCmd = new Command("Back", Command.BACK, 1);

    public BmiCanvas(ToolkitMIDlet midlet) {
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
        if (keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9) {
            char digit = (char) ('0' + (keyCode - KEY_NUM0));
            String current = focus == FOCUS_WEIGHT ? weightText : heightText;
            String updated = current.equals("0") ? String.valueOf(digit) : current + digit;
            if (focus == FOCUS_WEIGHT) {
                weightText = updated;
            } else {
                heightText = updated;
            }
            repaint();
            return;
        }
        if (keyCode == KEY_POUND) {
            String current = focus == FOCUS_WEIGHT ? weightText : heightText;
            String updated = current.length() > 1 ? current.substring(0, current.length() - 1) : "0";
            if (focus == FOCUS_WEIGHT) {
                weightText = updated;
            } else {
                heightText = updated;
            }
            repaint();
            return;
        }

        int action = getGameAction(keyCode);
        if (action == Canvas.UP || action == Canvas.DOWN) {
            focus = 1 - focus;
            midlet.sound().playNav();
            repaint();
        }
    }

    private double bmi() {
        double kg = NumberUtil.parse(weightText);
        double cm = NumberUtil.parse(heightText);
        if (cm <= 0) {
            return 0;
        }
        double m = cm / 100.0;
        return kg / (m * m);
    }

    private String category(double b) {
        if (b <= 0) return "-";
        if (b < 18.5) return "Underweight";
        if (b < 25) return "Normal";
        if (b < 30) return "Overweight";
        return "Obese";
    }

    private int categoryColor(double b) {
        if (b <= 0) return Theme.TEXT_DIM;
        if (b < 18.5) return Theme.ACCENT;
        if (b < 25) return Theme.SUCCESS;
        if (b < 30) return Theme.ACCENT;
        return Theme.DANGER;
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        g.setColor(Theme.BG);
        g.fillRect(0, 0, w, h);

        g.setColor(Theme.TEXT);
        g.drawString("BMI Calculator", 8, 4, Graphics.TOP | Graphics.LEFT);

        int top = 30;
        drawRow(g, top, "Weight (kg)", weightText, focus == FOCUS_WEIGHT);
        drawRow(g, top + 40, "Height (cm)", heightText, focus == FOCUS_HEIGHT);

        double b = bmi();
        int resultTop = top + 90;
        g.setColor(Theme.PANEL);
        g.fillRoundRect(8, resultTop, w - 16, 64, 8, 8);

        String bmiStr = "BMI: " + (b > 0 ? NumberUtil.format(b, 1) : "-");
        g.setColor(Theme.TEXT);
        g.drawString(bmiStr, 16, resultTop + 8, Graphics.TOP | Graphics.LEFT);

        String cat = category(b);
        g.setColor(categoryColor(b));
        g.drawString(cat, 16, resultTop + 32, Graphics.TOP | Graphics.LEFT);

        g.setColor(Theme.TEXT_DIM);
        String note = "General guide only, not medical advice";
        g.drawString(note, 8, h - 16, Graphics.TOP | Graphics.LEFT);
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
