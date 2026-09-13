import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;

/**
 * Entry point required by every J2ME app.
 * The phone calls startApp() when the user launches the game
 * from the Applications/Games menu.
 */
public class SnakeMIDlet extends MIDlet {

    private SnakeCanvas canvas;

    protected void startApp() {
        if (canvas == null) {
            canvas = new SnakeCanvas(this);
        }
        Display.getDisplay(this).setCurrent(canvas);
        canvas.startGame();
    }

    protected void pauseApp() {
        if (canvas != null) {
            canvas.pauseGame();
        }
    }

    protected void destroyApp(boolean unconditional) {
        if (canvas != null) {
            canvas.stopGame();
        }
    }

    /** Called by the canvas when the user chooses Exit. */
    public void exitApp() {
        destroyApp(true);
        notifyDestroyed();
    }
}
