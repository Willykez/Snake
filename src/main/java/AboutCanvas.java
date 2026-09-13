import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

/**
 * Static info screen. Kept as a Canvas (not a Form) purely so it keeps the
 * same visual theme as the rest of the suite instead of the phone's
 * default gray Form styling.
 */
public class AboutCanvas extends Canvas implements CommandListener {

    private final ToolkitMIDlet midlet;
    private final Command backCmd = new Command("Back", Command.BACK, 1);

    public AboutCanvas(ToolkitMIDlet midlet) {
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

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        g.setColor(Theme.BG);
        g.fillRect(0, 0, w, h);

        g.setColor(Theme.ACCENT);
        g.drawString("Village Toolkit", 8, 10, Graphics.TOP | Graphics.LEFT);

        g.setColor(Theme.TEXT);
        String[] lines = {
            "Version 1.0.0",
            "",
            "Offline tools for basic Java",
            "phones: calculator, unit",
            "converter, notes, a small",
            "Swahili-English dictionary,",
            "and a BMI calculator.",
            "",
            "Nothing in this app connects",
            "to the internet or sends",
            "data anywhere - everything",
            "runs and stays on the phone."
        };
        int y = 40;
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], 8, y, Graphics.TOP | Graphics.LEFT);
            y += 16;
        }
    }
}
