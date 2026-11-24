using System.Windows;
using System.Windows.Controls;
using System.Windows.Threading;
using Daymark.Models;
using Daymark.Services;
using Daymark.Views;

namespace Daymark;

public partial class MainWindow : Window
{
    public static MainWindow Current { get; private set; }

    private readonly Dictionary<string, UserControl> _views = new();
    private readonly DispatcherTimer _toastTimer = new() { Interval = TimeSpan.FromSeconds(5) };
    private Action _undoAction;

    public MainWindow()
    {
        InitializeComponent();
        Current = this;
        _views["today"] = new TodayView();
        _views["tasks"] = new TasksView();
        _views["calendar"] = new CalendarView();
        _views["notes"] = new NotesView();
        _views["settings"] = new SettingsView();
        Nav.SelectedIndex = 0;
        _toastTimer.Tick += (_, _) => HideToast();
    }

    private void Nav_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (Nav.SelectedItem is not ListBoxItem item) return;
        var key = item.Tag?.ToString() ?? "today";
        if (_views.TryGetValue(key, out var view))
        {
            ContentHost.Content = view;
            if (view is IRefreshable r) r.Refresh();
        }
    }

    public void Navigate(string key)
    {
        var item = Nav.Items.OfType<ListBoxItem>().FirstOrDefault(i => i.Tag?.ToString() == key);
        if (item != null) Nav.SelectedItem = item;
    }

    /// <summary>Navigate to the task library with a preselected smart filter.</summary>
    public void NavigateTasks(string filter)
    {
        Navigate("tasks");
        if (_views.TryGetValue("tasks", out var view) && view is TasksView tasks) tasks.ApplyFilter(filter);
    }

    public TaskItem OpenEditor(TaskItem task = null, DateTime? defaultDate = null)
    {
        var editor = new TaskEditor(task, defaultDate) { Owner = this };
        var result = editor.ShowDialog();
        return result == true ? editor.Result : null;
    }

    public void DeleteWithUndo(TaskItem task, Action afterDelete = null)
    {
        if (task == null) return;
        AppState.Instance.Delete(task.Id);
        afterDelete?.Invoke();
        ShowToast("Task deleted", () => { AppState.Instance.Restore(task); afterDelete?.Invoke(); });
    }

    public void ShowToast(string message, Action undo = null)
    {
        ToastText.Text = message;
        _undoAction = undo;
        UndoButton.Visibility = undo == null ? Visibility.Collapsed : Visibility.Visible;
        Toast.Visibility = Visibility.Visible;
        _toastTimer.Stop();
        _toastTimer.Start();
    }

    private void HideToast()
    {
        _toastTimer.Stop();
        Toast.Visibility = Visibility.Collapsed;
        _undoAction = null;
    }

    private void Undo_Click(object sender, RoutedEventArgs e)
    {
        var action = _undoAction;
        HideToast();
        action?.Invoke();
    }
}

public interface IRefreshable { void Refresh(); }
