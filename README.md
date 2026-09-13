# Village Toolkit — J2ME (CLDC 1.1 / MIDP 2.0)

A multi-module offline utility suite for basic Java feature phones (same
target as the Snake project: phones like the Tecno T528 class of device —
240x320 screen, keypad only). Everything is custom-drawn with a consistent
dark navy / gold theme instead of the phone's default gray MIDP widgets,
wherever the widget type allows it.

## Modules

- **Launcher** — a themed icon grid (not a plain List) with a pulsing
  selection highlight, navigated by arrow keys
- **Calculator** — four-function calculator with a fully custom-drawn
  button grid; operate it either by moving a cursor over the buttons with
  arrows + FIRE, or by typing digits directly on the keypad for speed
  (`*` = decimal point, `#` = equals)
- **Converter** — length (m/km/ft/mi), weight (kg/g/lb), and temperature
  (°C/°F), with a live-updating result as you type; all fixed physical
  constants, so it needs no internet and never goes stale
- **Notes** — a simple notes list, saved permanently via `RecordStore` so
  they survive relaunching the app
- **Dictionary** — a small bundled Swahili-English phrasebook (~50 common
  words/phrases) with search, fully offline since the word list ships
  inside the JAR
- **BMI calculator** — computes only; nothing entered here is saved
  anywhere, so there's no persistent record of any measurement
- **About** — app info, in the same visual theme as everything else

## What's in here

```
toolkit-j2me/
├── src/main/java/
│   ├── ToolkitMIDlet.java     – entry point, owns navigation between screens
│   ├── Theme.java             – shared color palette
│   ├── NumberUtil.java        – manual decimal parse/format (see note below)
│   ├── SoundManager.java      – isolated MMAPI tone playback with safe fallback
│   ├── LauncherCanvas.java    – home screen icon grid
│   ├── CalculatorCanvas.java  – calculator
│   ├── ConverterCanvas.java   – unit converter
│   ├── BmiCanvas.java         – BMI calculator
│   ├── NotesScreen.java       – notes list + editor (RecordStore-backed)
│   ├── DictionaryScreen.java  – phrasebook + search
│   └── AboutCanvas.java       – about screen
├── src/main/resources/
│   └── icon.png               – app icon (32x32), packaged into the JAR root
├── res/
│   └── icon-16.png            – smaller icon, in case 32x32 doesn't display well
├── .github/workflows/
│   └── build.yml              – compiles the app on every push, no local setup needed
├── pom.xml                    – Maven build definition
├── manifest.mf                – MIDlet metadata, reused for both the JAR and the JAD
└── README.md
```

### Why NumberUtil exists instead of using Double.parseDouble/toString

The Snake game's CI build failed on `Math.random()`, which turned out not
to exist in CLDC's stripped-down `java.lang.Math` at all - it's a J2SE-only
method. That was a useful lesson: several things that feel like "obviously
present" basics aren't guaranteed in CLDC. Rather than gamble the same way
on `Double.parseDouble`/`Double.toString` (commonly present in CLDC 1.1,
but not worth re-risking), the calculator, converter, and BMI screens all
route through a small hand-written parser/formatter using only integer
arithmetic and `Long.toString`, which are unambiguously part of the spec.

## Building — same GitHub Actions flow as the Snake project

No PC or OS-specific installer needed:

1. Create a new GitHub repository
2. Upload every file in this folder, preserving the folder structure
   exactly as shown above (the `.github/workflows/build.yml` path matters)
3. Commit/push to `main`
4. Open the repo's **Actions** tab — a run starts automatically
5. When it turns green, scroll to **Artifacts** and download
   `toolkit-j2me-build.zip`, containing `Toolkit.jar` and `Toolkit.jad`

Same build mechanics as Snake: CLDC/MIDP API stubs pulled from Maven
Central (published by the MicroEmulator project), preverification done via
ProGuard's `-microedition` mode instead of the old native `preverify` tool.

### The one dependency I still can't fully verify

Same caveat as before: `SoundManager.java`'s MMAPI sound calls compile
against `microemu-jsr-135`, which I can't independently verify from here.
If the Actions log fails specifically on `SoundManager.java`, paste me the
error - or just delete that file, remove its `pom.xml` dependency block,
and remove `midlet.sound()`/`new SoundManager()` references from the other
files. Everything else keeps working without it, since sound was built as
a non-essential layer throughout, same as in Snake.

### Untested until CI runs

This is a much larger codebase than Snake, so treat it as more likely to
need at least one round of CI-driven fixes, the same way Snake did. If
anything fails, paste the error back and I'll fix it directly against the
real compiler output rather than guessing.

## Installing on the phone

1. Copy both `Toolkit.jar` and `Toolkit.jad` from the downloaded zip onto
   a microSD card
2. Insert the card into the phone
3. Open the phone's file manager, navigate to the files
4. Select `Toolkit.jad` (or `Toolkit.jar` if the phone doesn't need the
   JAD) to start installation
5. Find "Village Toolkit" under the phone's Applications menu and launch it

## Controls

- Launcher: arrows to move between modules, FIRE to open
- Calculator: arrows + FIRE to press an on-screen button, or type digits
  directly (`*` = decimal point, `#` = equals)
- Converter/BMI: UP/DOWN to move between fields, LEFT/RIGHT to change a
  selection, digits to type a value, `#` to backspace
- Notes: select a note to open it, "Add" for a new one, "Delete" to remove
  the selected one
- Dictionary: "Search" to filter, select a word to see its translation,
  "Show all" to reset the list
- "Back" soft-key on every screen returns to the launcher; "Exit" on the
  launcher quits the app

## About the app icon

Same approach as Snake: a plain, indexed-palette PNG with no
transparency, referenced from `manifest.mf`'s `MIDlet-1` line. A number of
very basic/budget Java phones ignore custom MIDlet icons entirely - that's
a phone limitation, not fixable from the app side, and doesn't affect
whether the app runs.
