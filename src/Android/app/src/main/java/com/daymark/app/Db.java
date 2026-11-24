package com.daymark.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Local offline storage. V2 keeps the V1 schema intact and applies additive,
 * idempotent migrations after copying a timestamped backup of the V1 database.
 * No network access anywhere.
 */
public class Db extends SQLiteOpenHelper {
    private static final int VERSION = 2;
    private static final String MIGRATION_PREFS = "daymark_migration_status";
    private static final String[] REQUIRED_TABLES = {"tasks", "notes", "settings"};
    private final Context context;

    public boolean lastBackupOk = true;
    public String lastBackupPath;
    public String lastMigrationBlockReason;
    public String lastMigrationFailure;

    /** Thrown when the verified pre-migration backup cannot be produced. */
    public static class MigrationBlockedException extends RuntimeException {
        public final String reason;

        MigrationBlockedException(String reason) {
            super("Migration blocked: " + reason);
            this.reason = reason;
        }
    }

    /** Thrown only when migration fails after a verified safety backup exists. */
    public static class PostBackupMigrationException extends RuntimeException {
        public final String detail;

        PostBackupMigrationException(String detail, Throwable cause) {
            super("Migration failed after backup: " + detail, cause);
            this.detail = detail;
        }
    }

    public Db(Context context) {
        super(context, "daymark.db", null, VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            InputStream in = context.getResources().openRawResource(R.raw.daymark_schema);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) continue;
                sql.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    db.execSQL(sql.toString());
                    sql.setLength(0);
                }
            }
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise Daymark database", e);
        }
        upgradeToV2(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion >= 2 || newVersion < 2) return;

        // SQLiteOpenHelper calls onUpgrade inside a transaction. Reading the source
        // through the same open connection and writing a separate SQLite file avoids
        // a blind file copy that could miss journal/WAL state.
        String verifiedBackup = createVerifiedBackup(db);
        lastBackupPath = verifiedBackup;
        lastBackupOk = verifiedBackup != null;

        if (verifiedBackup == null) {
            lastMigrationBlockReason = "backup_unverified";
            recordMigrationStatus("blocked", "backup_unverified", "");
            throw new MigrationBlockedException("backup_unverified");
        }

        try {
            upgradeToV2(db);
            recordMigrationStatus("ok", "", "");
        } catch (RuntimeException e) {
            lastMigrationFailure = e.getClass().getSimpleName();
            recordMigrationStatus("post_backup_failed", "migration_failed", lastMigrationFailure);
            throw new PostBackupMigrationException(lastMigrationFailure, e);
        }
    }

    /**
     * Creates a complete logical backup using SQLite itself, verifies it, and only
     * then returns its path. Returns null on any failure; the caller must abort.
     */
    private String createVerifiedBackup(SQLiteDatabase source) {
        File dbFile = context.getDatabasePath("daymark.db");
        if (dbFile == null || !dbFile.exists() || dbFile.length() == 0) return null;

        File dir = new File(dbFile.getParentFile(), "backups");
        if (!dir.exists() && !dir.mkdirs()) return null;

        SchemaSnapshot schema = readSchema(source);
        if (schema == null || !hasRequiredTables(schema)) return null;

        // A failed migration keeps its verified backup. Reuse it instead of making
        // another copy on every retry; never delete or replace existing backups.
        String reusable = findExistingVerifiedBackup(dir, source, schema);
        if (reusable != null) return reusable;

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File finalFile = uniqueBackupFile(dir, "daymark-pre-v2-" + stamp + ".db");
        File tmpFile = new File(dir, finalFile.getName() + ".tmp");
        SQLiteDatabase backup = null;

        try {
            backup = SQLiteDatabase.openOrCreateDatabase(tmpFile, null);
            backup.beginTransaction();
            for (SchemaObject table : schema.tables) backup.execSQL(table.sql);
            for (SchemaObject table : schema.tables) copyRows(source, backup, table.name);
            for (SchemaObject other : schema.other) backup.execSQL(other.sql);
            backup.setTransactionSuccessful();
            backup.endTransaction();
            backup.close();
            backup = null;

            if (!verifyBackup(tmpFile, source, schema)) return null;
            if (!tmpFile.renameTo(finalFile)) return null;
            if (!verifyBackup(finalFile, source, schema)) return null;

            return finalFile.getAbsolutePath();
        } catch (Exception e) {
            lastMigrationFailure = e.getClass().getSimpleName();
            return null;
        } finally {
            if (backup != null) {
                try { if (backup.inTransaction()) backup.endTransaction(); } catch (Exception ignored) { }
                try { backup.close(); } catch (Exception ignored) { }
            }
            // Only the temporary work file is removed. Existing backups are never deleted.
            if (tmpFile.exists()) {
                try { tmpFile.delete(); } catch (Exception ignored) { }
            }
        }
    }

    private static String findExistingVerifiedBackup(File dir, SQLiteDatabase source, SchemaSnapshot schema) {
        File[] candidates = dir.listFiles((d, name) ->
                name.startsWith("daymark-pre-v2-") && name.endsWith(".db"));
        if (candidates == null) return null;
        for (File candidate : candidates) {
            if (candidate.isFile() && verifyBackup(candidate, source, schema)) {
                return candidate.getAbsolutePath();
            }
        }
        return null;
    }

    private static SchemaSnapshot readSchema(SQLiteDatabase source) {
        SchemaSnapshot schema = new SchemaSnapshot();
        Cursor c = source.rawQuery(
                "SELECT type,name,sql FROM sqlite_master " +
                "WHERE sql IS NOT NULL AND name NOT LIKE 'sqlite_%' " +
                "ORDER BY CASE type WHEN 'table' THEN 0 WHEN 'index' THEN 1 ELSE 2 END, name", null);
        try {
            while (c.moveToNext()) {
                String type = c.getString(0);
                String name = c.getString(1);
                String sql = c.getString(2);
                if (type == null || name == null || sql == null || sql.trim().isEmpty()) continue;
                SchemaObject object = new SchemaObject(type, name, sql);
                if ("table".equalsIgnoreCase(type)) schema.tables.add(object);
                else schema.other.add(object);
            }
        } finally {
            c.close();
        }
        return schema;
    }

    private static boolean hasRequiredTables(SchemaSnapshot schema) {
        for (String required : REQUIRED_TABLES) {
            boolean found = false;
            for (SchemaObject table : schema.tables) {
                if (required.equalsIgnoreCase(table.name)) { found = true; break; }
            }
            if (!found) return false;
        }
        return true;
    }

    private static void copyRows(SQLiteDatabase source, SQLiteDatabase backup, String table) {
        Cursor c = source.rawQuery("SELECT * FROM " + quoteIdentifier(table), null);
        try {
            String[] columns = c.getColumnNames();
            if (columns == null || columns.length == 0) return;

            StringBuilder sql = new StringBuilder("INSERT INTO ")
                    .append(quoteIdentifier(table)).append(" (");
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) sql.append(',');
                sql.append(quoteIdentifier(columns[i]));
            }
            sql.append(") VALUES (");
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) sql.append(',');
                sql.append('?');
            }
            sql.append(')');

            SQLiteStatement statement = backup.compileStatement(sql.toString());
            try {
                while (c.moveToNext()) {
                    statement.clearBindings();
                    for (int i = 0; i < columns.length; i++) {
                        switch (c.getType(i)) {
                            case Cursor.FIELD_TYPE_NULL: statement.bindNull(i); break;
                            case Cursor.FIELD_TYPE_INTEGER: statement.bindLong(i, c.getLong(i)); break;
                            case Cursor.FIELD_TYPE_FLOAT: statement.bindDouble(i, c.getDouble(i)); break;
                            case Cursor.FIELD_TYPE_BLOB: statement.bindBlob(i, c.getBlob(i)); break;
                            default: statement.bindString(i, c.getString(i)); break;
                        }
                    }
                    statement.executeInsert();
                }
            } finally {
                statement.close();
            }
        } finally {
            c.close();
        }
    }

    private static boolean verifyBackup(File file, SQLiteDatabase source, SchemaSnapshot schema) {
        if (file == null || !file.exists() || file.length() == 0) return false;
        SQLiteDatabase check = null;
        try {
            check = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);

            Cursor integrity = check.rawQuery("PRAGMA integrity_check", null);
            boolean ok = false;
            if (integrity.moveToNext()) ok = "ok".equalsIgnoreCase(integrity.getString(0));
            integrity.close();
            if (!ok) return false;

            for (String required : REQUIRED_TABLES) {
                if (!hasTable(check, required)) return false;
            }
            for (SchemaObject table : schema.tables) {
                long sourceCount = countRows(source, table.name);
                long backupCount = countRows(check, table.name);
                if (sourceCount < 0 || sourceCount != backupCount) return false;
                if (!contentMatches(source, check, table.name)) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (check != null) {
                try { check.close(); } catch (Exception ignored) { }
            }
        }
    }

    /** Compares every value in a table so a same-row-count but damaged backup is rejected. */
    private static boolean contentMatches(SQLiteDatabase source, SQLiteDatabase backup, String table) {
        String sql = "SELECT * FROM " + quoteIdentifier(table) + " ORDER BY 1";
        Cursor a = source.rawQuery(sql, null);
        Cursor b = backup.rawQuery(sql, null);
        try {
            if (a.getCount() != b.getCount()) return false;
            String[] columns = a.getColumnNames();
            while (a.moveToNext()) {
                if (!b.moveToNext()) return false;
                for (int i = 0; i < columns.length; i++) {
                    int typeA = a.getType(i);
                    if (typeA != b.getType(i)) return false;
                    switch (typeA) {
                        case Cursor.FIELD_TYPE_NULL: break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            if (a.getLong(i) != b.getLong(i)) return false;
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            if (Double.compare(a.getDouble(i), b.getDouble(i)) != 0) return false;
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            if (!Arrays.equals(a.getBlob(i), b.getBlob(i))) return false;
                            break;
                        default:
                            if (!a.getString(i).equals(b.getString(i))) return false;
                            break;
                    }
                }
            }
            return !b.moveToNext();
        } finally {
            a.close();
            b.close();
        }
    }

    private static boolean hasTable(SQLiteDatabase db, String table) {
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{table});
        try {
            return c.moveToNext() && c.getInt(0) > 0;
        } finally {
            c.close();
        }
    }

    private static long countRows(SQLiteDatabase db, String table) {
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + quoteIdentifier(table), null);
            try {
                return c.moveToNext() ? c.getLong(0) : -1;
            } finally {
                c.close();
            }
        } catch (Exception e) {
            return -1;
        }
    }

    private static String quoteIdentifier(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static File uniqueBackupFile(File dir, String base) {
        File candidate = new File(dir, base);
        int dot = base.lastIndexOf('.');
        String stem = dot > 0 ? base.substring(0, dot) : base;
        String extension = dot > 0 ? base.substring(dot) : "";
        int suffix = 2;
        while (candidate.exists()) {
            candidate = new File(dir, stem + "-" + suffix + extension);
            suffix++;
        }
        return candidate;
    }

    /** Records a generic, non-sensitive migration status for support diagnostics. */
    private void recordMigrationStatus(String status, String reason, String detail) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("status", status)
                    .putString("reason", reason == null ? "" : reason)
                    .putString("detail", detail == null ? "" : detail)
                    .putString("at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date()))
                    .apply();
        } catch (Exception ignored) {
            // Never let diagnostics block the safety decision.
        }
    }

    private static final class SchemaObject {
        final String type;
        final String name;
        final String sql;

        SchemaObject(String type, String name, String sql) {
            this.type = type;
            this.name = name;
            this.sql = sql;
        }
    }

    private static final class SchemaSnapshot {
        final List<SchemaObject> tables = new ArrayList<>();
        final List<SchemaObject> other = new ArrayList<>();
    }

    private static void upgradeToV2(SQLiteDatabase db) {
        addColumn(db, "tasks", "category", "ALTER TABLE tasks ADD COLUMN category TEXT NOT NULL DEFAULT ''");
        addColumn(db, "tasks", "pinned", "ALTER TABLE tasks ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0");
        addColumn(db, "tasks", "completed_at", "ALTER TABLE tasks ADD COLUMN completed_at TEXT NULL");
        addColumn(db, "notes", "pinned", "ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0");
        addColumn(db, "notes", "title", "ALTER TABLE notes ADD COLUMN title TEXT NOT NULL DEFAULT ''");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_category ON tasks(category)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_completed_at ON tasks(completed_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS schema_meta (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)");
        db.execSQL("INSERT OR REPLACE INTO schema_meta(key,value) VALUES('schema_version','2')");
    }

    private static void addColumn(SQLiteDatabase db, String table, String column, String sql) {
        Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        boolean exists = false;
        while (c.moveToNext()) {
            if (column.equalsIgnoreCase(c.getString(c.getColumnIndexOrThrow("name")))) { exists = true; break; }
        }
        c.close();
        if (!exists) db.execSQL(sql);
    }

    // ---------- Tasks ----------

    private Task readTask(Cursor c) {
        Task t = new Task();
        t.id = c.getString(c.getColumnIndexOrThrow("id"));
        t.title = c.getString(c.getColumnIndexOrThrow("title"));
        t.description = c.getString(c.getColumnIndexOrThrow("description"));
        int ci = c.getColumnIndex("category");
        t.category = ci >= 0 ? c.getString(ci) : "";
        int pi = c.getColumnIndex("pinned");
        t.pinned = pi >= 0 && c.getInt(pi) != 0;
        int coi = c.getColumnIndex("completed_at");
        t.completedAt = coi >= 0 && !c.isNull(coi) ? c.getString(coi) : null;
        t.priority = c.getInt(c.getColumnIndexOrThrow("priority"));
        int dueIdx = c.getColumnIndexOrThrow("due_date");
        t.dueDate = c.isNull(dueIdx) ? null : c.getString(dueIdx);
        t.completed = c.getInt(c.getColumnIndexOrThrow("is_completed")) != 0;
        t.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        t.updatedAt = c.getString(c.getColumnIndexOrThrow("updated_at"));
        return t;
    }

    private ContentValues taskValues(Task t) {
        ContentValues v = new ContentValues();
        v.put("id", t.id);
        v.put("title", t.title);
        v.put("description", t.description == null ? "" : t.description);
        v.put("category", t.category == null ? "" : t.category);
        v.put("pinned", t.pinned ? 1 : 0);
        v.put("priority", t.priority);
        if (t.dueDate == null) v.putNull("due_date"); else v.put("due_date", t.dueDate);
        v.put("is_completed", t.completed ? 1 : 0);
        if (t.completed) {
            if (t.completedAt == null || t.completedAt.isEmpty()) t.completedAt = Task.nowIso();
            v.put("completed_at", t.completedAt);
        } else {
            t.completedAt = null;
            v.putNull("completed_at");
        }
        v.put("created_at", t.createdAt);
        v.put("updated_at", t.updatedAt);
        return v;
    }

    public List<Task> allTasks() {
        List<Task> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tasks ORDER BY pinned DESC, is_completed ASC, priority DESC, " +
                "CASE WHEN due_date IS NULL THEN 1 ELSE 0 END ASC, due_date ASC, created_at DESC", null);
        while (c.moveToNext()) list.add(readTask(c));
        c.close();
        return list;
    }

    public List<Task> tasksForDate(String isoDate) {
        List<Task> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM tasks WHERE due_date = ? ORDER BY is_completed ASC, priority DESC, created_at DESC",
                new String[]{isoDate});
        while (c.moveToNext()) list.add(readTask(c));
        c.close();
        return list;
    }

    public Task taskById(String id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM tasks WHERE id = ?", new String[]{id});
        Task t = c.moveToNext() ? readTask(c) : null;
        c.close();
        return t;
    }

    public void saveTask(Task t) {
        t.updatedAt = Task.nowIso();
        SQLiteDatabase db = getWritableDatabase();
        if (taskById(t.id) == null) db.insert("tasks", null, taskValues(t));
        else db.update("tasks", taskValues(t), "id = ?", new String[]{t.id});
    }

    public void setCompleted(String id, boolean completed) {
        ContentValues v = new ContentValues();
        v.put("is_completed", completed ? 1 : 0);
        if (completed) v.put("completed_at", Task.nowIso()); else v.putNull("completed_at");
        v.put("updated_at", Task.nowIso());
        getWritableDatabase().update("tasks", v, "id = ?", new String[]{id});
    }

    public void deleteTask(String id) {
        getWritableDatabase().delete("tasks", "id = ?", new String[]{id});
    }

    public void restoreTask(Task t) {
        getWritableDatabase().insertWithOnConflict("tasks", null, taskValues(t), SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void clearTasksAndNotes() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("tasks", null, null);
        db.delete("notes", null, null);
    }

    public List<String> categories() {
        List<String> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT category FROM tasks WHERE category != '' ORDER BY category", null);
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        return list;
    }

    /** ISO date -> completion count, for accurate weekly/streak statistics. */
    public Map<String, Integer> completionsSince(String isoDate) {
        Map<String, Integer> map = new HashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT substr(completed_at,1,10) d, COUNT(*) FROM tasks " +
                "WHERE completed_at IS NOT NULL AND completed_at >= ? GROUP BY d", new String[]{isoDate});
        while (c.moveToNext()) map.put(c.getString(0), c.getInt(1));
        c.close();
        return map;
    }

    // ---------- Notes ----------

    public String noteFor(String isoDate) {
        Cursor c = getReadableDatabase().rawQuery("SELECT content FROM notes WHERE note_date = ?", new String[]{isoDate});
        String s = c.moveToNext() ? c.getString(0) : "";
        c.close();
        return s;
    }

    public void saveNote(String isoDate, String content) {
        String text = content == null ? "" : content;
        String first = text.split("\n")[0].trim();
        ContentValues v = new ContentValues();
        v.put("content", text);
        v.put("title", first.length() > 80 ? first.substring(0, 80) : first);
        v.put("updated_at", Task.nowIso());

        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM notes WHERE note_date = ?", new String[]{isoDate});
        boolean exists = c.moveToNext();
        c.close();
        if (exists) {
            // Update in place: CONFLICT_REPLACE would delete and reinsert the row,
            // dropping the pin flag that is not part of this ContentValues.
            db.update("notes", v, "note_date = ?", new String[]{isoDate});
        } else {
            v.put("note_date", isoDate);
            db.insert("notes", null, v);
        }
    }

    public void setNotePinned(String isoDate, boolean pinned) {
        ContentValues v = new ContentValues();
        v.put("pinned", pinned ? 1 : 0);
        getWritableDatabase().update("notes", v, "note_date = ?", new String[]{isoDate});
    }

    public boolean isNotePinned(String isoDate) {
        Cursor c = getReadableDatabase().rawQuery("SELECT pinned FROM notes WHERE note_date = ?", new String[]{isoDate});
        boolean pinned = c.moveToNext() && c.getInt(0) != 0;
        c.close();
        return pinned;
    }

    public static class NoteInfo {
        public String date, title, content;
        public boolean pinned;
    }

    public List<NoteInfo> searchNotes(String query) {
        List<NoteInfo> list = new ArrayList<>();
        String q = query == null ? "" : query.trim();
        String like = "%" + q + "%";
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT note_date,title,content,pinned FROM notes WHERE ?='' OR content LIKE ? OR title LIKE ? " +
                "ORDER BY pinned DESC, note_date DESC LIMIT 60", new String[]{q, like, like});
        while (c.moveToNext()) {
            NoteInfo n = new NoteInfo();
            n.date = c.getString(0); n.title = c.getString(1); n.content = c.getString(2);
            n.pinned = c.getInt(3) != 0;
            list.add(n);
        }
        c.close();
        return list;
    }

    /** Dates with non-empty notes, newest first. */
    public List<String> noteDates() {
        List<String> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT note_date FROM notes WHERE content != '' ORDER BY note_date DESC", null);
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        return list;
    }

    // ---------- Settings & backup ----------

    public String setting(String key, String fallback) {
        Cursor c = getReadableDatabase().rawQuery("SELECT value FROM settings WHERE key = ?", new String[]{key});
        String s = c.moveToNext() ? c.getString(0) : fallback;
        c.close();
        return s;
    }

    public void setSetting(String key, String value) {
        ContentValues v = new ContentValues();
        v.put("key", key);
        v.put("value", value);
        getWritableDatabase().insertWithOnConflict("settings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Copies the local database to app-private storage (no permission required). */
    public String createBackupCopy() {
        File source = context.getDatabasePath("daymark.db");
        File dir = new File(context.getExternalFilesDir(null), "backups");
        if (!dir.exists() && !dir.mkdirs()) return null;
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File target = new File(dir, "daymark-backup-" + stamp + ".db");
        try {
            FileInputStream in = new FileInputStream(source);
            FileOutputStream out = new FileOutputStream(target);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush(); out.close(); in.close();
            return target.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public String dbPath() {
        return context.getDatabasePath("daymark.db").getAbsolutePath();
    }

    public int taskCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tasks", null);
        int n = c.moveToNext() ? c.getInt(0) : 0;
        c.close();
        return n;
    }

    public int noteCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM notes WHERE content != ''", null);
        int n = c.moveToNext() ? c.getInt(0) : 0;
        c.close();
        return n;
    }

    public int schemaVersion() {
        try {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT value FROM schema_meta WHERE key = 'schema_version'", null);
            int v = c.moveToNext() ? Integer.parseInt(c.getString(0)) : 0;
            c.close();
            return v;
        } catch (Exception e) {
            return 0;
        }
    }

    public long dbSizeBytes() {
        File f = context.getDatabasePath("daymark.db");
        return f.exists() ? f.length() : 0;
    }
}
