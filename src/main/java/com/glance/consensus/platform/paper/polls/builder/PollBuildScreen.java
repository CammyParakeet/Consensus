package com.glance.consensus.platform.paper.polls.builder;

import com.glance.consensus.platform.paper.polls.display.format.AlignmentUtils;
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
        Component bookTips = Component.text()
            .append(Component.text("Tips for book pages", NamedTextColor.GOLD)).appendNewline()
            .append(Component.text("• Width limit: ", NamedTextColor.GRAY))
            .append(Component.text(AlignmentUtils.MAX_PIXEL_WIDTH + "px", NamedTextColor.AQUA))
            .append(Component.text(" per line; longer text truncates.", NamedTextColor.GRAY)).appendNewline()

            .append(Component.text("• Bold/gradients are wider: ", NamedTextColor.GRAY))
            .append(Component.text("+1px per bold glyph;\n gradients add markup width.",
                    NamedTextColor.WHITE)).appendNewline()

            .append(Component.text("• Alignment fallback: ", NamedTextColor.GRAY))
            .append(Component.text("two-sided alignment (badge + label)\n falls back if line would truncate.",
                    NamedTextColor.WHITE)).appendNewline()

            .append(Component.text("• Full text: ", NamedTextColor.GRAY))
            .append(Component.text("hover truncated lines to see the complete label.",
                    NamedTextColor.WHITE)).appendNewline()

            .append(Component.text("• Color contrast: ", NamedTextColor.GRAY))
            .append(Component.text("parchment background is light, prefer high contrast\n" +
                            " (e.g, gold/white over gray).",
                    NamedTextColor.WHITE)).appendNewline()

            .append(Component.text("• Newlines & escapes: ", NamedTextColor.GRAY))
            .append(Component.text("\\n for new line, \\< to show a literal '<'.",
                    NamedTextColor.WHITE)).appendNewline()

            .append(Component.text("• Reset styles: ", NamedTextColor.GRAY))
            .append(Component.text("use <reset> after colored badges to avoid color bleed.",
                    NamedTextColor.WHITE)).appendNewline()

            .append(Component.text("• Keep prefixes short: ", NamedTextColor.GRAY))
            .append(Component.text("shorter badges fit better;\n hover shows the full stats.",
                    NamedTextColor.WHITE)).appendNewline()
            .build();

        String raw = "<gold><b>Yes</b></gold> <gray>- select me!</gray>";
        Component rendered = MiniMessage.miniMessage().deserialize(raw);

        Component mmFormatTitle = Component.text("MiniMessage Formatting", NamedTextColor.AQUA);
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
                .append(bookTips).appendNewline()
                .append(mmFormatTitle).appendNewline()
                .append(basics)
                .append(exampleHeader).appendNewline()
                .append(rawHeader).append(rawBlock)
                .append(previewHeader).append(rendered)
                .build();
    }

    default Component emptyLine() {
        return Component.text("");
    }

}
