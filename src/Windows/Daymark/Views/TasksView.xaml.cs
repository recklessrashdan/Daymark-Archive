using System.Windows;
using System.Windows.Controls;
using Daymark.Models;
using Daymark.Services;

namespace Daymark.Views;

public partial class TasksView : UserControl, IRefreshable
{
    private string _filter = "all";
    private string _category = "All categories";
    private int _sort;
    private bool _populating;

    public TasksView()
    {
        InitializeComponent();
        AppState.Instance.Changed += (_, _) => Dispatcher.Invoke(Refresh);
        Loaded += (_, _) => Refresh();
    }

    public void ApplyFilter(string filter)
    {
        var radio = FindRadio(filter);
        if (radio != null) radio.IsChecked = true;
        else { _filter = filter; Refresh(); }
    }

    private RadioButton FindRadio(string tag) => this.FindVisualChildren<RadioButton>()
        .FirstOrDefault(r => r.Tag?.ToString() == tag);

    public void Refresh()
    {
        if (TaskList == null || GroupLabel == null) return;
        var all = AppState.Instance.Tasks;
        CountLine.Text = $"{all.Count(t => !t.IsCompleted)} active  ·  {all.Count(t => t.IsCompleted)} completed  ·  {all.Count(t => t.IsOverdue)} overdue";

        PopulateCategories();

        var query = SearchBox?.Text?.Trim() ?? string.Empty;
        IEnumerable<TaskItem> items = all;
        if (!string.IsNullOrEmpty(query))
            items = items.Where(t => t.Title.Contains(query, StringComparison.OrdinalIgnoreCase)
                                  || (t.Description ?? string.Empty).Contains(query, StringComparison.OrdinalIgnoreCase)
                                  || (t.Category ?? string.Empty).Contains(query, StringComparison.OrdinalIgnoreCase));
        if (_category != "All categories") items = items.Where(t => t.Category == _category);

        var today = DateTime.Today;
        items = _filter switch
        {
            "active" => items.Where(t => !t.IsCompleted),
            "completed" => items.Where(t => t.IsCompleted),
            "high" => items.Where(t => t.Priority == PriorityLevel.High || t.Pinned),
            "overdue" => items.Where(t => t.IsOverdue),
            "upcoming" => items.Where(t => !t.IsCompleted && t.DueDate.HasValue
                                           && t.DueDate.Value.Date > today && t.DueDate.Value.Date <= today.AddDays(7)),
            _ => items
        };

        var list = Sort(items).ToList();
        GroupLabel.Text = _filter switch
        {
            "active" => "Active tasks",
            "completed" => "Completed tasks",
            "high" => "Priority focus",
            "overdue" => "Overdue tasks",
            "upcoming" => "Upcoming deadlines",
            _ => "Everything"
        };
        ResultCount.Text = list.Count + (list.Count == 1 ? " task" : " tasks");

        TaskList.Children.Clear();
        foreach (var t in list) TaskList.Children.Add(TodayView.CreateRow(t));
        EmptyText.Visibility = list.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    }

    private IEnumerable<TaskItem> Sort(IEnumerable<TaskItem> items) => _sort switch
    {
        1 => items.OrderBy(t => t.IsCompleted).ThenBy(t => t.DueDate ?? DateTime.MaxValue),
        2 => items.OrderBy(t => t.IsCompleted).ThenByDescending(t => t.Priority).ThenBy(t => t.DueDate ?? DateTime.MaxValue),
        3 => items.OrderByDescending(t => t.CreatedAt),
        4 => items.OrderBy(t => t.Title, StringComparer.OrdinalIgnoreCase),
        _ => items.OrderByDescending(t => t.Pinned).ThenBy(t => t.IsCompleted)
                  .ThenBy(t => t.DueDate ?? DateTime.MaxValue)
    };

    private void PopulateCategories()
    {
        if (CategoryBox == null || _populating) return;
        _populating = true;
        var categories = AppState.Instance.Database.GetCategories();
        var selected = _category;
        CategoryBox.Items.Clear();
        CategoryBox.Items.Add("All categories");
        foreach (var c in categories) CategoryBox.Items.Add(c);
        CategoryBox.SelectedItem = categories.Contains(selected) ? selected : "All categories";
        _category = CategoryBox.SelectedItem as string ?? "All categories";
        _populating = false;
    }

    private void SearchBox_TextChanged(object sender, TextChangedEventArgs e) => Refresh();

    private void Filter_Checked(object sender, RoutedEventArgs e)
    {
        if (sender is RadioButton rb) { _filter = rb.Tag?.ToString() ?? "all"; Refresh(); }
    }

    private void CategoryBox_Changed(object sender, SelectionChangedEventArgs e)
    {
        if (_populating) return;
        _category = CategoryBox.SelectedItem as string ?? "All categories";
        Refresh();
    }

    private void SortBox_Changed(object sender, SelectionChangedEventArgs e)
    {
        _sort = SortBox.SelectedIndex;
        Refresh();
    }

    private void NewTask_Click(object sender, RoutedEventArgs e)
    {
        var task = MainWindow.Current.OpenEditor();
        if (task != null) { AppState.Instance.Add(task); MainWindow.Current.ShowToast("Task created"); }
    }
}

internal static class VisualTreeExtensions
{
    public static IEnumerable<T> FindVisualChildren<T>(this DependencyObject root) where T : DependencyObject
    {
        if (root == null) yield break;
        int count = System.Windows.Media.VisualTreeHelper.GetChildrenCount(root);
        for (int i = 0; i < count; i++)
        {
            var child = System.Windows.Media.VisualTreeHelper.GetChild(root, i);
            if (child is T typed) yield return typed;
            foreach (var item in FindVisualChildren<T>(child)) yield return item;
        }
    }
}
