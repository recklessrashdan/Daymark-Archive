package com.daymark.app;

import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Polished monthly calendar: task indicators, day selection, selected-day list and quick create. */
public class CalendarScreen implements MainActivity.Screen, TaskRow.Listener {
    private final MainActivity a;
    private View root;
    private LinearLayout grid, dayList;
    private TextView monthLabel, selectedLabel, summary, emptyDay;
    private final Calendar shown = Calendar.getInstance();
    private final Calendar selected = Calendar.getInstance();

    public CalendarScreen(MainActivity a) {
        this.a = a;
        shown.set(Calendar.DAY_OF_MONTH, 1);
        zero(selected);
        zero(shown);
    }

    private static void zero(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
    }

    public View build(LayoutInflater inflater, ViewGroup parent) {
        root = inflater.inflate(R.layout.view_calendar, parent, false);
        grid = root.findViewById(R.id.cal_grid);
        dayList = root.findViewById(R.id.cal_task_list);
        monthLabel = root.findViewById(R.id.cal_month);
        selectedLabel = root.findViewById(R.id.cal_selected);
        summary = root.findViewById(R.id.cal_summary);
        emptyDay = root.findViewById(R.id.cal_empty);
        root.findViewById(R.id.cal_prev).setOnClickListener(v -> { shown.add(Calendar.MONTH, -1); refresh(); });
        root.findViewById(R.id.cal_next).setOnClickListener(v -> { shown.add(Calendar.MONTH, 1); refresh(); });
        root.findViewById(R.id.cal_today).setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            shown.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1);
            selected.setTime(now.getTime());
            zero(shown); zero(selected);
            refresh();
        });
        root.findViewById(R.id.cal_add).setOnClickListener(v ->
                TaskEditor.show(a, null, Task.key(selected.getTime()),
                        t -> { refresh(); Toast.makeText(a, "Task added", Toast.LENGTH_SHORT).show(); }));
        return root;
    }

    @Override
    public void refresh() {
        monthLabel.setText(new SimpleDateFormat("MMMM yyyy", Locale.US).format(shown.getTime()));
        selectedLabel.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.US).format(selected.getTime()));
        buildGrid();
        List<Task> tasks = a.db.tasksForDate(Task.key(selected.getTime()));
        int done = 0;
        for (Task t : tasks) if (t.completed) done++;
        summary.setText(tasks.size() + (tasks.size() == 1 ? " task" : " tasks") + " · " + done + " done");
        dayList.removeAllViews();
        for (Task t : tasks) dayList.addView(TaskRow.create(LayoutInflater.from(a), dayList, t, a, this));
        emptyDay.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, a.getResources().getDisplayMetrics());
    }

    private void buildGrid() {
        grid.removeAllViews();
        Calendar first = (Calendar) shown.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7; // Monday first
        Calendar start = (Calendar) first.clone();
        start.add(Calendar.DAY_OF_MONTH, -offset);

        Map<String, List<Task>> byDate = new HashMap<>();
        for (Task t : a.db.allTasks()) {
            if (t.dueDate == null) continue;
            List<Task> list = byDate.get(t.dueDate);
            if (list == null) { list = new ArrayList<>(); byDate.put(t.dueDate, list); }
            list.add(t);
        }
        String todayKey = Task.todayKey();
        String selectedKey = Task.key(selected.getTime());
        int shownMonth = shown.get(Calendar.MONTH);

        for (int week = 0; week < 6; week++) {
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            for (int d = 0; d < 7; d++) {
                Calendar date = (Calendar) start.clone();
                date.add(Calendar.DAY_OF_MONTH, week * 7 + d);
                String key = Task.key(date.getTime());
                boolean outside = date.get(Calendar.MONTH) != shownMonth;
                row.addView(buildCell(date, key, outside, key.equals(todayKey),
                        key.equals(selectedKey), byDate.getOrDefault(key, new ArrayList<>())));
            }
            grid.addView(row);
        }
    }

    private View buildCell(Calendar date, String key, boolean outside, boolean isToday,
                           boolean isSelected, List<Task> dayTasks) {
        LinearLayout cell = new LinearLayout(a);
        cell.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(58), 1f);
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        cell.setLayoutParams(lp);
        cell.setPadding(dp(6), dp(5), dp(6), dp(5));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(11));
        if (isSelected) bg.setColor(a.getColor(R.color.teal));
        else if (isToday) { bg.setColor(a.altColor()); bg.setStroke(dp(1), a.getColor(R.color.amber)); }
        else { bg.setColor(a.surfaceColor()); bg.setStroke(dp(1), a.borderColor()); }
        cell.setBackground(bg);

        TextView num = new TextView(a);
        num.setText(String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
        num.setTextSize(12);
        num.setTextColor(isSelected ? 0xFFFFFFFF : outside ? a.mutedColor() : a.textColor());
        if (isToday || isSelected) num.setTypeface(null, android.graphics.Typeface.BOLD);
        cell.addView(num);

        if (!dayTasks.isEmpty()) {
            LinearLayout dots = new LinearLayout(a);
            dots.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.topMargin = dp(4);
            dots.setLayoutParams(dlp);
            int n = Math.min(dayTasks.size(), 4);
            for (int i = 0; i < n; i++) {
                Task t = dayTasks.get(i);
                View dot = new View(a);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(5), dp(5));
                p.rightMargin = dp(3);
                dot.setLayoutParams(p);
                GradientDrawable dg = new GradientDrawable();
                dg.setShape(GradientDrawable.OVAL);
                int color = isSelected ? 0xFFFFFFFF
                        : t.isOverdue() ? a.getColor(R.color.danger)
                        : t.priority == Task.HIGH ? a.getColor(R.color.high)
                        : a.getColor(R.color.teal);
                dg.setColor(color);
                dot.setBackground(dg);
                dots.addView(dot);
            }
            cell.addView(dots);
        }

        cell.setOnClickListener(v -> {
            selected.setTime(date.getTime());
            zero(selected);
            if (outside) {
                shown.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), 1);
                zero(shown);
            }
            refresh();
        });
        return cell;
    }

    @Override public void onToggle(Task task, boolean completed) { a.db.setCompleted(task.id, completed); refresh(); }
    @Override public void onEdit(Task task) { TaskEditor.show(a, task, null, t -> refresh()); }
    @Override public void onDelete(Task task) {
        Task copy = task.copy();
        a.db.deleteTask(task.id);
        refresh();
        a.offerUndo(copy);
    }
}
