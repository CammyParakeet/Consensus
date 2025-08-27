package com.glance.consensus.platform.paper.polls.builder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a single UI screen within the poll-building wizard
 * <p>
 * Each screen corresponds to one {@link PollBuildSession.Stage} and is
 * responsible for rendering its dialog and updating the session state
 *
 * @author Cammy
 */
public interface PollBuildScreen {

    /**
     * @return the {@link PollBuildSession.Stage} this screen implements
     */
    PollBuildSession.Stage stage();

    /**
     * Opens this screen's UI for the player
     *
     * @param player  the player to show the UI to
     * @param session the player's build session
     */
    void open(@NotNull Player player, @NotNull PollBuildSession session);

    /**
     * Builds a reference component showing basic MiniMessage formatting info
     *
     * @return a formatted component showing MiniMessage usage
     */
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

}
