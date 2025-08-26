package com.glance.consensus.platform.paper.polls.builder;

import com.glance.consensus.platform.paper.polls.builder.dialog.DurationPresets;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
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

    public enum Stage { GENERAL, OPTIONS, PREVIEW, OVERRIDE }

    private final UUID playerId;
    private Stage stage = Stage.GENERAL;

    private String questionRaw = "";

    private @Nullable String durationPresetId = "1h";
    private int customHours = 0;
    private int customMins = 0;

    private final List<PollOption> options = new ArrayList<>();

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
    public int optionCount() { return options.size(); }

    /** True if you can add another option */
    public boolean canAddOption() {
        return options.size() < MAX_OPTIONS;
    }

    /** Adds a new option at the end, reindexing */
    public PollOption addOption(
        @NotNull String labelRaw,
        @Nullable String tooltipRaw
    ) {
        if (!canAddOption()) throw new IllegalStateException("Max options reached");
        PollOption option = new PollOption(options.size(), labelRaw, StringUtils.emptyToNull(tooltipRaw), 0);
        options.add(option);
        return option;
    }

    /** Inserts or replaces at index; index clamped to [0..size] */
    public PollOption upsertOption(
        int index, String labelRaw, String tooltipRaw
    ) {
        int size = options.size();
        if (index < 0) index = 0;
        if (index > size) index = size; // insert at end

        if (index == size) {
            return addOption(labelRaw, tooltipRaw);
        } else {
            PollOption existing = options.get(index);
            PollOption updated = new PollOption(
                    existing.index(), labelRaw, StringUtils.emptyToNull(tooltipRaw), 0);
            options.set(index, updated);
            reindex();
            return updated;
        }
    }

    /** Removes at index if valid and reindexes */
    public boolean removeOption(int index) {
        if (index < 0 || index >= options.size()) return false;
        options.remove(index);
        reindex();
        return true;
    }

    /** Normalize indices to 0..n-1 in order */
    private void reindex() {
        for (int i = 0; i < options.size(); i++) {
            var o = options.get(i);
            if (o.index() != i) options.set(i, new PollOption(i, o.labelRaw(), o.tooltipRaw(), o.votes()));
        }
    }

    private int resolveDurationMins() {
        if (usingPresetDuration()) {
            assert durationPresetId != null;
            return DurationPresets.minutesFor(durationPresetId).orElse(5);
        }
        return Math.max(0, customHours) * (60 + Math.max(0, Math.min(59, customMins)));
    }

    private boolean usingPresetDuration() {
        return durationPresetId != null && !durationPresetId.isBlank();
    }


}
