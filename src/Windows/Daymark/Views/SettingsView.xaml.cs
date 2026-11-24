using System.Windows;
using System.Windows.Controls;
using Daymark.Services;
using Microsoft.Win32;

namespace Daymark.Views;

public partial class SettingsView : UserControl, IRefreshable
{
    public SettingsView()
    {
        InitializeComponent();
        DbPath.Text = AppState.Instance.Database.DatabasePath;
        StorageInfo.Text = $"{AppState.Instance.Database.CountTasks()} tasks · " +
                           $"{AppState.Instance.Database.DatabaseSizeBytes / 1024.0:F0} KB on disk";
        SchemaInfo.Text = "V2 (schema " + AppState.Instance.Database.SchemaVersionNow + ")";
        var theme = AppState.Instance.Theme;
        (theme switch { "light" => LightTheme, "dark" => DarkTheme, _ => SystemTheme }).IsChecked = true;
        Loaded += (_, _) => Refresh();
    }

    public void Refresh()
    {
        StorageInfo.Text = $"{AppState.Instance.Database.CountTasks()} tasks · " +
                           $"{AppState.Instance.Database.DatabaseSizeBytes / 1024.0:F0} KB on disk";
    }

    private void Theme_Checked(object sender, RoutedEventArgs e)
    {
        if (sender is RadioButton rb && rb.IsChecked == true)
            AppState.Instance.SetTheme(rb.Tag?.ToString() ?? "system");
    }

    private void Backup_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new SaveFileDialog
        {
            Title = "Save Daymark backup",
            Filter = "SQLite database (*.db)|*.db",
            FileName = $"daymark-backup-{DateTime.Now:yyyyMMdd-HHmmss}.db"
        };
        if (dialog.ShowDialog() != true) return;
        try
        {
            AppState.Instance.Database.BackupTo(dialog.FileName);
            MainWindow.Current.ShowToast("Backup created");
        }
        catch (Exception ex)
        {
            MessageBox.Show("Backup failed: " + ex.Message, "Daymark", MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    private void Clear_Click(object sender, RoutedEventArgs e)
    {
        var result = MessageBox.Show(
            "Delete all tasks and notes on this device? This cannot be undone.\n\n" +
            "A backup copy is recommended first (use 'Create backup copy').",
            "Clear all data", MessageBoxButton.YesNo, MessageBoxImage.Warning, MessageBoxResult.No);
        if (result == MessageBoxResult.Yes)
        {
            AppState.Instance.Database.ClearAllUserData();
            AppState.Instance.Refresh();
            Refresh();
            MainWindow.Current.ShowToast("All local data cleared");
        }
    }
}
