package tw.nekomimi.nekogram.helpers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.util.ArrayList;

/**
 * Local history of edited message versions (freshGram feature).
 * Stored in a separate database so Telegram's own cache schema is never touched.
 */
public class EditHistoryHelper {

    public static final int MAX_VERSIONS_PER_MESSAGE = 50;

    public static class EditEntry {
        public String text;
        public int date;
        public int editDate;

        public EditEntry(String text, int date, int editDate) {
            this.text = text;
            this.date = date;
            this.editDate = editDate;
        }
    }

    private static SQLiteDatabase database;
    private static boolean failed;

    private static synchronized SQLiteDatabase getDatabase() {
        if (database != null) {
            return database;
        }
        if (failed) {
            return null;
        }
        try {
            File path = ApplicationLoader.applicationContext.getDatabasePath("fgram_edits.db");
            File parent = path.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            database = SQLiteDatabase.openOrCreateDatabase(path, null);
            database.execSQL("CREATE TABLE IF NOT EXISTS edits (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "dialog_id INTEGER NOT NULL, " +
                    "message_id INTEGER NOT NULL, " +
                    "text TEXT NOT NULL, " +
                    "date INTEGER NOT NULL, " +
                    "edit_date INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_edits_msg ON edits(dialog_id, message_id)");
        } catch (Throwable e) {
            failed = true;
            FileLog.e(e);
            database = null;
        }
        return database;
    }

    /** Stores the previous version of an edited message. Safe to call from any thread. */
    public static void recordEdit(long dialogId, int messageId, String text, int date, int editDate) {
        if (dialogId == 0 || messageId == 0 || text == null || text.isEmpty()) {
            return;
        }
        SQLiteDatabase db = getDatabase();
        if (db == null) {            return;
        }
        try {
            Cursor last = db.rawQuery(
                    "SELECT text FROM edits WHERE dialog_id = ? AND message_id = ? ORDER BY id DESC LIMIT 1",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (last != null) {
                boolean same = last.moveToFirst() && text.equals(last.getString(0));
                last.close();
                if (same) {
                    return;
                }
            }
            ContentValues values = new ContentValues();
            values.put("dialog_id", dialogId);
            values.put("message_id", messageId);
            values.put("text", text);
            values.put("date", date);
            values.put("edit_date", editDate == 0 ? date : editDate);
            long row = db.insert("edits", null, values);            // keep only the newest versions per message
            Cursor cursor = db.rawQuery(
                    "SELECT id FROM edits WHERE dialog_id = ? AND message_id = ? ORDER BY id DESC LIMIT 1 OFFSET ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId), String.valueOf(MAX_VERSIONS_PER_MESSAGE)});
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    long cutoff = cursor.getLong(0);
                    db.delete("edits", "dialog_id = ? AND message_id = ? AND id <= ?",
                            new String[]{String.valueOf(dialogId), String.valueOf(messageId), String.valueOf(cutoff)});
                }
                cursor.close();
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static boolean hasHistory(long dialogId, int messageId) {
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            return false;
        }
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM edits WHERE dialog_id = ? AND message_id = ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (cursor != null) {
                boolean result = cursor.moveToFirst() && cursor.getInt(0) > 0;
                int cnt = cursor.moveToFirst() ? cursor.getInt(0) : 0;
                cursor.close();                return result;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return false;
    }

    /** Returns versions from oldest to newest. */
    public static ArrayList<EditEntry> getHistory(long dialogId, int messageId) {
        ArrayList<EditEntry> result = new ArrayList<>();
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            return result;
        }
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT text, date, edit_date FROM edits WHERE dialog_id = ? AND message_id = ? ORDER BY id ASC",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    result.add(new EditEntry(cursor.getString(0), cursor.getInt(1), cursor.getInt(2)));
                }
                cursor.close();
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return result;
    }

    public static void clearHistory(long dialogId, int messageId) {
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            return;
        }
        try {
            db.delete("edits", "dialog_id = ? AND message_id = ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
