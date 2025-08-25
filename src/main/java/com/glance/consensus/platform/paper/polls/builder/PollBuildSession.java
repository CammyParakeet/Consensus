package com.glance.consensus.platform.paper.polls.builder;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Ephemeral per-player state for the poll creation wizard */
@Data
public final class PollBuildSession {

    public enum Stage { GENERAL, OPTIONS, PREVIEW, OVERRIDE }

    private final UUID playerId;
    private Stage stage = Stage.GENERAL;

    private String questionRaw = "";
    private int durationMinutes = 60;

    private final List<String> options = new ArrayList<>();

    /** When true, creation is locked (after confirmation) to avoid double submissions */
    private boolean creating = false;

    public PollBuildSession(UUID playerId) {
        this.playerId = playerId;
    }

}
