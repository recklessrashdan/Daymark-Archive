using System.Windows;
using System.Windows.Controls;
using System.Windows.Threading;
using Daymark.Services;

namespace Daymark.Views;

public partial class NotesView : UserControl, IRefreshable
{
    private readonly DispatcherTimer _saveTimer = new() { Interval = TimeSpan.FromMilliseconds(500) };
    private bool _loading;
    private bool _dirty;
    private DateTime _displayedDate = DateTime.Today;

    public NotesView()
    {
        InitializeComponent();
        NoteDate.SelectedDate = DateTime.Today;
        _saveTimer.Tick += (_, _) => SaveNow();
        Loaded += (_, _) => Refresh();
        Unloaded += (_, _) => { if (_saveTimer.IsEnabled) SaveNow(); };
    }

    private DateTime SelectedDate => NoteDate.SelectedDate?.Date ?? DateTime.Today;

    public void Refresh()
    {
        _loading = true;
        _dirty = false;
        _displayedDate = SelectedDate;
        NoteBox.Text = AppState.Instance.Database.GetNote(SelectedDate);
        CharCount.Text = NoteBox.Text.Length + (NoteBox.Text.Length == 1 ? " character" : " characters");
        SaveStatus.Text = "Saved";
        bool pinned = AppState.Instance.Database.IsNotePinned(SelectedDate);
        PinButton.IsChecked = pinned;
        PinButton.Content = pinned ? "★ Pinned" : "☆ Pin";
        _loading = false;
        BuildDateList();
    }

    private void BuildDateList()
    {
        NoteDates.Children.Clear();
        var notes = AppState.Instance.Database.SearchNotes(NoteSearch?.Text?.Trim() ?? string.Empty);
        foreach (var note in notes.Take(24))
        {
            var sp = new StackPanel { Margin = new Thickness(0, 0, 0, 2) };
            var b = new Button
            {
                Content = (note.Pinned ? "★ " : string.Empty) + note.Date.ToString("ddd, d MMM yyyy") + "   →",
                Style = (Style)FindResource("QuietButton"),
                HorizontalAlignment = HorizontalAlignment.Stretch,
                HorizontalContentAlignment = HorizontalAlignment.Left,
                Margin = new Thickness(-6, 0, 0, 0),
                Tag = note.Date,
                Foreground = note.Pinned ? (System.Windows.Media.Brush)FindResource("PrimaryDark")
                                         : (System.Windows.Media.Brush)FindResource("TextMuted")
            };
            b.Click += (_, _) => { SaveNow(); NoteDate.SelectedDate = note.Date; };
            sp.Children.Add(b);
            if (!string.IsNullOrWhiteSpace(note.Title))
                sp.Children.Add(new TextBlock
                {
                    Text = note.Title, Style = (Style)FindResource("Muted"), FontSize = 10,
                    TextTrimming = TextTrimming.CharacterEllipsis, Margin = new Thickness(4, -2, 0, 4)
                });
            NoteDates.Children.Add(sp);
        }
        NoNotes.Visibility = notes.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    }

    private void SaveNow() => SavePendingNote(SelectedDate);

    private void SavePendingNote(DateTime date)
    {
        _saveTimer.Stop();
        if (_loading) return;
        AppState.Instance.Database.SaveNote(date, NoteBox.Text);
        _dirty = false;
        SaveStatus.Text = "Saved";
        BuildDateList();
    }

    private void NoteBox_Changed(object sender, TextChangedEventArgs e)
    {
        if (_loading) return;
        _dirty = true;
        CharCount.Text = NoteBox.Text.Length + (NoteBox.Text.Length == 1 ? " character" : " characters");
        SaveStatus.Text = "Saving...";
        _saveTimer.Stop();
        _saveTimer.Start();
    }

    private void Pin_Click(object sender, RoutedEventArgs e)
    {
        if (_loading) return;
        SaveNow();
        bool pinned = PinButton.IsChecked == true;
        AppState.Instance.Database.SetNotePinned(SelectedDate, pinned);
        PinButton.Content = pinned ? "★ Pinned" : "☆ Pin";
        BuildDateList();
    }

    private void NoteSearch_Changed(object sender, TextChangedEventArgs e) => BuildDateList();
    private void NoteDate_Changed(object sender, SelectionChangedEventArgs e)
    {
        // The text in NoteBox always belongs to _displayedDate. Flush any pending
        // edit to that date before loading the newly selected date, otherwise
        // changing the DatePicker silently discards the previous day's text.
        if (!_loading && _dirty)
            SavePendingNote(_displayedDate);
        Refresh();
    }
    private void PrevDay_Click(object sender, RoutedEventArgs e) { SaveNow(); NoteDate.SelectedDate = SelectedDate.AddDays(-1); }
    private void NextDay_Click(object sender, RoutedEventArgs e) { SaveNow(); NoteDate.SelectedDate = SelectedDate.AddDays(1); }
    private void Today_Click(object sender, RoutedEventArgs e) { SaveNow(); NoteDate.SelectedDate = DateTime.Today; }
}
