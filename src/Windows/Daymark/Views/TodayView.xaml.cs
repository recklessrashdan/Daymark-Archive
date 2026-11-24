using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using Daymark.Models;
using Daymark.Services;

namespace Daymark.Views;

public partial class TodayView : UserControl, IRefreshable
{
    public TodayView()
    {
        InitializeComponent();
        AppState.Instance.Changed += (_, _) => Dispatcher.Invoke(Refresh);
        Loaded += (_, _) => Refresh();
    }

    public void Refresh()
    {
        var today = DateTime.Today;
        DateLine.Text = today.ToString("dddd, d MMMM yyyy").ToUpperInvariant();
        int hour = DateTime.Now.Hour;
        Greeting.Text = hour < 12 ? "Good morning." : hour < 17 ? "Good afternoon." : "Good evening.";

        var (total, done, left, pct) = AppState.Instance.Progress();
        TotalText.Text = total.ToString();
        DoneText.Text = done.ToString();
        LeftText.Text = left.ToString();
        PercentText.Text = pct + "%";
        Ring.Percent = pct;
        ProgressNote.Text = total == 0
            ? "Add a task to begin your day."
            : pct == 100 ? "Everything is done. Well earned." :
              pct >= 50 ? "You're more than halfway there." : "Every small step counts.";

        var week = AppState.Instance.WeeklyCompletions();
        WeekTotal.Text = week.Sum(x => x.Count) + " done this week";
        BuildWeekBars(week);
        var summary = AppState.Instance.Summary();
        StreakText.Text = summary.streak == 0 ? "—" : summary.streak + (summary.streak == 1 ? " day" : " days");

        var overdue = AppState.Instance.OverdueTasks();
        var upcoming = AppState.Instance.UpcomingTasks();
        var priority = AppState.Instance.PriorityTasks();
        OverdueCount.Text = overdue.Count.ToString();
        UpcomingCount.Text = upcoming.Count.ToString();
        PriorityCount.Text = priority.Count.ToString();

        RenderTasks(TaskList, AppState.Instance.TodayTasks(), EmptyTasks, 6);
        RenderTasks(PriorityList, priority, EmptyPriority, 5);
        RenderTasks(UpcomingList, upcoming, EmptyUpcoming, 4);

        NoteDate.Text = today.ToString("d MMMM yyyy");
        var note = AppState.Instance.Database.GetNote(today);
        NotePreview.Text = string.IsNullOrWhiteSpace(note) ? "Your thoughts for today will appear here." : note;
        NotePreview.Opacity = string.IsNullOrWhiteSpace(note) ? 0.5 : 1;
    }

    private void BuildWeekBars(List<(DateTime Date, int Count)> week)
    {
        WeekBars.Children.Clear();
        int max = Math.Max(1, week.Max(x => x.Count));
        foreach (var (date, count) in week)
        {
            var col = new StackPanel { Width = 33, VerticalAlignment = VerticalAlignment.Bottom, HorizontalAlignment = HorizontalAlignment.Center };
            var bar = new Border
            {
                Width = 20,
                Height = count == 0 ? 3 : Math.Max(8, 52.0 * count / max),
                CornerRadius = new CornerRadius(5),
                Background = date.Date == DateTime.Today
                    ? (Brush)FindResource("Amber")
                    : (Brush)FindResource(count > 0 ? "Primary" : "Border"),
                HorizontalAlignment = HorizontalAlignment.Center,
                ToolTip = $"{date:ddd}: {count} completed"
            };
            col.Children.Add(bar);
            col.Children.Add(new TextBlock
            {
                Text = date.ToString("ddd").Substring(0, 1),
                FontSize = 10,
                Foreground = (Brush)FindResource(date.Date == DateTime.Today ? "PrimaryDark" : "TextMuted"),
                HorizontalAlignment = HorizontalAlignment.Center,
                Margin = new Thickness(0, 6, 0, 0)
            });
            WeekBars.Children.Add(col);
        }
    }

    private void RenderTasks(StackPanel panel, List<TaskItem> tasks, TextBlock empty, int limit)
    {
        panel.Children.Clear();
        foreach (var t in tasks.Take(limit))
            panel.Children.Add(CreateRow(t));
        empty.Visibility = tasks.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    }

    internal static TaskRow CreateRow(TaskItem t)
    {
        var row = new TaskRow { Task = t };
        row.ToggleRequested += item => AppState.Instance.ToggleComplete(item);
        row.EditRequested += item =>
        {
            var edited = MainWindow.Current.OpenEditor(item);
            if (edited != null) AppState.Instance.Update(edited);
        };
        row.DeleteRequested += item => MainWindow.Current.DeleteWithUndo(item);
        return row;
    }

    private void AddQuick()
    {
        var title = QuickAdd.Text.Trim();
        if (title.Length == 0) return;
        AppState.Instance.Add(new TaskItem { Title = title, DueDate = DateTime.Today, Priority = PriorityLevel.Medium });
        QuickAdd.Text = string.Empty;
        MainWindow.Current.ShowToast("Task added for today");
    }

    private void QuickAdd_Click(object sender, RoutedEventArgs e) => AddQuick();
    private void QuickAdd_KeyDown(object sender, KeyEventArgs e)
    {
        if (e.Key == Key.Enter) { AddQuick(); e.Handled = true; }
    }

    private void NewTask_Click(object sender, RoutedEventArgs e)
    {
        var task = MainWindow.Current.OpenEditor(defaultDate: DateTime.Today);
        if (task != null) { AppState.Instance.Add(task); MainWindow.Current.ShowToast("Task created"); }
    }

    private void ViewAll_Click(object sender, RoutedEventArgs e) => MainWindow.Current.Navigate("tasks");
    private void OpenNotes_Click(object sender, RoutedEventArgs e) => MainWindow.Current.Navigate("notes");
    private void OpenCalendar_Click(object sender, RoutedEventArgs e) => MainWindow.Current.Navigate("calendar");
    private void Overdue_Click(object sender, RoutedEventArgs e) => MainWindow.Current.NavigateTasks("overdue");
    private void Upcoming_Click(object sender, RoutedEventArgs e) => MainWindow.Current.NavigateTasks("upcoming");
    private void Priority_Click(object sender, RoutedEventArgs e) => MainWindow.Current.NavigateTasks("high");
}
