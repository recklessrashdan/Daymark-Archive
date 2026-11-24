using System.Globalization;
using System.IO;
using Daymark.Models;
using Microsoft.Data.Sqlite;

namespace Daymark.Services;

/// <summary>
/// Local-only SQLite store. V2 keeps the V1 schema intact and applies additive,
/// idempotent migrations in a transaction after taking a timestamped file backup.
/// </summary>
public sealed class DatabaseService
{
    /// <summary>Raised when the verified pre-upgrade backup cannot be produced.</summary>
    public sealed class MigrationSafetyException : Exception
    {
        public string Reason { get; }
        public Exception Cause { get; }

        public MigrationSafetyException(string reason, string message, Exception cause = null) : base(message)
        {
            Reason = reason;
            Cause = cause;
        }
    }

    public const int SchemaVersion = 2;
    private readonly string _connectionString;
    public string DatabasePath { get; }
    public string LastBackupPath { get; private set; }

    public DatabaseService()
    {
        var dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Daymark");
        Directory.CreateDirectory(dir);
        DatabasePath = Path.Combine(dir, "daymark.db");
        _connectionString = new SqliteConnectionStringBuilder { DataSource = DatabasePath, Mode = SqliteOpenMode.ReadWriteCreate }.ToString();
        Initialize();
    }

    private SqliteConnection Open()
    {
        var c = new SqliteConnection(_connectionString);
        c.Open();
        return c;
    }

    private void Initialize()
    {
        // A backup is taken before any V2 schema change. If this fails we refuse
        // to touch the user's database rather than risk an unrecoverable upgrade.
        using (var probe = Open())
        {
            int version = ReadVersion(probe);
            // A genuinely fresh database has no Daymark schema objects and no user
            // data to preserve. Only legacy or partially migrated databases need
            // the verified backup gate before schema changes.
            if (version < SchemaVersion && HasAnyDaymarkSchema(probe))
            {
                string backupDir = Path.Combine(Path.GetDirectoryName(DatabasePath), "backups");
                string finalPath = UniquePath(backupDir,
                    $"daymark-pre-v2-{DateTime.Now:yyyyMMdd-HHmmss}.db");
                string tempPath = finalPath + ".tmp";

                try
                {
                    Directory.CreateDirectory(backupDir);
                    // VACUUM INTO reads through SQLite, so journal/WAL state is included.
                    using (var backup = probe.CreateCommand())
                    {
                        backup.CommandText = $"VACUUM INTO '{EscapeLiteral(tempPath)}'";
                        backup.ExecuteNonQuery();
                    }
                    VerifyBackup(probe, tempPath);
                    File.Move(tempPath, finalPath);
                    VerifyBackup(probe, finalPath);
                    LastBackupPath = finalPath;
                }
                catch (Exception ex)
                {
                    if (File.Exists(tempPath))
                    {
                        try { File.Delete(tempPath); } catch { /* keep original untouched */ }
                    }
                    throw new MigrationSafetyException(
                        "backup_unverified",
                        "Upgrade stopped: a verified local backup could not be created. " +
                        "Your existing data has not been intentionally changed. " +
                        "Check available storage and try again.", ex);
                }
            }
        }

        using var c = Open();
        using var tx = c.BeginTransaction();
        Exec(c, tx, @"
CREATE TABLE IF NOT EXISTS tasks (
 id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL DEFAULT '',
 priority INTEGER NOT NULL DEFAULT 1, due_date TEXT NULL, is_completed INTEGER NOT NULL DEFAULT 0,
 created_at TEXT NOT NULL, updated_at TEXT NOT NULL);
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);
CREATE INDEX IF NOT EXISTS idx_tasks_completed ON tasks(is_completed);
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority);
CREATE TABLE IF NOT EXISTS notes (note_date TEXT PRIMARY KEY NOT NULL, content TEXT NOT NULL DEFAULT '', updated_at TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS schema_meta (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL);");

        // Additive only: existing rows keep every V1 value and receive safe defaults.
        Exec(c, tx, "ALTER TABLE tasks ADD COLUMN category TEXT NOT NULL DEFAULT ''", "category", "tasks");
        Exec(c, tx, "ALTER TABLE tasks ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0", "pinned", "tasks");
        Exec(c, tx, "ALTER TABLE tasks ADD COLUMN completed_at TEXT NULL", "completed_at", "tasks");
        Exec(c, tx, "ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0", "pinned", "notes");
        Exec(c, tx, "ALTER TABLE notes ADD COLUMN title TEXT NOT NULL DEFAULT ''", "title", "notes");
        Exec(c, tx, "CREATE INDEX IF NOT EXISTS idx_tasks_category ON tasks(category)");
        Exec(c, tx, "CREATE INDEX IF NOT EXISTS idx_tasks_completed_at ON tasks(completed_at)");

        Exec(c, tx, "INSERT OR REPLACE INTO schema_meta(key,value) VALUES('schema_version','2')");
        Exec(c, tx, "PRAGMA user_version = 2");
        tx.Commit();
    }

