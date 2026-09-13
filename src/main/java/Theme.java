/**
 * Shared visual theme so every screen in the suite looks consistent.
 * A dark background with a warm gold accent reads well on the low-contrast,
 * often passive-matrix screens common on basic phones - much better than
 * light backgrounds, which tend to wash out.
 */
public final class Theme {
    public static final int BG = 0x0B1E33;        // deep navy background
    public static final int PANEL = 0x123B5C;      // card / button background
    public static final int PANEL_FOCUS = 0x1C4E78; // focused card / button background
    public static final int ACCENT = 0xF2B705;      // gold accent (selection, highlights)
    public static final int ACCENT_DIM = 0x9C7A1E;
    public static final int TEXT = 0xFFFFFF;
    public static final int TEXT_DIM = 0xA9C1D9;
    public static final int SUCCESS = 0x3DDC97;
    public static final int DANGER = 0xE85D5D;

    private Theme() {
    }
}
