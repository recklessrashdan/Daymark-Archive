using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using Daymark.Models;

namespace Daymark.Views;

public partial class TaskRow : UserControl
{
    public event Action<TaskItem> EditRequested;
    public event Action<TaskItem> DeleteRequested;
    public event Action<TaskItem> ToggleRequested;

    private TaskItem _task;
    private bool _suppressToggle;

    public TaskRow()
    {
        InitializeComponent();
        DataContextChanged += (_, _) => Bind(DataContext as TaskItem);
        Unloaded += (_, _) => { EditRequested = null; DeleteRequested = null; ToggleRequested = null; };
    }

    public TaskItem Task
    {
        get => _task;
        set { _task = value; DataContext = value; }
    }

    private void Bind(TaskItem t)
    {
        _task = t;
        if (t == null) return;
        TitleText.Text = (t.Pinned ? "◆  " : string.Empty) + t.Title;
        TitleText.TextDecorations = t.IsCompleted ? TextDecorations.Strikethrough : null;
        TitleText.Opacity = t.IsCompleted ? 0.5 : 1;
        TitleText.Foreground = (Brush)FindResource("Text");
        _suppressToggle = true;
        Check.IsChecked = t.IsCompleted;
        _suppressToggle = false;

        PriorityPill.Visibility = t.Priority == PriorityLevel.High ? Visibility.Visible : Visibility.Collapsed;
        PriorityPillText.Text = t.Priority == PriorityLevel.High ? "HIGH" : string.Empty;
        DuePill.Visibility = t.DueDateText.Length > 0 ? Visibility.Visible : Visibility.Collapsed;
        DuePillText.Text = t.DueDateText;
        if (t.IsOverdue)
        {
            DuePill.Background = (Brush)FindResource("AmberSoft");
            DuePillText.Foreground = (Brush)FindResource("Danger");
        }

        var bits = new List<string>();
        if (!string.IsNullOrEmpty(t.Description)) bits.Add("Has notes");
        if (t.HasCategory) bits.Add(t.Category);
        if (t.IsOverdue) bits.Add("Overdue");
        bits.Add(t.PriorityLabel + " priority");
        MetaText.Text = string.Join("   ·   ", bits);
        MetaText.Foreground = (Brush)FindResource(t.IsOverdue ? "Danger" : "TextMuted");

        PriorityBar.Background = t.Priority switch
        {
            PriorityLevel.High => (Brush)FindResource("PriorityHigh"),
            PriorityLevel.Low => (Brush)FindResource("PriorityLow"),
            _ => (Brush)FindResource("PriorityMedium")
        };
        Card.Opacity = t.IsCompleted ? 0.78 : 1;
    }

    private void Check_Changed(object sender, RoutedEventArgs e)
    {
        if (!_suppressToggle && _task != null) ToggleRequested?.Invoke(_task);
    }

    private void Edit_Click(object sender, RoutedEventArgs e) => EditRequested?.Invoke(_task);
    private void Delete_Click(object sender, RoutedEventArgs e) => DeleteRequested?.Invoke(_task);
}
