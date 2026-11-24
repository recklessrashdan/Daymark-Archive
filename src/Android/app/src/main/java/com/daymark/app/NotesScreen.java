package com.daymark.app;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Daily notes with lossless autosave, search, pinning and a date library. */
public class NotesScreen implements MainActivity.Screen {
    private final MainActivity a;
    private View root;
    private EditText noteText, noteSearch;
    private TextView dateLabel, status, chars, empty;
    private Button pinButton;
    private LinearLayout dateList;
    private final Calendar selected = Calendar.getInstance();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean loading;
    private final Runnable saveRunnable = this::saveNow;

    public NotesScreen(MainActivity a) {
        this.a = a;
        zero(selected);
    }

    private static void zero(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
    }

    public View build(LayoutInflater inflater, ViewGroup parent) {
        root = inflater.inflate(R.layout.view_notes, parent, false);
        noteText = root.findViewById(R.id.note_text);
        noteSearch = root.findViewById(R.id.note_search);
        dateLabel = root.findViewById(R.id.note_date);
        status = root.findViewById(R.id.note_status);
        chars = root.findViewById(R.id.note_chars);
        empty = root.findViewById(R.id.note_empty);
        pinButton = root.findViewById(R.id.note_pin);
        dateList = root.findViewById(R.id.note_dates);

        root.findViewById(R.id.note_prev).setOnClickListener(v -> { flush(); selected.add(Calendar.DAY_OF_MONTH, -1); refresh(); });
        root.findViewById(R.id.note_next).setOnClickListener(v -> { flush(); selected.add(Calendar.DAY_OF_MONTH, 1); refresh(); });
        root.findViewById(R.id.note_today).setOnClickListener(v -> {
            flush();
            selected.setTime(new Date());
            zero(selected);
            refresh();
        });
        pinButton.setOnClickListener(v -> {
            flush();
            boolean next = !a.db.isNotePinned(key());
            a.db.setNotePinned(key(), next);
            // Display only what was actually persisted; there may be no note row yet.
            syncPinState();
            buildDateList();
        });

        noteText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int b) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (loading) return;
                updateChars();
                status.setText("Saving...");
                status.setTextColor(a.getColor(R.color.medium));
                handler.removeCallbacks(saveRunnable);
                handler.postDelayed(saveRunnable, 500);
            }
        });
        noteSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int b) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { buildDateList(); }
        });
        return root;
    }

    private String key() { return Task.key(selected.getTime()); }

    private void updateChars() {
        int n = noteText.getText().length();
        chars.setText(n + (n == 1 ? " character" : " characters"));
    }

    private void updatePin(boolean pinned) {
        pinButton.setText(pinned ? "★ Pinned" : "☆ Pin");
        pinButton.setTextColor(pinned ? a.getColor(R.color.teal_dark) : a.mutedColor());
    }

    /** Re-reads pin state from storage so the button never claims an unsaved pin. */
    private void syncPinState() {
        updatePin(a.db.isNotePinned(key()));
    }

    @Override
    public void refresh() {
        loading = true;
        dateLabel.setText(new SimpleDateFormat("EEE, d MMM yyyy", Locale.US).format(selected.getTime()));
        noteText.setText(a.db.noteFor(key()));
        updateChars();
        status.setText("Saved");
        status.setTextColor(a.getColor(R.color.low));
        syncPinState();
        loading = false;
        buildDateList();
    }

    private void buildDateList() {
        dateList.removeAllViews();
        List<Db.NoteInfo> notes = a.db.searchNotes(noteSearch.getText().toString());
        for (Db.NoteInfo n : notes) {
            Date d = Task.parseKey(n.date);
            if (d == null) continue;
            String title = n.title == null ? "" : n.title.trim();
            String label = (n.pinned ? "★ " : "") + new SimpleDateFormat("d MMM", Locale.US).format(d)
                    + (title.isEmpty() ? "" : " · " + title);
            Button b = new Button(a);
            b.setText(label);
            b.setAllCaps(false);
            b.setTextSize(11);
            b.setTextColor(n.pinned ? a.getColor(R.color.teal_dark) : a.mutedColor());
            b.setBackgroundResource(n.pinned ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
            if (n.pinned) b.setTextColor(0xFFFFFFFF);
            b.setMinHeight(0); b.setMinWidth(0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = 8;
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> {
                flush();
                Date target = Task.parseKey(n.date);
                if (target != null) { selected.setTime(target); zero(selected); refresh(); }
            });
            dateList.addView(b);
        }
        empty.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void saveNow() {
        handler.removeCallbacks(saveRunnable);
        if (loading) return;
        a.db.saveNote(key(), noteText.getText().toString());
        status.setText("Saved");
        status.setTextColor(a.getColor(R.color.low));
        syncPinState();
        buildDateList();
    }

    /** Called before leaving the screen so no keystroke is lost. */
    @Override
    public void flush() { saveNow(); }
}
