package com.glance.consensus.platform.paper.polls.builder;

import com.glance.consensus.platform.paper.polls.builder.dialog.DurationPresets;
import com.glance.consensus.platform.paper.polls.domain.PollAnswer;
import com.glance.consensus.utils.StringUtils;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Ephemeral per-player state for the poll creation wizard */
@Data
public final class PollBuildSession {

    public static final int MAX_OPTIONS = 6;

    public enum Stage { GENERAL, ANSWER, PREVIEW, OVERRIDE }

    private final UUID playerId;
    private Stage stage = Stage.GENERAL;

    private String questionRaw = "";

    private @Nullable String durationPresetId = "1h";
    private int customHours = 0;
    private int customMins = 0;

    private final List<PollAnswer> answers = new ArrayList<>();

    private boolean multipleChoice = false; // default to single-answer
    private int maxSelections = 1; // capped to 1 if multipleChoice = false, and to MAX_OPTIONS
    private boolean allowResubmission = false; // only respected for single-answer polls

    private int editingIndex = 0;

    /** When true, creation is locked (after confirmation) to avoid double submissions */
    private boolean creating = false;

    public PollBuildSession(UUID playerId) {
        this.playerId = playerId;
    }

    /* ---------- Editing helpers ---------- */

    /** Returns current count */
    public int answerCount() { return answers.size(); }

    /** True if you can add another option */
    public boolean canAddAnswer() {
        return answers.size() < MAX_OPTIONS;
    }

    /** Adds a new answer at the end, reindexing */
    public PollAnswer addAnswer(
        @NotNull String labelRaw,
        @Nullable String tooltipRaw
    ) {
        if (!canAddAnswer()) throw new IllegalStateException("Max poll answers reached");
        PollAnswer option = new PollAnswer(answers.size(), labelRaw, StringUtils.emptyToNull(tooltipRaw), 0);
        answers.add(option);
        return option;
    }

    /** Inserts or replaces at index; index clamped to [0..size] */
    public PollAnswer upsertAnswer(
        int index, String labelRaw, String tooltipRaw
    ) {
        int size = answers.size();
        if (index < 0) index = 0;
        if (index > size) index = size; // insert at end

        if (index == size) {
            return addAnswer(labelRaw, tooltipRaw);
        } else {
            PollAnswer existing = answers.get(index);
            PollAnswer updated = new PollAnswer(
                    existing.index(), labelRaw, StringUtils.emptyToNull(tooltipRaw), 0);
            answers.set(index, updated);
            reindex();
            return updated;
        }
    }

    /** Removes at index if valid and reindexes */
    public boolean removeAnswer(int index) {
        if (index < 0 || index >= answers.size()) return false;
        answers.remove(index);
        reindex();
        return true;
    }

    /* ---------- Reordering helpers ---------- */

    /** Can the current editing answer move up one position? */
    public boolean canMoveEditingUp() {
        return editingIndex > 0 && editingIndex < answers.size();
    }

    /** Can the current editing answer move down one position? */
    public boolean canMoveEditingDown() {
        return editingIndex >= 0 && editingIndex < answers.size() - 1;
    }

    /** Move the answer at index 'from' to index 'to' (clamped) */
    public boolean moveAnswer(int from, int to) {
        int size = answers.size();
        if (from < 0 || from >= size) return false;

        to = Math.max(0, Math.min(size - 1, to));
        if (from == to) return false;

        final PollAnswer moved = answers.remove(from);
        answers.add(to, moved);
        reindex();

        return true;
    }

    /** Swap the editing answer up by one slot (index-1) */
    public boolean moveEditingUp() {
        if (!canMoveEditingUp()) return false;
        return moveAnswer(editingIndex, editingIndex - 1);
    }

    /** Swap the editing answer down by one slot (index+1) */
    public boolean moveEditingDown() {
        if (!canMoveEditingDown()) return false;
        return moveAnswer(editingIndex, editingIndex + 1);
    }

    /** Normalize indices to 0..n-1 in order */
    private void reindex() {
        for (int i = 0; i < answers.size(); i++) {
            var o = answers.get(i);
            if (o.index() != i) answers.set(i, new PollAnswer(i, o.labelRaw(), o.tooltipRaw(), o.votes()));
        }
    }

    public int resolveDurationMins() {
        if (usingPresetDuration()) {
            assert durationPresetId != null;
            return DurationPresets.minutesFor(durationPresetId).orElse(5);
        }
        int hours = Math.max(0, customHours);
        int mins  = Math.max(0, Math.min(59, customMins));
        return hours * 60 + mins;
    }

    private boolean usingPresetDuration() {
        return durationPresetId != null && !durationPresetId.isBlank();
    }


}
