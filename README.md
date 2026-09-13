# Snake — J2ME (CLDC 1.1 / MIDP 2.0)

A Canvas-based Snake game for basic Java feature phones (tested target:
phones like the Tecno T528 class of device — 240x320 screen, keypad only),
with a full game loop, difficulty curve, and a title/menu screen. See the
feature list at the bottom of this file.

## What's in here

```
snake-j2me/
├── src/main/java/
│   ├── SnakeMIDlet.java   – app entry point / lifecycle
│   ├── SnakeCanvas.java   – game logic, rendering, title screen, high scores
│   └── SoundManager.java  – isolated MMAPI tone playback with safe fallback
├── src/main/resources/
│   └── icon.png           – app icon (32x32), packaged into the JAR root
├── res/
│   └── icon-16.png        – smaller icon, in case 32x32 doesn't display well
├── .github/workflows/
│   └── build.yml          – compiles the game on every push, no local setup needed
├── pom.xml                – Maven build definition
├── manifest.mf            – MIDlet metadata, reused for both the JAR and the JAD
└── README.md
```

## Building it — no computer setup required

This project is wired to build itself on **GitHub's own servers** via
GitHub Actions, so you don't need a PC, a specific OS, or any of the old
J2ME toolkit installers:

1. Create a new GitHub repository (public or private both work)
2. Upload every file in this folder into it, preserving the folder
   structure exactly as shown above (the `.github/workflows/build.yml`
   path matters — GitHub only picks up workflows from that exact path)
3. Commit to the `main` branch (or push if using git directly)
4. Go to the repo's **Actions** tab — a workflow run called "Build J2ME
   MIDlet" should start automatically within a few seconds
5. Click into the run, wait for the green checkmark (usually under a
   couple of minutes)
6. Scroll to the **Artifacts** section at the bottom of the run page —
   download `snake-j2me-build.zip`, which contains `Snake.jar` and
   `Snake.jad`

That zip is what you copy onto the microSD card — see "Installing on the
phone" below.

### How the build actually works, if you're curious

- **No manual jar-hunting**: instead of the old approach of downloading
  Oracle's discontinued Java ME SDK/WTK installer, `pom.xml` pulls the
  CLDC 1.1 and MIDP 2.0 API class stubs straight from Maven Central
  (published by the open-source MicroEmulator project), so the compiler
  has something to check your code against
- **No native preverify tool**: instead of the old `preverify` binary
  (which has no clean way to run on GitHub's Linux runners), the build
  uses ProGuard's `-microedition` mode, which performs the same
  preverification step in pure Java
- Both of these choices come from a documented, working 2026 approach to
  J2ME development without the old desktop toolchain

### One dependency I couldn't fully verify

Everything above is based on a confirmed-working pattern. The one piece
I couldn't independently verify from here is the `microemu-jsr-135`
dependency in `pom.xml`, which supplies the compile-time classes for
`SoundManager.java`'s sound calls. If the Actions run fails and the log
points specifically at `SoundManager.java`, paste me the error and I'll
fix it — or, since sound was deliberately built as an isolated,
non-essential piece, you can just delete `SoundManager.java`, remove its
dependency block from `pom.xml`, and remove the two lines in
`SnakeCanvas.java` that reference `sound.play...()` and `new
SoundManager()`. Everything else keeps working exactly the same without
it.

## Installing on the phone

1. Copy both `Snake.jar` and `Snake.jad` from the downloaded zip onto a
   microSD card
2. Insert the card into the phone
3. Open the phone's file manager, navigate to the files
4. Select `Snake.jad` (or `Snake.jar` if the phone doesn't need the JAD)
   to start installation
5. Find "Snake" under the phone's Applications/Games menu and launch it

## Controls

- Title screen: UP/DOWN to choose Start game / Exit, FIRE to select
- In game: arrow keys steer the snake, FIRE pauses/resumes
- After game over: FIRE returns to the title screen
- Exit soft-key — quits the app

## Game features

- **Title screen**: shows your best level/score, with Start game / Exit
  options navigated by UP/DOWN and selected with FIRE
- **Levels**: every 5 pieces of food eaten advances you to the next level
- **Obstacle mazes**: each level regenerates a denser wall layout (gray
  blocks) that ends the run if you hit them, same as hitting yourself
- **Increasing speed**: the snake moves faster every level, down to a
  floor speed so it never becomes literally unplayable
- **3 lives**: dying resets your snake (not your score or level), so a
  slip doesn't end a long run immediately
- **Bonus food**: occasionally a gold square appears worth 30 points and
  blinks out after a short time if not eaten
- **Sound cues**: short tones on eating, bonus, level-up, losing a life,
  and game over — played via MMAPI. If the phone doesn't support MMAPI,
  sound silently disables itself; nothing else is affected
- **Persistent best run**: high score and highest level reached are saved
  on-device via `RecordStore` and shown on the title screen next launch

## About the app icon

- `MIDlet-1` in `manifest.mf` points at `/icon.png` inside the JAR — Maven
  packages `src/main/resources/icon.png` there automatically
- Plain PNG, indexed 8-color palette, no transparency, for maximum
  compatibility with old MIDP phones
- A number of very basic/budget Java phones ignore custom MIDlet icons
  entirely and always show their own generic "Java app" icon regardless
  of what's packaged — that's a phone limitation, not fixable from the
  app side, and doesn't affect whether the game runs

## If it doesn't install at all

This means the phone either doesn't expose a Java app installer, or
expects a different MIDP/CLDC version. Before assuming the code is at
fault, try:
- Installing a known-working, very simple public-domain MIDlet from an
  old Java-phone community site to confirm the phone can install
  *anything*
- Checking the phone's own menu for something called "Java," "Application
  Manager," or "Install App"
