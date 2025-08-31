package com.glance.consensus.platform.paper.polls.display.format;

import com.glance.consensus.platform.paper.polls.display.book.utils.AlignmentUtils;
import com.glance.consensus.platform.paper.polls.display.book.utils.TextAlign;
import com.glance.consensus.platform.paper.polls.display.book.utils.TruncationUtils;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.utils.ComponentUtils;
import com.glance.consensus.platform.paper.utils.Mini;
import lombok.Data;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    // TODO: add hover and click to poll info??
    public List<Component> formatQuestion(
            @NotNull Poll poll,
            @NotNull PollRules finalRules,
            @NotNull Options options
    ) {
        List<Component> question = new ArrayList<>();
        Component questionTooltip = Component.empty();

        if (!options.preview) {
            String amountMsg = (finalRules.multipleChoice() && finalRules.maxSelections() > 1) ? "one or multiple" : "one";
            questionTooltip = Mini.parseMini("<dark_gray>Select " + amountMsg + " answers to vote for");
        }

        var questionParsed = center(poll.getQuestionRaw());
        Component questionComp;
        if (questionParsed.truncated()) {
            final Component fullQuestion = Mini.parseMini(poll.getQuestionRaw());
            final Component hover;

            if (ComponentUtils.isVisuallyEmpty(questionTooltip)) {
                hover = fullQuestion;
            } else {
                hover = Component.text().append(fullQuestion, fullNewline(), questionTooltip).build();
            }

            questionComp = questionParsed.value().hoverEvent(hover);
        } else {
            questionComp = questionParsed.value();
        }

        question.add(questionComp);

        return question;
    }

    // TODO: add click event to vote!!
    public List<Component> formatAnswers(
            @NotNull Player viewer,
            @NotNull Poll poll,
            @NotNull PollRules finalRules,
            @NotNull Options options,
            @Nullable Set<Integer> viewerVotes
    ) {
        List<Component> answers = new ArrayList<>();

        // Center if we have missing answers
        int answerCount = poll.getOptions().size();
        int missing = Math.max(0, TARGET_OPTIONS - answerCount);
        int extraPad = (missing * LINES_PER_OPTION_MISSING) / 2;
        for (int i = 0; i < extraPad; i++) {
            answers.add(Component.empty());
        }

        //final Set<Integer> viewerVotes = runtime.selectionSnapshot(viewer.getUniqueId());

        for (var opt : poll.getOptions()) {
            answers.add(SMALL_GAP);

            final int idx = opt.index();
            final boolean selected = viewerVotes.contains(idx);

            final String prefix = selected ? "[✓] " : "[ ] ";
            final String displayRaw = prefix + opt.labelRaw();

            Component tt = opt.tooltipRaw() != null
                    ? Mini.parseMini(opt.tooltipRaw())
                    : Component.empty();

            var answerParsed = center(displayRaw);
            Component hoverComp;
            if (answerParsed.truncated()) {
                if (!ComponentUtils.isVisuallyEmpty(tt)) tt = Component
                        .text("").appendNewline().append(tt);

                hoverComp = Mini.parseMini(opt.labelRaw()).append(fullNewline(), tt);
            } else {
                hoverComp = tt;
            }

            final Component actionHint = buildActionHint(poll, selected);
            final Component fullHoverComp = Component.text()
                    .append(hoverComp, fullNewline(), actionHint).build();

            Component line = answerParsed.value().hoverEvent(fullHoverComp);
            line = line.clickEvent(ClickEvent.callback(a -> {
                if (!(a instanceof Player p)) return;

                p.sendMessage("You voted for " + poll.getPollIdentifier());
                p.performCommand("poll vote " + poll.getPollIdentifier() + " " + idx);
            }));

            answers.add(line);
        }
        answers.add(SMALL_GAP);

        return answers;
    }

    private Component buildActionHint(@NotNull Poll poll, boolean selected) {
        var rules = poll.getRules();
        final String msg;
        if (selected) {
            if (!rules.allowResubmissions()) {
                msg = "<gray>You've voted for this option";
            } else if (rules.multipleChoice()) {
                msg = "<gray>You've voted for this option. Click to unselect";
            } else {
                msg = "<gray>You've voted for this option. Click to change your vote";
            }
        } else {
            msg = rules.multipleChoice()
                    ? "<green>Click to select"
                    : "<green>Click to vote";
        }

        return Mini.parseMini(msg);
    }

    private Component fullNewline() {
        return Component.text("\n\n");
    }

    private TruncationUtils.TruncateResult center(@NotNull String raw) {
        return AlignmentUtils.alignAndTruncate(raw, TextAlign.CENTER);
    }
    private TruncationUtils.TruncateResult left(@NotNull String raw) {
        return AlignmentUtils.alignAndTruncate(raw, TextAlign.LEFT);
    }
    private TruncationUtils.TruncateResult right(@NotNull String raw) {
        return AlignmentUtils.alignAndTruncate(raw, TextAlign.RIGHT);
    }

}
