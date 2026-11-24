package com.daymark.app;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

/** Binds one task row (item_task.xml) with completion toggle, edit and delete. */
public class TaskRow {
    public interface Listener {
        void onToggle(Task task, boolean completed);
        void onEdit(Task task);
        void onDelete(Task task);
    }

    public static View create(LayoutInflater inflater, ViewGroup parent, Task task,
                              MainActivity activity, Listener listener) {
        View row = inflater.inflate(R.layout.item_task, parent, false);
        TextView title = row.findViewById(R.id.task_title);
        TextView meta = row.findViewById(R.id.task_meta);
        CheckBox check = row.findViewById(R.id.task_check);
        View bar = row.findViewById(R.id.task_priority_bar);
        Button edit = row.findViewById(R.id.task_edit);
        Button delete = row.findViewById(R.id.task_delete);

        title.setText((task.pinned ? "◆ " : "") + task.title);
        StringBuilder m = new StringBuilder(task.priorityLabel());
        if (task.category != null && !task.category.trim().isEmpty()) m.append("  ·  ").append(task.category.trim());
        String due = task.dueLabel();
        if (!due.isEmpty()) m.append("  ·  ").append(task.isOverdue() ? "Overdue · " + due : due);
        meta.setText(m.toString());
        meta.setTextColor(activity.getColor(task.isOverdue() ? R.color.danger : android.R.color.transparent));
        if (!task.isOverdue()) meta.setTextColor(activity.mutedColor());

        int barColor = task.priority == Task.HIGH ? activity.getColor(R.color.high)
                : task.priority == Task.LOW ? activity.getColor(R.color.low)
                : activity.getColor(R.color.medium);
        bar.setBackgroundColor(barColor);

        check.setOnCheckedChangeListener(null);
        check.setChecked(task.completed);
        applyCompletedStyle(title, meta, task.completed, activity);
        check.setOnCheckedChangeListener((v, checked) -> {
            applyCompletedStyle(title, meta, checked, activity);
            listener.onToggle(task, checked);
        });

        edit.setOnClickListener(v -> listener.onEdit(task));
        delete.setOnClickListener(v -> listener.onDelete(task));
        row.setOnClickListener(v -> listener.onEdit(task));
        return row;
    }

    private static void applyCompletedStyle(TextView title, TextView meta, boolean completed, MainActivity a) {
        if (completed) {
            title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            title.setTextColor(a.mutedColor());
            meta.setAlpha(0.6f);
        } else {
            title.setPaintFlags(title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            title.setTextColor(a.textColor());
            meta.setAlpha(1f);
        }
    }
}
