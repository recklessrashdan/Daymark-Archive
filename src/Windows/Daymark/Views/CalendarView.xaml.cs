using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Shapes;
using Daymark.Models;
using Daymark.Services;

namespace Daymark.Views;

public partial class CalendarView : UserControl, IRefreshable
{
    private DateTime _shownMonth = new(DateTime.Today.Year, DateTime.Today.Month, 1);
    private DateTime _selected = DateTime.Today;
    private DateTime _weekStart = DateTime.Today.AddDays(-((int)DateTime.Today.DayOfWeek + 6) % 7);
    private bool _weekMode;

    public CalendarView()
    {
        InitializeComponent();
        AppState.Instance.Changed += (_, _) => Dispatcher.Invoke(Refresh);
        Loaded += (_, _) => Refresh();
    }

    public void Refresh()
    {
        MonthLabel.Text = _weekMode
            ? _weekStart.ToString("d MMM") + " – " + _weekStart.AddDays(6).ToString("d MMM yyyy")
            : _shownMonth.ToString("MMMM yyyy");
        CalendarGrid.Visibility = _weekMode ? Visibility.Collapsed : Visibility.Visible;
        WeekPanel.Visibility = _weekMode ? Visibility.Visible : Visibility.Collapsed;

        SelectedDateLabel.Text = _selected.ToString("dddd, d MMMM");
        BuildGrids();

        var tasks = AppState.Instance.Database.GetTasksForDate(_selected);
        DaySummary.Text = $"{tasks.Count} task{(tasks.Count == 1 ? "" : "s")} · {tasks.Count(t => t.IsCompleted)} done";
        DayTaskList.Children.Clear();
        foreach (var t in tasks) DayTaskList.Children.Add(TodayView.CreateRow(t));
        EmptyDay.Visibility = tasks.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    }

    private void BuildGrids()
    {
        if (_weekMode) BuildWeek(); else BuildMonth();
    }

