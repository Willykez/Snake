import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;

/**
 * Entry point for the Village Toolkit suite. Holds the shared Display and
 * exposes simple show*() methods each screen calls to navigate - this
 * keeps navigation logic out of individual screens.
 */
public class ToolkitMIDlet extends MIDlet {

    private Display display;
    private LauncherCanvas launcher;
    private final SoundManager sound = new SoundManager();

    protected void startApp() {
        display = Display.getDisplay(this);
        if (launcher == null) {
            launcher = new LauncherCanvas(this);
        }
        display.setCurrent(launcher);
    }

    protected void pauseApp() {
        // nothing to release between foreground sessions
    }

    protected void destroyApp(boolean unconditional) {
        // nothing persistent to close - each screen closes its own RecordStore
    }

    public void exitApp() {
        destroyApp(true);
        notifyDestroyed();
    }

    public SoundManager sound() {
        return sound;
    }

    public void showLauncher() {
        display.setCurrent(launcher);
    }

    public void showCalculator() {
        display.setCurrent(new CalculatorCanvas(this));
    }

    public void showConverter() {
        display.setCurrent(new ConverterCanvas(this));
    }

    public void showNotes() {
        display.setCurrent(new NotesScreen(this).getMainList());
    }

    public void showDictionary() {
        display.setCurrent(new DictionaryScreen(this).getMainList());
    }

    public void showBmi() {
        display.setCurrent(new BmiCanvas(this));
    }

    public void showAbout() {
        display.setCurrent(new AboutCanvas(this));
    }

    public Display getDisplay() {
        return display;
    }
}
