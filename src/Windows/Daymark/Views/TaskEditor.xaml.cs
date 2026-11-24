using System.Windows;
using System.Windows.Input;
using Daymark.Models;
using Daymark.Services;

namespace Daymark.Views;

public partial class TaskEditor : Window
{
    public TaskItem Result { get; private set; }

    public TaskEditor(TaskItem task = null, DateTime? defaultDate = null)
    {
        InitializeComponent();
        Result = task?.Clone() ?? new TaskItem();
        TitleBox.Text = Result.Title;
        DescriptionBox.Text = Result.Description;
        CategoryBox.Items.Add(string.Empty);
        foreach (var category in AppState.Instance.Database.GetCategories()) CategoryBox.Items.Add(category);
        CategoryBox.Text = Result.Category ?? string.Empty;
        PinBox.IsChecked = Result.Pinned;
        PriorityBox.SelectedIndex = (int)Result.Priority;
        var due = task?.DueDate ?? defaultDate;
        if (due.HasValue)
        {
            HasDue.IsChecked = true;
            DueBox.IsEnabled = true;
            DueBox.SelectedDate = due;
        }
        else DueBox.SelectedDate = DateTime.Today;
        HasDue.Click += (_, _) => DueBox.IsEnabled = HasDue.IsChecked == true;
        Loaded += (_, _) => { TitleBox.Focus(); TitleBox.CaretIndex = TitleBox.Text.Length; };
    }

    private void Save_Click(object sender, RoutedEventArgs e)
    {
        if (string.IsNullOrWhiteSpace(TitleBox.Text))
        {
            MessageBox.Show("Please enter a task title.", "Daymark", MessageBoxButton.OK, MessageBoxImage.Information);
            TitleBox.Focus();
            return;
        }
        Result.Title = TitleBox.Text.Trim();
        Result.Description = DescriptionBox.Text.Trim();
        Result.Category = (CategoryBox.Text ?? string.Empty).Trim();
        Result.Pinned = PinBox.IsChecked == true;
        Result.Priority = (PriorityLevel)PriorityBox.SelectedIndex;
        Result.DueDate = HasDue.IsChecked == true ? DueBox.SelectedDate?.Date : null;
        DialogResult = true;
        Close();
    }

    private void Cancel_Click(object sender, RoutedEventArgs e) { DialogResult = false; Close(); }
    private void TitleBox_KeyDown(object sender, KeyEventArgs e)
    {
        if (e.Key == Key.Enter && Keyboard.Modifiers == ModifierKeys.Control) Save_Click(sender, null);
    }
}