    private void BuildMonth()
    {
        CalendarGrid.Children.Clear();
        CalendarGrid.RowDefinitions.Clear();
        CalendarGrid.ColumnDefinitions.Clear();
        for (int i = 0; i < 6; i++) CalendarGrid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        for (int i = 0; i < 7; i++) CalendarGrid.ColumnDefinitions.Add(new ColumnDefinition());

        int offset = ((int)_shownMonth.DayOfWeek + 6) % 7;
        var start = _shownMonth.AddDays(-offset);
        var byDate = AppState.Instance.Tasks.Where(t => t.DueDate.HasValue)
            .GroupBy(t => t.DueDate.Value.Date).ToDictionary(g => g.Key, g => g.ToList());

        for (int i = 0; i < 42; i++)
        {
            var date = start.AddDays(i);
            bool outside = date.Month != _shownMonth.Month;
            bool isToday = date.Date == DateTime.Today;
            bool isSelected = date.Date == _selected.Date;
            byDate.TryGetValue(date.Date, out var dayTasks);

            var cell = new Border
            {
                Margin = new Thickness(3), Height = 66, CornerRadius = new CornerRadius(12),
                Background = isSelected ? (Brush)FindResource("Primary")
                           : isToday ? (Brush)FindResource("PrimarySoft")
                           : (Brush)FindResource("Surface"),
                BorderBrush = isSelected ? (Brush)FindResource("Primary") : (Brush)FindResource("Border"),
                BorderThickness = isSelected ? new Thickness(0) : new Thickness(1),
                Cursor = Cursors.Hand, Padding = new Thickness(8, 6, 8, 6), Tag = date
            };
            cell.MouseLeftButtonUp += (_, _) => SelectDate(date, outside);

            var sp = new StackPanel();
            sp.Children.Add(new TextBlock
            {
                Text = date.Day.ToString(), FontSize = 13,
                FontWeight = isToday || isSelected ? FontWeights.SemiBold : FontWeights.Normal,
                Foreground = isSelected ? (Brush)FindResource("OnPrimary")
                           : outside ? (Brush)FindResource("TextMuted") : (Brush)FindResource("Text")
            });

            if (dayTasks != null && dayTasks.Count > 0)
            {
                var dots = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(0, 7, 0, 0) };
                foreach (var t in dayTasks.Take(4))
                {
                    dots.Children.Add(new Ellipse
                    {
                        Width = 6, Height = 6, Margin = new Thickness(0, 0, 4, 0),
                        Fill = isSelected ? (Brush)FindResource("OnPrimary")
                             : t.IsOverdue ? (Brush)FindResource("Danger")
                             : t.Priority == PriorityLevel.High ? (Brush)FindResource("PriorityHigh")
                             : (Brush)FindResource("Primary")
                    });
                }
                sp.Children.Add(dots);
            }
            cell.Child = sp;
            Grid.SetRow(cell, i / 7);
            Grid.SetColumn(cell, i % 7);
            CalendarGrid.Children.Add(cell);
        }
    }

    private void BuildWeek()
    {
        WeekPanel.Children.Clear();
        WeekPanel.ColumnDefinitions.Clear();
        for (int i = 0; i < 7; i++) WeekPanel.ColumnDefinitions.Add(new ColumnDefinition());

        var byDate = AppState.Instance.Tasks.Where(t => t.DueDate.HasValue)
            .GroupBy(t => t.DueDate.Value.Date).ToDictionary(g => g.Key, g => g.ToList());

        for (int i = 0; i < 7; i++)
        {
            var date = _weekStart.AddDays(i);
            bool isToday = date.Date == DateTime.Today;
            bool isSelected = date.Date == _selected.Date;
            byDate.TryGetValue(date.Date, out var dayTasks);

            var cell = new Border
            {
                Margin = new Thickness(3), CornerRadius = new CornerRadius(12), Padding = new Thickness(8, 10, 8, 10),
                Background = isSelected ? (Brush)FindResource("Primary") : isToday ? (Brush)FindResource("PrimarySoft") : (Brush)FindResource("Surface"),
                BorderBrush = isSelected ? (Brush)FindResource("Primary") : (Brush)FindResource("Border"),
                BorderThickness = isSelected ? new Thickness(0) : new Thickness(1),
                Cursor = Cursors.Hand, Tag = date
            };
            cell.MouseLeftButtonUp += (_, _) => SelectDate(date, false);

            var sp = new StackPanel();
            sp.Children.Add(new TextBlock { Text = date.ToString("ddd").ToUpperInvariant(), FontSize = 10,
                Foreground = isSelected ? (Brush)FindResource("OnPrimary") : (Brush)FindResource("TextMuted"), HorizontalAlignment = HorizontalAlignment.Center });
            sp.Children.Add(new TextBlock { Text = date.Day.ToString(), FontSize = 22, FontWeight = FontWeights.SemiBold,
                Foreground = isSelected ? (Brush)FindResource("OnPrimary") : (Brush)FindResource("Text"), HorizontalAlignment = HorizontalAlignment.Center, Margin = new Thickness(0, 4, 0, 10) });
            if (dayTasks != null && dayTasks.Count > 0)
            {
                foreach (var t in dayTasks.Take(3))
                    sp.Children.Add(new TextBlock
                    {
                        Text = "• " + t.Title, FontSize = 10, TextWrapping = TextWrapping.Wrap,
                        Foreground = isSelected ? (Brush)FindResource("OnPrimary") : (Brush)FindResource("TextMuted"),
                        Margin = new Thickness(0, 0, 0, 5)
                    });
                if (dayTasks.Count > 3)
                    sp.Children.Add(new TextBlock { Text = "+" + (dayTasks.Count - 3) + " more", FontSize = 10,
                        Foreground = isSelected ? (Brush)FindResource("OnPrimary") : (Brush)FindResource("PrimaryDark") });
            }
            else
            {
                sp.Children.Add(new TextBlock { Text = "—", FontSize = 14, HorizontalAlignment = HorizontalAlignment.Center,
                    Foreground = (Brush)FindResource("TextMuted"), Margin = new Thickness(0, 20, 0, 0) });
            }
            cell.Child = sp;
            Grid.SetColumn(cell, i);
            WeekPanel.Children.Add(cell);
        }
    }

    private void SelectDate(DateTime date, bool outside)
    {
        _selected = date.Date;
        if (outside)
        {
            _shownMonth = new DateTime(date.Year, date.Month, 1);
            _weekStart = date.Date.AddDays(-((int)date.DayOfWeek + 6) % 7);
        }
        Refresh();
    }

    private void ViewMode_Checked(object sender, RoutedEventArgs e)
    {
        bool week = ReferenceEquals(sender, WeekToggle);
        if (week == _weekMode) return;
        _weekMode = week;
        if (week) MonthToggle.IsChecked = false;
        else WeekToggle.IsChecked = false;
        Refresh();
    }

    private void ViewMode_Unchecked(object sender, RoutedEventArgs e)
    {
        if (!ReferenceEquals(sender, WeekToggle) || !_weekMode) return;
        _weekMode = false;
        MonthToggle.IsChecked = true;
        Refresh();
    }

    private void Prev_Click(object sender, RoutedEventArgs e)
    {
        if (_weekMode) _weekStart = _weekStart.AddDays(-7);
        else _shownMonth = _shownMonth.AddMonths(-1);
        Refresh();
    }

    private void Next_Click(object sender, RoutedEventArgs e)
    {
        if (_weekMode) _weekStart = _weekStart.AddDays(7);
        else _shownMonth = _shownMonth.AddMonths(1);
        Refresh();
    }

    private void Today_Click(object sender, RoutedEventArgs e)
    {
        _shownMonth = new DateTime(DateTime.Today.Year, DateTime.Today.Month, 1);
        _weekStart = DateTime.Today.AddDays(-((int)DateTime.Today.DayOfWeek + 6) % 7);
        _selected = DateTime.Today;
        Refresh();
    }

    private void AddForDay_Click(object sender, RoutedEventArgs e)
    {
        var task = MainWindow.Current.OpenEditor(defaultDate: _selected);
        if (task != null) { AppState.Instance.Add(task); MainWindow.Current.ShowToast("Task added"); }
    }
}