    private static void Exec(SqliteConnection c, SqliteTransaction tx, string sql, string column = null, string table = null)
    {
        if (column != null && HasColumn(c, tx, table, column)) return;
        using var cmd = c.CreateCommand();
        cmd.Transaction = tx;
        cmd.CommandText = sql;
        cmd.ExecuteNonQuery();
    }

    private static bool HasColumn(SqliteConnection c, SqliteTransaction tx, string table, string column)
    {
        using var cmd = c.CreateCommand();
        cmd.Transaction = tx;
        cmd.CommandText = $"PRAGMA table_info({table})";
        using var r = cmd.ExecuteReader();
        while (r.Read())
            if (string.Equals(r.GetString(1), column, StringComparison.OrdinalIgnoreCase)) return true;
        return false;
    }

    private static bool HasAnyDaymarkSchema(SqliteConnection c)
    {
        using var cmd = c.CreateCommand();
        cmd.CommandText = @"
SELECT COUNT(*) FROM sqlite_master
WHERE type IN ('table','index')
  AND name IN ('tasks','notes','settings','schema_meta',
               'idx_tasks_due_date','idx_tasks_completed','idx_tasks_priority',
               'idx_tasks_category','idx_tasks_completed_at')";
        return Convert.ToInt64(cmd.ExecuteScalar()) > 0;
    }

    private static int ReadVersion(SqliteConnection c)
    {
        int pragma = 0;
        using (var cmd = c.CreateCommand())
        {
            cmd.CommandText = "PRAGMA user_version";
            pragma = Convert.ToInt32(cmd.ExecuteScalar());
        }
        int meta = 0;
        try
        {
            using var cmd = c.CreateCommand();
            cmd.CommandText = "SELECT value FROM schema_meta WHERE key='schema_version'";
            var v = cmd.ExecuteScalar() as string;
            if (!string.IsNullOrEmpty(v)) int.TryParse(v, out meta);
        }
        catch { /* schema_meta does not exist in V1 yet */ }
        return Math.Max(pragma, meta);
    }

    private static string EscapeLiteral(string value) => value.Replace("'", "''");

    private static string UniquePath(string directory, string fileName)
    {
        string candidate = Path.Combine(directory, fileName);
        string stem = Path.GetFileNameWithoutExtension(fileName);
        string extension = Path.GetExtension(fileName);
        int suffix = 2;
        while (File.Exists(candidate))
        {
            candidate = Path.Combine(directory, $"{stem}-{suffix}{extension}");
            suffix++;
        }
        return candidate;
    }

