import javax.microedition.media.Manager;

/**
 * Wraps javax.microedition.media (MMAPI / JSR-135) tone playback for UI
 * feedback sounds.
 *
 * Isolated in its own class on purpose: MMAPI is an OPTIONAL package on
 * MIDP phones and some Java feature phones don't ship it. Keeping the
 * import confined here means a missing implementation only disables
 * sound, caught the first time this class is touched, rather than
 * crashing the app.
 */
public class SoundManager {

    private boolean enabled = true;

    public void playNav() {
        play(64, 30, 60);      // very short, quiet - menu movement
    }

    public void playSelect() {
        play(72, 60, 90);      // confirm / open a screen
    }

    public void playSuccess() {
        play(80, 100, 100);    // saved / computed successfully
    }

    public void playError() {
        play(48, 200, 100);    // invalid input, delete, etc.
    }

    private void play(int note, int durationMs, int volume) {
        if (!enabled) {
            return;
        }
        try {
            Manager.playTone(note, durationMs, volume);
        } catch (Throwable t) {
            // Covers a normal MediaException as well as NoClassDefFoundError
            // if MMAPI isn't present at all. Either way, stop trying.
            enabled = false;
        }
    }
}
