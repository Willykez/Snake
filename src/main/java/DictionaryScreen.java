import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;

/**
 * A small bundled Swahili-English phrasebook. Fully offline - the word
 * list ships inside the JAR, so there's nothing to download or connect to.
 */
public class DictionaryScreen implements CommandListener {

    // word, translation pairs - common greetings, numbers, and everyday words
    private static final String[][] WORDS = {
        { "jambo", "hello" },
        { "habari", "news / how are you" },
        { "nzuri", "good / fine" },
        { "asante", "thank you" },
        { "asante sana", "thank you very much" },
        { "karibu", "welcome / you're welcome" },
        { "tafadhali", "please" },
        { "samahani", "sorry / excuse me" },
        { "ndiyo", "yes" },
        { "hapana", "no" },
        { "kwaheri", "goodbye" },
        { "usiku mwema", "good night" },
        { "leo", "today" },
        { "kesho", "tomorrow" },
        { "jana", "yesterday" },
        { "sasa", "now" },
        { "maji", "water" },
        { "chakula", "food" },
        { "nyumba", "house" },
        { "shule", "school" },
        { "shamba", "farm" },
        { "soko", "market" },
        { "pesa", "money" },
        { "bei", "price" },
        { "rafiki", "friend" },
        { "familia", "family" },
        { "mtoto", "child" },
        { "mzee", "elder" },
        { "daktari", "doctor" },
        { "hospitali", "hospital" },
        { "moja", "one" },
        { "mbili", "two" },
        { "tatu", "three" },
        { "nne", "four" },
        { "tano", "five" },
        { "sita", "six" },
        { "saba", "seven" },
        { "nane", "eight" },
        { "tisa", "nine" },
        { "kumi", "ten" },
        { "pole", "sorry (sympathy)" },
        { "haraka", "quickly / hurry" },
        { "polepole", "slowly" },
        { "njia", "road / path" },
        { "gari", "car / vehicle" },
        { "simu", "phone" },
        { "kazi", "work" },
        { "mvua", "rain" },
        { "jua", "sun" },
        { "usiku", "night" }
    };

    private final ToolkitMIDlet midlet;
    private final List mainList;
    private int[] visibleIndices;

    private final Command searchCmd = new Command("Search", Command.SCREEN, 1);
    private final Command clearCmd = new Command("Show all", Command.SCREEN, 2);
    private final Command backCmd = new Command("Back", Command.BACK, 3);
    private final Command goCmd = new Command("Search", Command.OK, 1);
    private final Command cancelCmd = new Command("Cancel", Command.BACK, 2);

    public DictionaryScreen(ToolkitMIDlet midlet) {
        this.midlet = midlet;
        mainList = new List("Dictionary", List.IMPLICIT);
        mainList.addCommand(searchCmd);
        mainList.addCommand(clearCmd);
        mainList.addCommand(backCmd);
        mainList.setCommandListener(this);
        showAll();
    }

    public List getMainList() {
        return mainList;
    }

    private void showAll() {
        int[] all = new int[WORDS.length];
        for (int i = 0; i < WORDS.length; i++) {
            all[i] = i;
        }
        populate(all);
    }

    private void populate(int[] indices) {
        visibleIndices = indices;
        mainList.deleteAll();
        for (int i = 0; i < indices.length; i++) {
            mainList.append(WORDS[indices[i]][0], null);
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (d == mainList) {
            if (c == backCmd) {
                midlet.showLauncher();
            } else if (c == searchCmd) {
                openSearchBox();
            } else if (c == clearCmd) {
                showAll();
            } else if (c == List.SELECT_COMMAND) {
                showDetail();
            }
        } else if (d instanceof TextBox) {
            if (c == goCmd) {
                runSearch(((TextBox) d).getString());
            } else if (c == cancelCmd) {
                midlet.getDisplay().setCurrent(mainList);
            }
        }
    }

    private void openSearchBox() {
        TextBox box = new TextBox("Search", "", 40, 0);
        box.addCommand(goCmd);
        box.addCommand(cancelCmd);
        box.setCommandListener(this);
        midlet.getDisplay().setCurrent(box);
    }

    private void runSearch(String query) {
        String q = query.toLowerCase();
        int matchCount = 0;
        for (int i = 0; i < WORDS.length; i++) {
            if (WORDS[i][0].toLowerCase().indexOf(q) >= 0 || WORDS[i][1].toLowerCase().indexOf(q) >= 0) {
                matchCount++;
            }
        }
        int[] matches = new int[matchCount];
        int pos = 0;
        for (int i = 0; i < WORDS.length; i++) {
            if (WORDS[i][0].toLowerCase().indexOf(q) >= 0 || WORDS[i][1].toLowerCase().indexOf(q) >= 0) {
                matches[pos] = i;
                pos++;
            }
        }
        populate(matches);
        midlet.getDisplay().setCurrent(mainList);
    }

    private void showDetail() {
        int selected = mainList.getSelectedIndex();
        if (selected < 0 || selected >= visibleIndices.length) {
            return;
        }
        int wordIndex = visibleIndices[selected];
        String word = WORDS[wordIndex][0];
        String translation = WORDS[wordIndex][1];
        midlet.sound().playSelect();
        Alert alert = new Alert(word, translation, null, AlertType.INFO);
        alert.setTimeout(Alert.FOREVER);
        midlet.getDisplay().setCurrent(alert, mainList);
    }
}