    private static void VerifyBackup(SqliteConnection source, string backupPath)
    {
        var builder = new SqliteConnectionStringBuilder
        {
            DataSource = backupPath,
            Mode = SqliteOpenMode.ReadOnly,
            // Do not keep a pooled handle on the backup file: the caller moves it into
            // place immediately after verification and Windows locks open files.
            Pooling = false
        };
        using var check = new SqliteConnection(builder.ToString());
        check.Open();

        using (var integrity = check.CreateCommand())
        {
            integrity.CommandText = "PRAGMA integrity_check";
            var result = Convert.ToString(integrity.ExecuteScalar());
            if (!string.Equals(result, "ok", StringComparison.OrdinalIgnoreCase))
                throw new InvalidDataException("Backup integrity check failed.");
        }

        foreach (var table in new[] { "tasks", "notes", "settings" })
        {
            if (!TableExists(check, table) || CountRows(source, table) != CountRows(check, table))
                throw new InvalidDataException($"Backup verification failed for '{table}'.");
        }
    }

    private static bool TableExists(SqliteConnection connection, string table)
    {
        using var cmd = connection.CreateCommand();
        cmd.CommandText = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=$name";
        cmd.Parameters.AddWithValue("$name", table);
        return Convert.ToInt64(cmd.ExecuteScalar()) > 0;
    }

