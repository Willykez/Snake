import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordEnumeration;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * A simple notes list. Notes are plain text, stored one per RecordStore
 * record using DataOutputStream.writeUTF/DataInputStream.readUTF, which
 * are the standard, portable way to persist Strings in CLDC/MIDP rather
 * than relying on String.getBytes()/String(byte[]), whose availability in
 * CLDC is less certain.
 */
public class NotesScreen implements CommandListener {

    private static final String STORE = "toolkit_notes";
    private static final int MAX_PREVIEW = 34;

    private final ToolkitMIDlet midlet;
    private final List mainList;

    private final Command addCmd = new Command("Add", Command.SCREEN, 1);
    private final Command openCmd = new Command("Open", Command.ITEM, 1);
    private final Command deleteCmd = new Command("Delete", Command.SCREEN, 2);
    private final Command backCmd = new Command("Back", Command.BACK, 3);

    private final Command saveCmd = new Command("Save", Command.SCREEN, 1);
    private final Command cancelCmd = new Command("Cancel", Command.BACK, 2);

    // recordId parallel to the visible list rows, so a selection maps
    // back to the right RecordStore record even after deletions
    private int[] recordIds = new int[0];

    private int editingRecordId = -1; // -1 means "adding a new note"

    public NotesScreen(ToolkitMIDlet midlet) {
        this.midlet = midlet;
        mainList = new List("Notes", List.IMPLICIT);
        mainList.addCommand(addCmd);
        mainList.addCommand(openCmd);
        mainList.addCommand(deleteCmd);
        mainList.addCommand(backCmd);
        mainList.setCommandListener(this);
        reload();
    }

    public List getMainList() {
        return mainList;
    }

    private void reload() {
        mainList.deleteAll();
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            RecordEnumeration en = rs.enumerateRecords(null, null, false);
            int count = en.numRecords();
            recordIds = new int[count];
            int i = 0;
            while (en.hasNextElement()) {
                int id = en.nextRecordId();
                recordIds[i] = id;
                String text = readNote(rs, id);
                mainList.append(preview(text), null);
                i++;
            }
            en.destroy();
        } catch (RecordStoreException e) {
            // start with an empty list if the store can't be opened
            recordIds = new int[0];
        } finally {
            closeQuietly(rs);
        }
    }

    private String preview(String text) {
        String firstLine = text;
        int nl = text.indexOf('\n');
        if (nl >= 0) {
            firstLine = text.substring(0, nl);
        }
        if (firstLine.length() > MAX_PREVIEW) {
            return firstLine.substring(0, MAX_PREVIEW) + "...";
        }
        return firstLine.length() == 0 ? "(empty note)" : firstLine;
    }

    private String readNote(RecordStore rs, int id) {
        try {
            byte[] data = rs.getRecord(id);
            ByteArrayInputStream bin = new ByteArrayInputStream(data);
            DataInputStream din = new DataInputStream(bin);
            String text = din.readUTF();
            din.close();
            return text;
        } catch (IOException e) {
            return "";
        } catch (RecordStoreException e) {
            return "";
        }
    }

    private void writeNote(RecordStore rs, String text) throws RecordStoreException {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            dout.writeUTF(text);
            dout.close();
            byte[] data = bout.toByteArray();
            if (editingRecordId < 0) {
                rs.addRecord(data, 0, data.length);
            } else {
                rs.setRecord(editingRecordId, data, 0, data.length);
            }
        } catch (IOException e) {
            throw new RecordStoreException("encode failed");
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (d == mainList) {
            handleMainListCommand(c);
        } else if (d instanceof TextBox) {
            handleEditorCommand(c, (TextBox) d);
        }
    }

    private void handleMainListCommand(Command c) {
        if (c == backCmd) {
            midlet.showLauncher();
        } else if (c == addCmd) {
            openEditor(-1, "");
        } else if (c == openCmd || c == List.SELECT_COMMAND) {
            int index = mainList.getSelectedIndex();
            if (index >= 0 && index < recordIds.length) {
                openEditor(recordIds[index], loadNoteById(recordIds[index]));
            }
        } else if (c == deleteCmd) {
            int index = mainList.getSelectedIndex();
            if (index >= 0 && index < recordIds.length) {
                deleteNote(recordIds[index]);
            }
        }
    }

    private String loadNoteById(int id) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            return readNote(rs, id);
        } catch (RecordStoreException e) {
            return "";
        } finally {
            closeQuietly(rs);
        }
    }

    private void openEditor(int recordId, String text) {
        editingRecordId = recordId;
        TextBox box = new TextBox(recordId < 0 ? "New note" : "Edit note", text, 2000, 0);
        box.addCommand(saveCmd);
        box.addCommand(cancelCmd);
        box.setCommandListener(this);
        midlet.getDisplay().setCurrent(box);
    }

    private void handleEditorCommand(Command c, TextBox box) {
        if (c == saveCmd) {
            saveNote(box.getString());
            midlet.sound().playSuccess();
            reload();
            midlet.getDisplay().setCurrent(mainList);
        } else if (c == cancelCmd) {
            midlet.getDisplay().setCurrent(mainList);
        }
    }

    private void saveNote(String text) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            writeNote(rs, text);
        } catch (RecordStoreException e) {
            // note simply won't persist this time; nothing else to do here
        } finally {
            closeQuietly(rs);
        }
    }

    private void deleteNote(int id) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            rs.deleteRecord(id);
            midlet.sound().playError();
        } catch (RecordStoreException e) {
            Alert alert = new Alert("Error", "Could not delete note.", null, AlertType.ERROR);
            midlet.getDisplay().setCurrent(alert, mainList);
            return;
        } finally {
            closeQuietly(rs);
        }
        reload();
    }

    private void closeQuietly(RecordStore rs) {
        if (rs != null) {
            try { rs.closeRecordStore(); } catch (RecordStoreException e) { /* ignore */ }
        }
    }
}
