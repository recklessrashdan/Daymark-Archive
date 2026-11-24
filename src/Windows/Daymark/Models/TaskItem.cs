using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace Daymark.Models;

public enum PriorityLevel
{
    Low = 0,
    Medium = 1,
    High = 2
}

public sealed class TaskItem : INotifyPropertyChanged
{
    private string _title;
    private string _description;
    private string _category;
    private PriorityLevel _priority;
    private DateTime? _dueDate;
    private DateTime? _completedAt;
    private bool _isCompleted;
    private bool _pinned;

    public string Id { get; set; } = Guid.NewGuid().ToString("N");
    public DateTime CreatedAt { get; set; } = DateTime.Now;
    public DateTime UpdatedAt { get; set; } = DateTime.Now;

    public string Title
    {
        get => _title;
        set { if (_title != value) { _title = value; OnPropertyChanged(); } }
    }

    public string Description
    {
        get => _description;
        set { if (_description != value) { _description = value; OnPropertyChanged(); } }
    }

    /// <summary>Free-text label/category, added by the V2 migration.</summary>
    public string Category
    {
        get => _category;
        set { if (_category != value) { _category = value; OnPropertyChanged(); OnPropertyChanged(nameof(HasCategory)); } }
    }

    public bool Pinned
    {
        get => _pinned;
        set { if (_pinned != value) { _pinned = value; OnPropertyChanged(); } }
    }

    public PriorityLevel Priority
    {
        get => _priority;
        set { if (_priority != value) { _priority = value; OnPropertyChanged(); OnPropertyChanged(nameof(PriorityLabel)); } }
    }

    public DateTime? DueDate
    {
        get => _dueDate;
        set { if (_dueDate != value) { _dueDate = value; OnPropertyChanged(); OnPropertyChanged(nameof(DueDateText)); OnPropertyChanged(nameof(IsOverdue)); } }
    }

    public DateTime? CompletedAt
    {
        get => _completedAt;
        set { if (_completedAt != value) { _completedAt = value; OnPropertyChanged(); } }
    }

    public bool IsCompleted
    {
        get => _isCompleted;
        set { if (_isCompleted != value) { _isCompleted = value; OnPropertyChanged(); OnPropertyChanged(nameof(TitleOpacity)); OnPropertyChanged(nameof(IsOverdue)); } }
    }

    public string PriorityLabel => Priority switch
    {
        PriorityLevel.High => "High",
        PriorityLevel.Low => "Low",
        _ => "Medium"
    };

    public string DueDateText => DueDate.HasValue
        ? (DueDate.Value.Date == DateTime.Today ? "Today" :
           DueDate.Value.Date == DateTime.Today.AddDays(1) ? "Tomorrow" :
           DueDate.Value.Date == DateTime.Today.AddDays(-1) ? "Yesterday" :
           DueDate.Value.ToString("ddd, d MMM"))
        : string.Empty;

    public bool IsOverdue => DueDate.HasValue && !IsCompleted && DueDate.Value.Date < DateTime.Today;

    public bool HasCategory => !string.IsNullOrWhiteSpace(Category);

    public double TitleOpacity => IsCompleted ? 0.5 : 1.0;

    public event PropertyChangedEventHandler PropertyChanged;

    private void OnPropertyChanged([CallerMemberName] string name = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));

    public TaskItem Clone() => new TaskItem
    {
        Id = Id,
        Title = Title,
        Description = Description,
        Category = Category,
        Pinned = Pinned,
        Priority = Priority,
        DueDate = DueDate,
        CompletedAt = CompletedAt,
        IsCompleted = IsCompleted,
        CreatedAt = CreatedAt,
        UpdatedAt = UpdatedAt
    };
}
