package com.daymark.app;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

/** Preferences: theme, local storage controls, about. */
public class SettingsScreen implements MainActivity.Screen {
    private final MainActivity a;
    private View root;
    private boolean initializing = true;

    public SettingsScreen(MainActivity a) { this.a = a; }

    public View build(LayoutInflater inflater, ViewGroup parent) {
        root = inflater.inflate(R.layout.view_settings, parent, false);
        TextView storage = root.findViewById(R.id.settings_storage);
        storage.setText(a.db.taskCount() + " tasks · " + a.db.noteCount() + " notes · "
                + Math.max(1, a.db.dbSizeBytes() / 1024) + " KB · schema V" + a.db.schemaVersion());
        TextView dbPath = root.findViewById(R.id.settings_db_path);
        dbPath.setText("Stored at: " + a.getDatabasePath("daymark.db").getAbsolutePath());

        RadioGroup group = root.findViewById(R.id.theme_group);
        String theme = a.db.setting("theme", "system");
        int checked = theme.equals("light") ? R.id.theme_light
                : theme.equals("dark") ? R.id.theme_dark : R.id.theme_system;
        group.check(checked);
        initializing = false;
        group.setOnCheckedChangeListener((g, id) -> {
            if (initializing) return;
            String value = id == R.id.theme_light ? "light" : id == R.id.theme_dark ? "dark" : "system";
            a.db.setSetting("theme", value);
            a.recreate();
        });

        root.findViewById(R.id.settings_backup).setOnClickListener(v -> {
            String path = a.db.createBackupCopy();
            Toast.makeText(a, path == null ? "Backup failed" : "Backup saved to app storage", Toast.LENGTH_SHORT).show();
        });

        root.findViewById(R.id.clear_data).setOnClickListener(v ->
                new AlertDialog.Builder(a)
                        .setTitle("Clear all data")
                        .setMessage("Delete all tasks and notes on this device? This cannot be undone.\n\n"
                                + "Use 'Create backup copy' first if you may need the data later.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (d, w) -> {
                            // Cancel any pending delete undo before erasing the data
                            // it refers to, so it cannot reappear after clear-all.
                            a.clearPendingUndo();
                            a.db.clearTasksAndNotes();
                            Toast.makeText(a, "All local data cleared", Toast.LENGTH_SHORT).show();
                            a.show("today");
                        })
                        .show());
        return root;
    }

    @Override public void refresh() { }
}
