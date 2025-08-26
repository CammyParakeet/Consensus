package com.glance.consensus.platform.paper.polls.builder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PollBuildScreen {
    PollBuildSession.Stage stage();
    void open(@NotNull Player player, @NotNull PollBuildSession session);

    default Component buildFormattingHelp() {
        String raw = "<gold><b>Yes</b></gold> <gray>- select me!</gray>";
        Component rendered = MiniMessage.miniMessage().deserialize(raw);

        Component title = Component.text("MiniMessage Formatting", NamedTextColor.AQUA);
        Component basics = Component.text()
                .append(Component.text("• Colors: ", NamedTextColor.GRAY))
                .append(Component.text("<red>, <gold>, <green>, ...\n", NamedTextColor.WHITE))
                .append(Component.text("• Styles: ", NamedTextColor.GRAY))
                .append(Component.text("<b>, <i>, <u>, <obf>, <strikethrough>\n", NamedTextColor.WHITE))
                .append(Component.text("• Gradients: ", NamedTextColor.GRAY))
                .append(Component.text("<gradient:#ff9d00:#ff3d00>text</gradient>\n", NamedTextColor.WHITE))
                .append(Component.text("• New line: ", NamedTextColor.GRAY))
                .append(Component.text("\\n\n", NamedTextColor.WHITE))
                .append(Component.text("• Escape '<': ", NamedTextColor.GRAY))
                .append(Component.text("\\<\n", NamedTextColor.WHITE))
                .build();

        Component exampleHeader = Component.text("\nExample:", NamedTextColor.GOLD);
        Component rawHeader = Component.text(" Raw: ", NamedTextColor.GRAY);
        Component rawBlock = Component.text(raw, NamedTextColor.WHITE);
        Component previewHeader = Component.text("\n Preview: ", NamedTextColor.GRAY);

        return Component.text()
                .append(title).appendNewline()
                .append(basics)
                .append(exampleHeader).appendNewline()
                .append(rawHeader).append(rawBlock)
                .append(previewHeader).append(rendered)
                .build();
    }

    static Component parseMini(@NotNull String raw) {
        return MiniMessage.miniMessage().deserialize(raw);
    }
}
