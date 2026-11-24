package com.daymark.app;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Calendar;

/** Create/edit dialog for a task, including V2 category and pin fields. */
public class TaskEditor {
    public interface Saved { void onSaved(Task task); }

    /** Pass null to create; pass an existing task to edit. presetDue may be null. */
    public static void show(MainActivity activity, Task existing, String presetDue, Saved callback) {
        Task task = existing != null ? existing.copy() : new Task();
        if (existing == null && presetDue != null) task.dueDate = presetDue;

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_task, null);
        EditText title = view.findViewById(R.id.edit_title);
        EditText desc = view.findViewById(R.id.edit_desc);
        EditText category = view.findViewById(R.id.edit_category);
        Spinner priority = view.findViewById(R.id.edit_priority);
        CheckBox hasDue = view.findViewById(R.id.edit_has_due);
        DatePicker due = view.findViewById(R.id.edit_due);
        CheckBox pin = view.findViewById(R.id.edit_pin);
        Button cancel = view.findViewById(R.id.edit_cancel);
        Button save = view.findViewById(R.id.edit_save);

        title.setText(task.title);
        desc.setText(task.description);
        category.setText(task.category == null ? "" : task.category);
        priority.setSelection(task.priority);
        boolean hasDate = task.dueDate != null;
        hasDue.setChecked(hasDate);
        due.setEnabled(hasDate);
        pin.setChecked(task.pinned);
        Calendar c = Calendar.getInstance();
        if (hasDate) {
            java.util.Date d = Task.parseKey(task.dueDate);
            if (d != null) c.setTime(d);
        }
        due.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), null);
        hasDue.setOnCheckedChangeListener((v, checked) -> due.setEnabled(checked));

        AlertDialog dialog = new AlertDialog.Builder(activity).setView(view).create();
        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            String t = title.getText().toString().trim();
            if (t.isEmpty()) {
                Toast.makeText(activity, "Please give the task a title.", Toast.LENGTH_SHORT).show();
                return;
            }
            task.title = t;
            task.description = desc.getText().toString().trim();
            task.category = category.getText().toString().trim();
            task.pinned = pin.isChecked();
            task.priority = priority.getSelectedItemPosition();
            task.dueDate = hasDue.isChecked()
                    ? String.format(java.util.Locale.US, "%04d-%02d-%02d",
                        due.getYear(), due.getMonth() + 1, due.getDayOfMonth())
                    : null;
            activity.db.saveTask(task);
            dialog.dismiss();
            callback.onSaved(task);
        });
        dialog.show();
    }
}
