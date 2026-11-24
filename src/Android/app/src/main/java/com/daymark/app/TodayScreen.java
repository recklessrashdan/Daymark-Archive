package com.daymark.app;

import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Dashboard: date/greeting, overview, progress, insights, priorities, deadlines, notes and navigation. */
public class TodayScreen implements MainActivity.Screen, TaskRow.Listener {
    private final MainActivity a;
    private View root;
    private LinearLayout taskList, priorityList, weekBars;
    private TextView emptyTasks, emptyPriority, percent, stats, notePreview;
    private TextView overdueView, upcomingView, highView, weeklyDone, streakView;
    private ProgressBar progress;
    private EditText quick;

    public TodayScreen(MainActivity a) { this.a = a; }

    public View build(LayoutInflater inflater, ViewGroup parent) {
        root = inflater.inflate(R.layout.view_today, parent, false);
        taskList = root.findViewById(R.id.today_task_list);
        priorityList = root.findViewById(R.id.today_priority_list);
        weekBars = root.findViewById(R.id.today_week_bars);
        emptyTasks = root.findViewById(R.id.today_empty);
        emptyPriority = root.findViewById(R.id.today_priority_empty);
        percent = root.findViewById(R.id.today_percent);
        stats = root.findViewById(R.id.today_stats);
        progress = root.findViewById(R.id.today_progress);
        notePreview = root.findViewById(R.id.today_note);
        overdueView = root.findViewById(R.id.today_overdue);
        upcomingView = root.findViewById(R.id.today_upcoming);
        highView = root.findViewById(R.id.today_high);
        weeklyDone = root.findViewById(R.id.today_weekly_done);
        streakView = root.findViewById(R.id.today_streak);
        quick = root.findViewById(R.id.today_quick);

        root.findViewById(R.id.today_add).setOnClickListener(v -> addQuick());
        root.findViewById(R.id.today_new).setOnClickListener(v ->
                TaskEditor.show(a, null, Task.todayKey(), t -> { refresh(); toast("Task created"); }));
        root.findViewById(R.id.today_open_notes).setOnClickListener(v -> a.show("notes"));
        root.findViewById(R.id.today_open_calendar).setOnClickListener(v -> a.show("calendar"));
        root.findViewById(R.id.today_open_tasks).setOnClickListener(v -> a.show("tasks"));
        overdueView.setOnClickListener(v -> a.filterTasks("overdue"));
        upcomingView.setOnClickListener(v -> a.filterTasks("upcoming"));
        highView.setOnClickListener(v -> a.filterTasks("high"));
        return root;
    }

    private void addQuick() {
        String title = quick.getText().toString().trim();
        if (title.isEmpty()) return;
        Task t = new Task();
        t.title = title;
        t.dueDate = Task.todayKey();
        t.priority = Task.MEDIUM;
        a.db.saveTask(t);
        quick.setText("");
        refresh();
        toast("Task added for today");
    }

    private List<Task> todayTasks() {
        String today = Task.todayKey();
        List<Task> out = new ArrayList<>();
        for (Task t : a.db.allTasks()) {
            if (today.equals(t.dueDate) || (t.dueDate == null && !t.completed)) out.add(t);
        }
        return out;
    }

    private List<Task> priorityTasks(List<Task> all) {
        List<Task> out = new ArrayList<>();
        for (Task t : all) if (!t.completed && (t.priority == Task.HIGH || t.pinned)) out.add(t);
        out.sort((x, y) -> {
            if (x.pinned != y.pinned) return x.pinned ? -1 : 1;
            if (x.dueDate == null && y.dueDate == null) return 0;
            if (x.dueDate == null) return 1;
            if (y.dueDate == null) return -1;
            return x.dueDate.compareTo(y.dueDate);
        });
        return out;
    }

