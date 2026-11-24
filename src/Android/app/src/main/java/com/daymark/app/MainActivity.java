package com.daymark.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Daymark for Android — offline daily planner.
 * Hosts the five screens (Today, Tasks, Calendar, Notes, Settings),
 * applies the saved theme, and provides the shared undo-delete bar.
 */
public class MainActivity extends Activity {
    public Db db;
    private FrameLayout content;
    private LinearLayout undoBar;
    private TextView undoText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Task pendingUndo;
    private Runnable undoTimeout;
    private Screen current;
    private String currentId = "today";
    private final Button[] navButtons = new Button[5];
    private static final String[] NAV_IDS = {"today", "tasks", "calendar", "notes", "settings"};
    private boolean upgradeBlocked;
    private String upgradeStatus = "";
    private String upgradeDetail = "";

    public interface Screen {
        void refresh();
        default void flush() {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Db opened = null;
        String pref = "system";
        try {
            // One open performs any necessary migration and reads the saved theme.
            opened = new Db(this);
            pref = opened.setting("theme", "system");
        } catch (Db.MigrationBlockedException e) {
            markUpgradeProblem("blocked", e.reason);
        } catch (Db.PostBackupMigrationException e) {
            markUpgradeProblem("post_backup_failed", e.detail);
        } catch (RuntimeException e) {
            markUpgradeProblem("open_failed", e.getClass().getSimpleName());
        }

        setTheme(themeFor(pref));
        super.onCreate(savedInstanceState);

        if (upgradeBlocked) {
            if (opened != null) {
                try { opened.close(); } catch (Exception ignored) { }
            }
            showUpgradeProblemDialog();
            return;
        }

        db = opened;

        setContentView(R.layout.activity_main);
        content = findViewById(R.id.content);
        undoBar = findViewById(R.id.undo_bar);
        undoText = findViewById(R.id.undo_text);
        findViewById(R.id.undo_button).setOnClickListener(v -> undoDelete());

        int[] navIds = {R.id.nav_today, R.id.nav_tasks, R.id.nav_calendar, R.id.nav_notes, R.id.nav_settings};
        for (int i = 0; i < navIds.length; i++) {
            final String id = NAV_IDS[i];
            navButtons[i] = findViewById(navIds[i]);
            navButtons[i].setOnClickListener(v -> show(id));
        }
        show("today");
    }

    private void markUpgradeProblem(String status, String detail) {
        upgradeBlocked = true;
        upgradeStatus = status == null ? "open_failed" : status;
        upgradeDetail = detail == null ? "" : detail;
    }

    private int themeFor(String pref) {
        boolean dark;
        if ("dark".equals(pref)) dark = true;
        else if ("light".equals(pref)) dark = false;
        else dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        return dark ? R.style.DaymarkTheme_Dark : R.style.DaymarkTheme;
    }

    /** Explains what happened without exposing paths or private data. */
    private void showUpgradeProblemDialog() {
        String title;
        String message;
        if ("blocked".equals(upgradeStatus)) {
            title = "Upgrade stopped to protect your data";
            message = "Daymark could not create and verify a safety backup of your existing data. " +
                    "The V1 to V2 upgrade was stopped before any changes were made.\n\n" +
                    "Your tasks and notes have not been intentionally erased. Free up storage space " +
                    "or check app storage access, then reopen Daymark and try again.";
        } else if ("post_backup_failed".equals(upgradeStatus)) {
            title = "Upgrade could not be completed";
            message = "A verified safety backup was created, but the upgrade did not finish. " +
                    "Your original data has not been intentionally erased and the backup was kept for recovery.\n\n" +
                    "Error: " + (upgradeDetail.isEmpty() ? "unknown" : upgradeDetail) + "\n\n" +
                    "Reopen Daymark to try again. If it continues, contact support without sharing private data.";
        } else {
            title = "Daymark could not be opened";
            message = "Daymark could not open its local database. No backup has been confirmed for this attempt.\n\n" +
                    "Error: " + (upgradeDetail.isEmpty() ? "unknown" : upgradeDetail) + "\n\n" +
                    "Reopen Daymark to try again. If it continues, contact support without sharing private data.";
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Close", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    public void show(String id) {
        if (current != null) current.flush();
        currentId = id;
        hideUndo();
        LayoutInflater inflater = LayoutInflater.from(this);
        View view;
        switch (id) {
            case "tasks": {
                TasksScreen s = new TasksScreen(this);
                view = s.build(inflater, content);
                current = s;
                break;
            }
            case "calendar": {
                CalendarScreen s = new CalendarScreen(this);
                view = s.build(inflater, content);
                current = s;
                break;
            }
            case "notes": {
                NotesScreen s = new NotesScreen(this);
                view = s.build(inflater, content);
                current = s;
                break;
            }
            case "settings": {
                SettingsScreen s = new SettingsScreen(this);
                view = s.build(inflater, content);
                current = s;
                break;
            }
            default: {
                TodayScreen s = new TodayScreen(this);
                view = s.build(inflater, content);
                current = s;
                break;
            }
        }
        content.removeAllViews();
        content.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        for (int i = 0; i < navButtons.length; i++) {
            boolean active = NAV_IDS[i].equals(id);
            navButtons[i].setTextColor(active ? themedColor(R.attr.day_primary_dark) : mutedColor());
            navButtons[i].setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
        if (current != null) current.refresh();
    }

    public void refreshCurrent() {
        if (current != null) current.refresh();
    }

    /** Dashboard insight tiles open the task library on a preselected filter. */
    public void filterTasks(String filter) {
        if (current instanceof TasksScreen) {
            ((TasksScreen) current).applyFilter(filter);
            return;
        }
        show("tasks");
        if (current instanceof TasksScreen) ((TasksScreen) current).applyFilter(filter);
    }

    public int textColor() { return themedColor(R.attr.day_text); }
    public int mutedColor() { return themedColor(R.attr.day_muted); }
    public int surfaceColor() { return themedColor(R.attr.day_surface); }
    public int altColor() { return themedColor(R.attr.day_alt); }
    public int borderColor() { return themedColor(R.attr.day_border); }

    private int themedColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    // ---------- Undo delete ----------

    public void offerUndo(Task deleted) {
        pendingUndo = deleted;
        undoText.setText("Deleted \u201C" + deleted.title + "\u201D");
        undoBar.setVisibility(View.VISIBLE);
        if (undoTimeout != null) handler.removeCallbacks(undoTimeout);
        undoTimeout = this::hideUndo;
        handler.postDelayed(undoTimeout, 6000);
    }

    /** Cancels any pending undo, e.g. before all local data is cleared. */
    public void clearPendingUndo() {
        hideUndo();
    }

    private void undoDelete() {
        if (pendingUndo != null) {
            db.restoreTask(pendingUndo);
            pendingUndo = null;
            hideUndo();
            refreshCurrent();
        }
    }

    private void hideUndo() {
        if (undoTimeout != null) { handler.removeCallbacks(undoTimeout); undoTimeout = null; }
        pendingUndo = null;
        if (undoBar != null) undoBar.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        if (current != null) current.flush();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (current != null) current.flush();
        handler.removeCallbacksAndMessages(null);
        if (db != null) db.close();
        super.onDestroy();
    }
}