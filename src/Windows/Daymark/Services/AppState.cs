using Daymark.Models;

namespace Daymark.Services;

public sealed class AppState
{
    public static AppState Instance { get; } = new AppState();

    public DatabaseService Database { get; }
    public List<TaskItem> Tasks { get; private set; } = new();
    public string Theme { get; private set; }

    public event EventHandler Changed;
    public event EventHandler ThemeChanged;

    private AppState()
    {
        Database = new DatabaseService();
        Theme = Database.GetSetting("theme", "system");
        Refresh();
    }

    public void Refresh()
    {
        Tasks = Database.GetAllTasks();
        Changed?.Invoke(this, EventArgs.Empty);
    }

    public void Add(TaskItem task)
    {
        if (string.IsNullOrWhiteSpace(task.Title)) return;
        task.Title = task.Title.Trim();
        Database.SaveTask(task);
        Refresh();
    }

    public void Update(TaskItem task)
    {
        Database.SaveTask(task);
        Refresh();
    }

    public void ToggleComplete(TaskItem task)
    {
        task.IsCompleted = !task.IsCompleted;
        Database.SaveTask(task);
        Refresh();
    }

    public TaskItem Delete(string id)
    {
        var task = Tasks.FirstOrDefault(t => t.Id == id)?.Clone();
        if (task == null) return null;
        Database.DeleteTask(id);
        Refresh();
        return task;
    }

    public void Restore(TaskItem task)
    {
        if (task != null) { Database.SaveTask(task); Refresh(); }
    }

    public void SetTheme(string theme)
    {
        Theme = theme;
        Database.SetSetting("theme", theme);
        ThemeChanged?.Invoke(this, EventArgs.Empty);
    }

    public List<TaskItem> TodayTasks()
    {
        var today = DateTime.Today;
        return Tasks.Where(t => t.DueDate?.Date == today || (!t.DueDate.HasValue && !t.IsCompleted)).ToList();
    }

    public List<TaskItem> PriorityTasks() => Tasks
        .Where(t => !t.IsCompleted && (t.Priority == PriorityLevel.High || t.Pinned))
        .OrderByDescending(t => t.Pinned).ThenBy(t => t.DueDate ?? DateTime.MaxValue).ToList();

    public List<TaskItem> OverdueTasks() => Tasks
        .Where(t => t.IsOverdue).OrderBy(t => t.DueDate).ToList();

    public List<TaskItem> UpcomingTasks(int days = 7)
    {
        var today = DateTime.Today;
        return Tasks.Where(t => !t.IsCompleted && t.DueDate.HasValue
                                && t.DueDate.Value.Date > today
                                && t.DueDate.Value.Date <= today.AddDays(days))
                    .OrderBy(t => t.DueDate).ToList();
    }

    public (int total, int done, int remaining, int percent) Progress()
    {
        var list = TodayTasks();
        var total = list.Count;
        var done = list.Count(t => t.IsCompleted);
        return (total, done, total - done, total == 0 ? 0 : (int)Math.Round(done * 100.0 / total));
    }

    /// <summary>
    /// Completion counts for the last seven days. V1 completions had no timestamp,
    /// so only completions actually recorded by V2 are represented (never invented).
    /// </summary>
    public List<(DateTime Date, int Count)> WeeklyCompletions()
    {
        var since = DateTime.Today.AddDays(-6);
        var map = Database.GetCompletionsSince(since);
        return Enumerable.Range(0, 7).Select(i => (Date: since.AddDays(i), Count: map.GetValueOrDefault(since.AddDays(i)))).ToList();
    }

    public int StreakDays()
    {
        var counts = Database.GetCompletionsSince(DateTime.Today.AddDays(-365));
        int streak = 0;
        var cursor = DateTime.Today;
        if (!counts.ContainsKey(cursor)) cursor = cursor.AddDays(-1);
        while (counts.ContainsKey(cursor))
        {
            streak++;
            cursor = cursor.AddDays(-1);
        }
        return streak;
    }

    public (int completedThisWeek, int active, int overdue, int high, int streak) Summary()
    {
        var week = WeeklyCompletions();
        return (week.Sum(x => x.Count), Tasks.Count(t => !t.IsCompleted), OverdueTasks().Count,
                Tasks.Count(t => !t.IsCompleted && t.Priority == PriorityLevel.High), StreakDays());
    }
}