    private static long CountRows(SqliteConnection connection, string table)
    {
        if (!TableExists(connection, table)) return -1;
        using var cmd = connection.CreateCommand();
        cmd.CommandText = $"SELECT COUNT(*) FROM \"{table.Replace("\"", "\"\"")}\"";
        return Convert.ToInt64(cmd.ExecuteScalar());
    }

    public int SchemaVersionNow
    {
        get { using var c = Open(); return ReadVersion(c); }
    }

    private const string SelectColumns =
        "id,title,description,category,pinned,priority,due_date,is_completed,completed_at,created_at,updated_at";

    private static TaskItem ReadTask(SqliteDataReader r)
    {
        string Get(string name) => r.GetString(r.GetOrdinal(name));
        bool IsNull(string name) => r.IsDBNull(r.GetOrdinal(name));
        return new TaskItem
        {
            Id = Get("id"),
            Title = Get("title"),
            Description = Get("description"),
            Category = Get("category"),
            Pinned = r.GetInt32(r.GetOrdinal("pinned")) != 0,
            Priority = (PriorityLevel)r.GetInt32(r.GetOrdinal("priority")),
            DueDate = IsNull("due_date") ? null : ParseDate(Get("due_date")),
            IsCompleted = r.GetInt32(r.GetOrdinal("is_completed")) != 0,
            CompletedAt = IsNull("completed_at") ? null : ParseDateTime(Get("completed_at")),
            CreatedAt = ParseDateTime(Get("created_at")),
            UpdatedAt = ParseDateTime(Get("updated_at"))
        };
    }

    private static DateTime ParseDate(string s) => DateTime.TryParse(s, CultureInfo.InvariantCulture,
        DateTimeStyles.None, out var d) ? d : DateTime.Today;
    private static DateTime ParseDateTime(string s) => DateTime.TryParse(s, CultureInfo.InvariantCulture,
        DateTimeStyles.RoundtripKind, out var d) ? d : DateTime.Now;

    public List<TaskItem> GetAllTasks()
    {
        var list = new List<TaskItem>();
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = $"SELECT {SelectColumns} FROM tasks ORDER BY pinned DESC, is_completed, " +
            "CASE priority WHEN 2 THEN 0 WHEN 1 THEN 1 ELSE 2 END, " +
            "CASE WHEN due_date IS NULL THEN 1 ELSE 0 END, due_date, created_at DESC";
        using var r = cmd.ExecuteReader();
        while (r.Read()) list.Add(ReadTask(r));
        return list;
    }

    public List<TaskItem> GetTasksForDate(DateTime date)
    {
        var list = new List<TaskItem>();
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = $"SELECT {SelectColumns} FROM tasks WHERE due_date=@d ORDER BY is_completed, priority DESC, created_at DESC";
        cmd.Parameters.AddWithValue("@d", date.ToString("yyyy-MM-dd"));
        using var r = cmd.ExecuteReader();
        while (r.Read()) list.Add(ReadTask(r));
        return list;
    }

    public void SaveTask(TaskItem t)
    {
        t.UpdatedAt = DateTime.Now;
        using var c = Open();
        using var tx = c.BeginTransaction();
        using (var check = c.CreateCommand())
        {
            check.Transaction = tx;
            check.CommandText = "SELECT completed_at FROM tasks WHERE id=@id";
            check.Parameters.AddWithValue("@id", t.Id);
            var previous = check.ExecuteScalar() as string;
            if (t.IsCompleted)
                t.CompletedAt = t.CompletedAt ?? (string.IsNullOrEmpty(previous) ? ParseDateTime(DateTime.Now.ToString("o")) : ParseDateTime(previous));
            else
                t.CompletedAt = null;
        }
        using var cmd = c.CreateCommand();
        cmd.Transaction = tx;
        cmd.CommandText = @"
INSERT INTO tasks (id,title,description,category,pinned,priority,due_date,is_completed,completed_at,created_at,updated_at)
VALUES (@id,@title,@desc,@cat,@pinned,@prio,@due,@done,@completed,@created,@updated)
ON CONFLICT(id) DO UPDATE SET title=excluded.title, description=excluded.description,
 category=excluded.category, pinned=excluded.pinned, priority=excluded.priority,
 due_date=excluded.due_date, is_completed=excluded.is_completed,
 completed_at=excluded.completed_at, updated_at=excluded.updated_at;";
        cmd.Parameters.AddWithValue("@id", t.Id);
        cmd.Parameters.AddWithValue("@title", t.Title);
        cmd.Parameters.AddWithValue("@desc", t.Description ?? string.Empty);
        cmd.Parameters.AddWithValue("@cat", t.Category ?? string.Empty);
        cmd.Parameters.AddWithValue("@pinned", t.Pinned ? 1 : 0);
        cmd.Parameters.AddWithValue("@prio", (int)t.Priority);
        cmd.Parameters.AddWithValue("@due", t.DueDate.HasValue ? t.DueDate.Value.ToString("yyyy-MM-dd") : (object)DBNull.Value);
        cmd.Parameters.AddWithValue("@done", t.IsCompleted ? 1 : 0);
        cmd.Parameters.AddWithValue("@completed", t.CompletedAt.HasValue ? t.CompletedAt.Value.ToString("o") : (object)DBNull.Value);
        cmd.Parameters.AddWithValue("@created", t.CreatedAt.ToString("o"));
        cmd.Parameters.AddWithValue("@updated", t.UpdatedAt.ToString("o"));
        cmd.ExecuteNonQuery();
        tx.Commit();
    }

    public void DeleteTask(string id)
    {
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = "DELETE FROM tasks WHERE id=@id";
        cmd.Parameters.AddWithValue("@id", id);
        cmd.ExecuteNonQuery();
    }

    public List<string> GetCategories()
    {
        var list = new List<string>();
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = "SELECT DISTINCT category FROM tasks WHERE category <> '' ORDER BY category";
        using var r = cmd.ExecuteReader();
        while (r.Read()) list.Add(r.GetString(0));
        return list;
    }

    public Dictionary<DateTime, int> GetCompletionsSince(DateTime from)
    {
        var map = new Dictionary<DateTime, int>();
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = "SELECT substr(completed_at,1,10) AS d, COUNT(*) FROM tasks " +
                          "WHERE completed_at IS NOT NULL AND completed_at >= @from GROUP BY d";
        cmd.Parameters.AddWithValue("@from", from.ToString("yyyy-MM-dd"));
        using var r = cmd.ExecuteReader();
        while (r.Read())
        {
            if (DateTime.TryParse(r.GetString(0), out var d)) map[d.Date] = r.GetInt32(1);
        }
        return map;
    }

    public string GetNote(DateTime date)
    {
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = "SELECT content FROM notes WHERE note_date=@d";
        cmd.Parameters.AddWithValue("@d", date.ToString("yyyy-MM-dd"));
        return cmd.ExecuteScalar() as string ?? string.Empty;
    }

    public void SaveNote(DateTime date, string content)
    {
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = @"INSERT INTO notes(note_date,content,title,updated_at,pinned) VALUES(@d,@c,@t,@u,0)
ON CONFLICT(note_date) DO UPDATE SET content=excluded.content, title=excluded.title, updated_at=excluded.updated_at";
        cmd.Parameters.AddWithValue("@d", date.ToString("yyyy-MM-dd"));
        cmd.Parameters.AddWithValue("@c", content ?? string.Empty);
        var firstLine = (content ?? string.Empty).Split('\n').FirstOrDefault()?.Trim() ?? string.Empty;
        if (firstLine.Length > 80) firstLine = firstLine[..80];
        cmd.Parameters.AddWithValue("@t", firstLine);
        cmd.Parameters.AddWithValue("@u", DateTime.Now.ToString("o"));
        cmd.ExecuteNonQuery();
    }

    public void SetNotePinned(DateTime date, bool pinned)
    {
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = "UPDATE notes SET pinned=@p WHERE note_date=@d";
        cmd.Parameters.AddWithValue("@p", pinned ? 1 : 0);
        cmd.Parameters.AddWithValue("@d", date.ToString("yyyy-MM-dd"));
        cmd.ExecuteNonQuery();
    }

    public bool IsNotePinned(DateTime date)
    {
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = "SELECT pinned FROM notes WHERE note_date=@d";
        cmd.Parameters.AddWithValue("@d", date.ToString("yyyy-MM-dd"));
        return Convert.ToInt32(cmd.ExecuteScalar() ?? 0) != 0;
    }

    public List<(DateTime Date, string Title, string Content, bool Pinned)> SearchNotes(string query)
    {
        var list = new List<(DateTime, string, string, bool)>();
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = @"SELECT note_date,title,content,pinned FROM notes
WHERE (@q='' OR content LIKE '%'||@q||'%' OR title LIKE '%'||@q||'%')
ORDER BY pinned DESC, note_date DESC LIMIT 60";
        cmd.Parameters.AddWithValue("@q", query ?? string.Empty);
        using var r = cmd.ExecuteReader();
        while (r.Read())
        {
            DateTime.TryParse(r.GetString(0), out var d);
            list.Add((d, r.GetString(1), r.GetString(2), r.GetInt32(3) != 0));
        }
        return list;
    }

    public string GetSetting(string key, string fallback = "")
    {
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = "SELECT value FROM settings WHERE key=@k";
        cmd.Parameters.AddWithValue("@k", key);
        return cmd.ExecuteScalar() as string ?? fallback;
    }

    public void SetSetting(string key, string value)
    {
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = @"INSERT INTO settings(key,value) VALUES(@k,@v) ON CONFLICT(key) DO UPDATE SET value=excluded.value";
        cmd.Parameters.AddWithValue("@k", key);
        cmd.Parameters.AddWithValue("@v", value);
        cmd.ExecuteNonQuery();
    }

    public int ClearAllUserData()
    {
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = "DELETE FROM tasks; DELETE FROM notes;";
        return cmd.ExecuteNonQuery();
    }

    public string BackupTo(string destinationPath)
    {
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = "PRAGMA wal_checkpoint(TRUNCATE)";
        try { cmd.ExecuteNonQuery(); } catch { /* journal mode may not be WAL */ }
        File.Copy(DatabasePath, destinationPath, true);
        return destinationPath;
    }

    public long DatabaseSizeBytes => new FileInfo(DatabasePath).Length;

    public int CountTasks() => Count("SELECT COUNT(*) FROM tasks");
    private int Count(string sql)
    {
        using var c = Open();
        using var cmd = c.CreateCommand();
        cmd.CommandText = sql;
        return Convert.ToInt32(cmd.ExecuteScalar());
    }
}
