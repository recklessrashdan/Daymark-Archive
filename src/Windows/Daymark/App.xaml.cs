using System.Windows;
using Daymark.Services;
using Microsoft.Win32;

namespace Daymark;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        try
        {
            ApplyTheme(AppState.Instance.Theme);
            AppState.Instance.ThemeChanged += (_,__) => ApplyTheme(AppState.Instance.Theme);
        }
        catch (Exception ex)
        {
            // AppState is initialized statically, so a migration failure is wrapped in
            // TypeInitializationException. Unwrap it so the specific safety reason is shown.
            var safety = FindMigrationSafetyException(ex);
            if (safety != null)
            {
                ShowDataSafetyError(
                    safety.Message + Environment.NewLine + Environment.NewLine +
                    "Reason: " + safety.Reason + Environment.NewLine +
                    "If a safety backup was created, it remains in the Daymark backups folder. " +
                    "Your original data has not been intentionally changed.");
                return;
            }

            ShowDataSafetyError(
                "Daymark could not safely open its local data, so the upgrade was stopped. " +
                "Your tasks and notes have not been intentionally erased. " +
                "If a safety backup was created, it remains in the Daymark backups folder. " +
                "Check available disk space and try again.");
        }
    }

    private static DatabaseService.MigrationSafetyException FindMigrationSafetyException(Exception ex)
    {
        for (var current = ex; current != null; current = current.InnerException)
        {
            if (current is DatabaseService.MigrationSafetyException safety) return safety;
        }
        return null;
    }

    private static void ShowDataSafetyError(string message)
    {
        MessageBox.Show(
            message,
            "Daymark - Upgrade stopped to protect your data",
            MessageBoxButton.OK,
            MessageBoxImage.Warning);
        Current.Shutdown();
    }

    public static void ApplyTheme(string theme)
    {
        bool dark = theme == "dark" || (theme == "system" && IsSystemDark());
        var dicts = Current.Resources.MergedDictionaries;
        var existing = dicts.FirstOrDefault(d => d.Source?.OriginalString.Contains("Themes/") == true);
        if (existing != null) dicts.Remove(existing);
        dicts.Insert(0, new ResourceDictionary { Source = new Uri(dark ? "Themes/Dark.xaml" : "Themes/Light.xaml", UriKind.Relative) });
    }

    private static bool IsSystemDark()
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Themes\Personalize");
            return key?.GetValue("AppsUseLightTheme") is int v && v == 0;
        }
        catch { return false; }
    }
}
