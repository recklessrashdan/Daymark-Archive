package com.daymark.app;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/** Shared Daymark V2 task model. Mirrors the Windows edition and the migrated schema. */
public class Task {
    public static final int LOW = 0;
    public static final int MEDIUM = 1;
    public static final int HIGH = 2;

    public String id = UUID.randomUUID().toString().replace("-", "");
    public String title = "";
    public String description = "";
    public String category = "";
    public boolean pinned;
    public int priority = MEDIUM;
    /** ISO yyyy-MM-dd, nullable. */
    public String dueDate;
    public boolean completed;
    /** ISO timestamp of the actual completion; V1 rows have none. */
    public String completedAt;
    public String createdAt = nowIso();
    public String updatedAt = nowIso();

    public static String nowIso() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
    }

    public static String todayKey() {
        return key(new Date());
    }

    public static String key(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d);
    }

    public static Date parseKey(String s) {
        try { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s); }
        catch (Exception e) { return null; }
    }

    public String priorityLabel() {
        return priority == HIGH ? "High" : priority == LOW ? "Low" : "Medium";
    }

    public boolean isOverdue() {
        if (dueDate == null || completed) return false;
        Date d = parseKey(dueDate);
        return d != null && d.before(dayStart(new Date()));
    }

    public static Date dayStart(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public String dueLabel() {
        if (dueDate == null) return "";
        Date d = parseKey(dueDate);
        if (d == null) return "";
        String today = todayKey();
        Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_MONTH, 1);
        if (dueDate.equals(today)) return "Today";
        if (dueDate.equals(key(c.getTime()))) return "Tomorrow";
        return new SimpleDateFormat("EEE, d MMM", Locale.US).format(d);
    }

    public Task copy() {
        Task t = new Task();
        t.id = id; t.title = title; t.description = description; t.category = category;
        t.pinned = pinned; t.priority = priority; t.dueDate = dueDate; t.completed = completed;
        t.completedAt = completedAt; t.createdAt = createdAt; t.updatedAt = updatedAt;
        return t;
    }
}