    @Override
    public void refresh() {
        Date now = new Date();
        ((TextView) root.findViewById(R.id.today_date)).setText(
                new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(now).toUpperCase(Locale.US));
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        ((TextView) root.findViewById(R.id.today_greeting)).setText(
                hour < 12 ? "Good morning." : hour < 17 ? "Good afternoon." : "Good evening.");

        List<Task> all = a.db.allTasks();
        List<Task> today = todayTasks();
        int total = today.size();
        int done = 0;
        for (Task t : today) if (t.completed) done++;
        int pct = total == 0 ? 0 : Math.round(done * 100f / total);
        percent.setText(pct + "%");
        progress.setProgress(pct);
        stats.setText(done + " of " + total + " done · " + (total - done) + " remaining");

        int overdue = 0, upcoming = 0;
        Calendar end = Calendar.getInstance();
        end.add(Calendar.DAY_OF_MONTH, 7);
        String endKey = Task.key(end.getTime());
        String todayKey = Task.todayKey();
        for (Task t : all) {
            if (t.isOverdue()) overdue++;
            if (!t.completed && t.dueDate != null && t.dueDate.compareTo(todayKey) > 0
                    && t.dueDate.compareTo(endKey) <= 0) upcoming++;
        }
        List<Task> priority = priorityTasks(all);
        overdueView.setText("OVERDUE\n" + overdue);
        upcomingView.setText("NEXT 7 DAYS\n" + upcoming);
        highView.setText("PRIORITY\n" + priority.size());

        buildWeekBars();

        render(taskList, today, emptyTasks);
        render(priorityList, priority, emptyPriority);

        ((TextView) root.findViewById(R.id.today_note_date)).setText(
                new SimpleDateFormat("d MMMM yyyy", Locale.US).format(now));
        String note = a.db.noteFor(todayKey);
        boolean blank = note == null || note.trim().isEmpty();
        notePreview.setText(blank ? "Your thoughts for today will appear here." : note);
        notePreview.setAlpha(blank ? 0.55f : 1f);
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, a.getResources().getDisplayMetrics());
    }

    /** Real completion counts only; V1 completions had no timestamp and are never invented. */
    private void buildWeekBars() {
        Calendar from = Calendar.getInstance();
        from.add(Calendar.DAY_OF_MONTH, -6);
        Map<String, Integer> map = a.db.completionsSince(Task.key(from.getTime()));
        int[] counts = new int[7];
        int max = 1;
        int total = 0;
        for (int i = 0; i < 7; i++) {
            Calendar d = (Calendar) from.clone();
            d.add(Calendar.DAY_OF_MONTH, i);
            counts[i] = map.getOrDefault(Task.key(d.getTime()), 0);
            total += counts[i];
            max = Math.max(max, counts[i]);
        }
        weeklyDone.setText(total + (total == 1 ? " done" : " done"));

        int streak = 0;
        Calendar cursor = Calendar.getInstance();
        if (map.getOrDefault(Task.key(cursor.getTime()), 0) == 0) cursor.add(Calendar.DAY_OF_MONTH, -1);
        while (map.getOrDefault(Task.key(cursor.getTime()), 0) > 0) {
            streak++;
            cursor.add(Calendar.DAY_OF_MONTH, -1);
        }
        streakView.setText(streak == 0 ? "—" : streak + (streak == 1 ? " day" : " days"));

        weekBars.removeAllViews();
        for (int i = 0; i < 7; i++) {
            Calendar d = (Calendar) from.clone();
            d.add(Calendar.DAY_OF_MONTH, i);
            LinearLayout col = new LinearLayout(a);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, dp(52), 1f));

            View bar = new View(a);
            int h = counts[i] == 0 ? dp(3) : Math.max(dp(8), (int) (dp(38) * counts[i] / (float) max));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(14), h);
            bp.bottomMargin = dp(2);
            bar.setLayoutParams(bp);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(5));
            boolean isToday = i == 6;
            bg.setColor(a.getColor(isToday ? R.color.amber : counts[i] > 0 ? R.color.teal : R.color.border));
            bar.setBackground(bg);
            col.addView(bar);

            TextView label = new TextView(a);
            label.setText(new SimpleDateFormat("EEE", Locale.US).format(d.getTime()).substring(0, 1));
            label.setTextSize(9);
            label.setGravity(Gravity.CENTER);
            label.setTextColor(isToday ? a.getColor(R.color.teal_dark) : a.mutedColor());
            col.addView(label);
            weekBars.addView(col);
        }
    }

    private void render(LinearLayout panel, List<Task> tasks, TextView empty) {
        panel.removeAllViews();
        int n = Math.min(tasks.size(), 6);
        for (int i = 0; i < n; i++) {
            panel.addView(TaskRow.create(LayoutInflater.from(a), panel, tasks.get(i), a, this));
        }
        empty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override public void onToggle(Task task, boolean completed) { a.db.setCompleted(task.id, completed); refresh(); }
    @Override public void onEdit(Task task) { TaskEditor.show(a, task, null, t -> refresh()); }
    @Override public void onDelete(Task task) {
        Task copy = task.copy();
        a.db.deleteTask(task.id);
        refresh();
        a.offerUndo(copy);
    }

    private void toast(String s) { Toast.makeText(a, s, Toast.LENGTH_SHORT).show(); }
}
