package com.glance.consensus.platform.paper.polls.display.format;

import com.glance.consensus.platform.paper.polls.domain.Poll;
import lombok.Data;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@UtilityClass
public class PollTextFormatter {

    private final Component SMALL_GAP = Component.text("-                         -");
    private final int TARGET_OPTIONS = 6;
    private final int LINES_PER_OPTION_MISSING = 2;

    @Data
    public final class Options {
        private final boolean preview;
        private final int targetOptions;
        private final int linesPerOptionMissing;
        private final boolean includeRulesHover;
        private final VoteBadgeUtils.VoteStyle voteStyle;
        private final VoteBadgeUtils.Theme theme;

        public static Options preview() {
            return new Options(
                    true,
                    TARGET_OPTIONS,
                    LINES_PER_OPTION_MISSING,
                    true,
                    VoteBadgeUtils.VoteStyle.CHECKBOX,
                    VoteBadgeUtils.Theme.defaultVote());
        }

        public static Options voting() {
            return new Options(
                    false,
                    TARGET_OPTIONS,
                    LINES_PER_OPTION_MISSING,
                    false,
                    VoteBadgeUtils.VoteStyle.CHECKBOX,
                    VoteBadgeUtils.Theme.defaultVote());
        }
    }

    public List<Component> formatQuestion(@NotNull Poll poll) {
        return List.of();
    }

}
