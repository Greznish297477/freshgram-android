package tw.nekomimi.nekogram.helpers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.util.ArrayList;

/**
 * Local history of deleted messages (AyuGram / freshGram feature).
 * Stored in a separate database so Telegram's own cache schema is never touched.
 */
public class DeleteHistoryHelper {

    public static final int MAX_VERSIONS_PER_MESSAGE = 100;
    public static final int MAX_TOTAL_ROWS = 20000;

    public static class DeleteEntry {
        public int messageId;
        public String text;
        public int date;
        public long fromId;

        public DeleteEntry(int messageId, String text, int date, long fromId) {
            this.messageId = messageId;
            this.text = text;
            this.date = date;
            this.fromId = fromId;
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
            File path = ApplicationLoader.applicationContext.getDatabasePath("fgram_deleted.db");
            File parent = path.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            database = SQLiteDatabase.openOrCreateDatabase(path, null);
            database.execSQL("CREATE TABLE IF NOT EXISTS deleted (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "dialog_id INTEGER NOT NULL, " +
                    "message_id INTEGER NOT NULL, " +
                    "text TEXT, " +
                    "date INTEGER NOT NULL, " +
                    "from_id INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_msg ON deleted(dialog_id, message_id)");
        } catch (Throwable e) {
            failed = true;
            FileLog.e(e);
            database = null;
        }
        return database;
    }

    public static void recordDelete(long dialogId, int messageId, long fromId, String text, int date) {
        if (dialogId == 0 || messageId == 0 || text == null) {
            return;
        }
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put("dialog_id", dialogId);
            values.put("message_id", messageId);
            values.put("text", text);
            values.put("date", date);
            values.put("from_id", fromId);
            db.insert("deleted", null, values);

            Cursor cursor = db.rawQuery(
                    "SELECT id FROM deleted WHERE dialog_id = ? AND message_id = ? ORDER BY id DESC LIMIT 1 OFFSET ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId), String.valueOf(MAX_VERSIONS_PER_MESSAGE)});
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    long cutoff = cursor.getLong(0);
                    db.delete("deleted", "dialog_id = ? AND message_id = ? AND id <= ?",
                            new String[]{String.valueOf(dialogId), String.valueOf(messageId), String.valueOf(cutoff)});
                }
                cursor.close();
            }
            pruneIfNeeded(db);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void pruneIfNeeded(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM deleted", null);
        int count = 0;
        if (c != null) {
            if (c.moveToFirst()) {
                count = c.getInt(0);
            }
            c.close();
        }
        if (count > MAX_TOTAL_ROWS) {
            db.execSQL("DELETE FROM deleted WHERE id IN (SELECT id FROM deleted ORDER BY id ASC LIMIT 1000)");
        }
    }

    public static boolean hasDeleted(long dialogId, int messageId) {
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            return false;
        }
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM deleted WHERE dialog_id = ? AND message_id = ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (cursor != null) {
                boolean result = cursor.moveToFirst() && cursor.getInt(0) > 0;
                cursor.close();
                return result;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return false;
    }

    public static ArrayList<DeleteEntry> getDeleted(long dialogId, int messageId) {
        ArrayList<DeleteEntry> result = new ArrayList<>();
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            return result;
        }
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT message_id, text, date, from_id FROM deleted WHERE dialog_id = ? AND message_id = ? ORDER BY id ASC",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    result.add(new DeleteEntry(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getLong(3)));
                }
                cursor.close();
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return result;
    }

    /** All saved deleted messages of a dialog, newest first. */
    public static ArrayList<DeleteEntry> getDeletedForDialog(long dialogId, int limit) {
        ArrayList<DeleteEntry> result = new ArrayList<>();
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            return result;
        }
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT message_id, text, date, from_id FROM deleted WHERE dialog_id = ? ORDER BY id DESC LIMIT ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(limit)});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    result.add(new DeleteEntry(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getLong(3)));
                }
                cursor.close();
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return result;
    }

    public static int getCountForDialog(long dialogId) {
        SQLiteDatabase db = getDatabase();
        if (db == null) {
            return 0;
        }
        try {
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM deleted WHERE dialog_id = ?",
                    new String[]{String.valueOf(dialogId)});
            if (cursor != null) {
                int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
                cursor.close();
                return count;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return 0;
    }
}
