package com.glance.consensus.platform.paper.polls.display.format;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class VoteBadgeUtils {

    public enum ResultsMode { PERCENT, COUNT }
    public enum VoteStyle { CHECKBOX, DOT, XMARK }

    public record Theme(
            @NotNull TextColor bracketSelected,
            @NotNull TextColor valueSelected,
            @NotNull TextColor bracketUnselected,
            @NotNull TextColor valueUnselected
    ) {
        public static Theme defaultResults(){
            return new Theme(
                    NamedTextColor.AQUA,
                    NamedTextColor.GREEN,
                    NamedTextColor.DARK_GRAY,
                    NamedTextColor.GRAY
            );
        }
        public static Theme defaultVote() {
            return new Theme(
                    NamedTextColor.GREEN,
                    NamedTextColor.WHITE,
                    NamedTextColor.DARK_GRAY,
                    NamedTextColor.GRAY
            );
        }
    }

    public @NotNull String voteBadgeRaw(
        boolean selected,
        @NotNull VoteStyle style,
        @NotNull Theme theme
    ) {
        final String inner = switch (style) {
            case CHECKBOX -> selected ? "✓" : " ";
            case DOT -> selected ? "•" : "·";
            case XMARK -> selected ? "x" : " ";
        };

        return buildBadge(inner, selected, theme);
    }

    public @NotNull String resultsBadgeRaw(
        int votes,
        int totalVotes,
        @NotNull ResultsMode mode,
        boolean viewerSelected,
        @NotNull Theme theme
    ) {
        final int percent = (totalVotes <= 0) ? 0 : (int) Math.round((votes * 100.0) / (double) totalVotes);
        final String inner = (mode == ResultsMode.PERCENT) ? (percent + "%") : Integer.toString(votes);

        return buildBadge(inner, viewerSelected, theme);
    }

    public @NotNull String buildBadge(
        @NotNull String inner,
        boolean viewerSelected,
        @NotNull Theme theme
    ) {
        TextColor bColor = viewerSelected ? theme.bracketSelected : theme.bracketUnselected;
        TextColor vColor = viewerSelected ? theme.valueSelected : theme.valueUnselected;

        String badge =
                open(bColor) + "[" + close(bColor) +
                        open(vColor) + inner + close(vColor) +
                        open(bColor) + "]" + close(bColor);

        return badge + "<reset>";
    }

    private @NotNull String open(@NotNull TextColor c) {
        return "<" + c.asHexString() + ">";
    }

    private @NotNull String close(@NotNull TextColor c) {
        return "</" + c.asHexString() + ">";
    }

}
