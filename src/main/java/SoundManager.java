import javax.microedition.media.Manager;

/**
 * Wraps javax.microedition.media (MMAPI / JSR-135) tone playback.
 *
 * Isolated in its own class on purpose: MMAPI is an OPTIONAL package on
 * MIDP phones. Some Java feature phones simply don't ship it. Keeping the
 * import confined here means that if the class is missing at runtime, the
 * failure happens the first time this class is touched — not when
 * SnakeCanvas (or the MIDlet) loads — and the catch(Throwable) below turns
 * that failure into "sound quietly turns itself off" instead of a crash.
 */
public class SoundManager {

    private boolean enabled = true;

    public void playEat() {
        play(72, 70, 100);        // short high blip
    }

    public void playBonus() {
        play(84, 90, 100);        // higher, slightly longer - distinct from normal food
    }

    public void playLevelUp() {
        play(76, 160, 100);       // a bit longer, mid-high
    }

    public void playHit() {
        play(55, 180, 100);       // low buzz - lost a life but still playing
    }

    public void playGameOver() {
        play(40, 400, 100);       // low, longer tone - run has ended
    }

    private void play(int note, int durationMs, int volume) {
        if (!enabled) {
            return;
        }
        try {
            Manager.playTone(note, durationMs, volume);
        } catch (Throwable t) {
            // Covers both a normal MediaException (tone playback failed)
            // and NoClassDefFoundError (MMAPI not present on this phone at
            // all). Either way, stop trying so we don't pay this cost -
            // and don't risk repeated failures - on every future call.
            enabled = false;
        }
    }
}
