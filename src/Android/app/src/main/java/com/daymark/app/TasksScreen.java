package com.daymark.app;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Task library: search, smart filters, categories, sorting, quick add and undo delete. */
public class TasksScreen implements MainActivity.Screen, TaskRow.Listener {
    private final MainActivity a;
    private View root;
    private LinearLayout list;
    private TextView empty, count, group, result;
    private EditText search, quick;
    private Spinner categorySpinner, sortSpinner;
    private String filter = "all";
    private String category = "All categories";
    private boolean initializing = true;
    private final Map<String, Button> chips = new LinkedHashMap<>();

    public TasksScreen(MainActivity a) { this.a = a; }

    public View build(LayoutInflater inflater, ViewGroup parent) {
        root = inflater.inflate(R.layout.view_tasks, parent, false);
        list = root.findViewById(R.id.tasks_list);
        empty = root.findViewById(R.id.tasks_empty);
        count = root.findViewById(R.id.tasks_count);
        group = root.findViewById(R.id.tasks_group);
        result = root.findViewById(R.id.tasks_result);
        search = root.findViewById(R.id.tasks_search);
        quick = root.findViewById(R.id.tasks_quick);
        categorySpinner = root.findViewById(R.id.tasks_category);
        sortSpinner = root.findViewById(R.id.tasks_sort);

        chips.put("all", root.findViewById(R.id.filter_all));
        chips.put("active", root.findViewById(R.id.filter_active));
        chips.put("completed", root.findViewById(R.id.filter_completed));
        chips.put("high", root.findViewById(R.id.filter_high));
        chips.put("overdue", root.findViewById(R.id.filter_overdue));
        chips.put("upcoming", root.findViewById(R.id.filter_upcoming));
        for (final Map.Entry<String, Button> e : chips.entrySet()) {
            e.getValue().setOnClickListener(v -> setFilter(e.getKey()));
        }

        root.findViewById(R.id.tasks_new).setOnClickListener(v ->
                TaskEditor.show(a, null, null, t -> { refresh(); Toast.makeText(a, "Task created", Toast.LENGTH_SHORT).show(); }));
        root.findViewById(R.id.tasks_quick_add).setOnClickListener(v -> quickAdd());

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int b) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refresh(); }
        });
        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                if (initializing) return;
                category = String.valueOf(p.getItemAtPosition(pos));
                refresh();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        sortSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                if (initializing) return;
                refresh();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        initializing = false;
        return root;
    }

    private void quickAdd() {
        String title = quick.getText().toString().trim();
        if (title.isEmpty()) return;
        Task t = new Task();
        t.title = title;
        t.priority = Task.MEDIUM;
        a.db.saveTask(t);
        quick.setText("");
        refresh();
        Toast.makeText(a, "Task added", Toast.LENGTH_SHORT).show();
    }

    private void setFilter(String f) { filter = f; refresh(); }

    /** Called by dashboard insight tiles. */
    public void applyFilter(String f) {
        filter = (f == null || !chips.containsKey(f)) ? "all" : f;
        if (root != null) refresh();
    }

    private void styleChip(Button b, boolean selected) {
        b.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        b.setTextColor(selected ? 0xFFFFFFFF : a.mutedColor());
    }

    @Override
    public void refresh() {
        List<Task> all = a.db.allTasks();
        int active = 0, done = 0, overdueAll = 0;
        for (Task t : all) {
            if (t.completed) done++; else active++;
            if (t.isOverdue()) overdueAll++;
        }
        count.setText(active + " active  ·  " + done + " completed  ·  " + overdueAll + " overdue");

        for (Map.Entry<String, Button> e : chips.entrySet()) styleChip(e.getValue(), e.getKey().equals(filter));

        populateCategories();

        String query = search.getText().toString().trim().toLowerCase(Locale.US);
        Calendar week = Calendar.getInstance();
        week.add(Calendar.DAY_OF_MONTH, 7);
        String todayKey = Task.todayKey();
        String weekKey = Task.key(week.getTime());

        List<Task> items = new ArrayList<>();
        for (Task t : all) {
            if (!query.isEmpty()) {
                String hay = (t.title + " " + (t.description == null ? "" : t.description)
                        + " " + (t.category == null ? "" : t.category)).toLowerCase(Locale.US);
                if (!hay.contains(query)) continue;
            }
            if (!"All categories".equals(category) && !category.equals(t.category)) continue;
            switch (filter) {
                case "active": if (t.completed) continue; break;
                case "completed": if (!t.completed) continue; break;
                case "high": if (!(!t.completed && (t.priority == Task.HIGH || t.pinned))) continue; break;
                case "overdue": if (!t.isOverdue()) continue; break;
                case "upcoming":
                    if (t.completed || t.dueDate == null || t.dueDate.compareTo(todayKey) <= 0
                            || t.dueDate.compareTo(weekKey) > 0) continue;
                    break;
                default: break;
            }
            items.add(t);
        }

        int sort = sortSpinner.getSelectedItemPosition();
        final int sortIndex = sort < 0 ? 0 : sort;
        items.sort((x, y) -> {
            switch (sortIndex) {
                case 1:
                    if (x.dueDate == null && y.dueDate == null) return 0;
                    if (x.dueDate == null) return 1;
                    if (y.dueDate == null) return -1;
                    return x.dueDate.compareTo(y.dueDate);
                case 2:
                    if (x.priority != y.priority) return Integer.compare(y.priority, x.priority);
                    return x.title.compareToIgnoreCase(y.title);
                case 3:
                    return y.createdAt.compareTo(x.createdAt);
                case 4:
                    return x.title.compareToIgnoreCase(y.title);
                default:
                    if (x.pinned != y.pinned) return x.pinned ? -1 : 1;
                    if (x.completed != y.completed) return x.completed ? 1 : -1;
                    if (x.dueDate == null && y.dueDate == null) return 0;
                    if (x.dueDate == null) return 1;
                    if (y.dueDate == null) return -1;
                    return x.dueDate.compareTo(y.dueDate);
            }
        });

        group.setText("all".equals(filter) ? "Everything"
                : "active".equals(filter) ? "Active tasks"
                : "completed".equals(filter) ? "Completed tasks"
                : "high".equals(filter) ? "Priority focus"
                : "overdue".equals(filter) ? "Overdue tasks" : "Upcoming deadlines");
        result.setText(items.size() + (items.size() == 1 ? " task" : " tasks"));

        list.removeAllViews();
        for (Task t : items) list.addView(TaskRow.create(LayoutInflater.from(a), list, t, a, this));
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void populateCategories() {
        List<String> values = new ArrayList<>();
        values.add("All categories");
        values.addAll(a.db.categories());
        String selected = values.contains(category) ? category : "All categories";
        category = selected;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(a,
                android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        boolean before = initializing;
        initializing = true;
        categorySpinner.setAdapter(adapter);
        categorySpinner.setSelection(values.indexOf(selected), false);
        initializing = before;
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
