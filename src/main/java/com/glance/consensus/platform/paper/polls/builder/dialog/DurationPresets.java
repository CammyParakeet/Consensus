package com.glance.consensus.platform.paper.polls.builder.dialog;

import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class DurationPresets {

    private record Preset(String id, int minutes, Component display) {}

    private static final List<Preset> PRESETS = List.of(
        new Preset("5m", 5, Component.text("5 Mins")),
        new Preset("15m", 15, Component.text("15 Mins")),
        new Preset("30m", 30, Component.text("30 Mins")),
        new Preset("1h", 60, Component.text("1 Hour")),
        new Preset("2h", 120, Component.text("2 Hours")),
        new Preset("4h", 240, Component.text("4 Hours")),
        new Preset("6h", 360, Component.text("6 Hours")),
        new Preset("1d", 24 * 60, Component.text("1 Day")),
        new Preset("3d", 24 * 60 * 3, Component.text("3 Days")),
        new Preset("1w", 24 * 60 * 7, Component.text("1 Week")),
        new Preset("custom", -1, Component.text("Custom"))
    );

    private DurationPresets() {}

    public static @NotNull List<SingleOptionDialogInput.OptionEntry> asOptions(@Nullable String initialId) {
        List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
        for (var p : PRESETS) {
            Component display = p.display().color(TextColor.color(224, 167, 34));
            boolean initial = p.id().equalsIgnoreCase(initialId);
            entries.add(SingleOptionDialogInput.OptionEntry.create(p.id(), display, initial));
        }
        return entries;
    }

    public static OptionalInt minutesFor(@NotNull String id) {
        return PRESETS.stream()
                .filter(p -> p.id.equalsIgnoreCase(id))
                .mapToInt(p -> p.minutes)
                .filter(v -> v >= 0)
                .findFirst();
    }

    public static String bestMatchId(int minutes) {
        return PRESETS.stream()
                .filter(p -> p.minutes == minutes)
                .map(p -> p.id)
                .findFirst()
                .orElse("custom");
    }

    public static boolean isCustom(String preset) {
        return preset.equalsIgnoreCase("custom");
    }

}
