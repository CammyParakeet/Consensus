package com.glance.consensus.platform.paper.polls.display.book;

import com.glance.consensus.platform.paper.polls.display.book.utils.AlignmentUtils;
import com.glance.consensus.platform.paper.polls.display.book.utils.TextAlign;
import com.glance.consensus.platform.paper.polls.display.book.builder.BookBuilder;
import com.glance.consensus.platform.paper.polls.display.book.utils.TruncationUtils;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.utils.ComponentUtils;
import com.glance.consensus.platform.paper.utils.Mini;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@UtilityClass
public class PollBookViews {

    public ItemStack buildVotingBook(
            @NotNull Player viewer,
            @NotNull PollRuntime runtime,
            @Nullable PollRules effectiveRules
    ) {
        var poll = runtime.getPoll();
        var finalRules = effectiveRules != null ? effectiveRules : poll.getRules();

        var builder = new BookBuilder()
            .setTitle(Mini.parseMini("Poll: " + poll.getQuestionRaw()))
            .setAuthor(Mini.parseMini(poll.getOwner().toString()));

        /* Handling Poll Question */
        List<Component> page = new ArrayList<>(formatPollQuestion(runtime));

        // Gap
        page.add(Component.empty());

        /* Handling Poll Answers */
        page.addAll(formatPollAnswers(viewer, runtime));

        builder.addPage(page);
        return builder.itemStack();
    }

    // TODO: add hover and click to poll info??
    private List<Component> formatPollQuestion(@NotNull PollRuntime runtime) {
        @NotNull Poll poll = runtime.getPoll();
        List<Component> question = new ArrayList<>();

        var questionParsed = center(poll.getQuestionRaw());
        Component questionComp;
        if (questionParsed.truncated()) {
            questionComp = questionParsed.value().hoverEvent(Mini.parseMini(poll.getQuestionRaw()));
        } else {
            questionComp = questionParsed.value();
        }
        question.add(questionComp);

        return question;
    }

    // TODO: add click event to vote!!
    private List<Component> formatPollAnswers(
        @NotNull Player viewer,
        @NotNull PollRuntime runtime
    ) {
        @NotNull Poll poll = runtime.getPoll();
        List<Component> answers = new ArrayList<>();

        // Center if we have missing answers
        int answerCount = poll.getOptions().size();
        int missing = Math.max(0, TARGET_OPTIONS - answerCount);
        int extraPad = (missing * LINES_PER_OPTION_MISSING) / 2;
        for (int i = 0; i < extraPad; i++) {
            answers.add(Component.empty());
        }

        final Set<Integer> viewerVotes = runtime.selectionSnapshot(viewer.getUniqueId());

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

                hoverComp = Mini.parseMini(opt.labelRaw()).append(Component.text("\n\n"), tt);
            } else {
                hoverComp = tt;
            }

            final Component actionHint = buildActionHint(poll, selected);
            final Component fullHoverComp = Component.text()
                    .append(hoverComp, Component.text("\n\n"), actionHint).build();

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

    // todo
    public ItemStack buildResultsBook(
        @NotNull Player viewer,
        @NotNull PollRuntime runtime
    ) {
        return null;
    }

    private @NotNull List<Component> formatResultsPage(@NotNull PollRuntime runtime) {
        final Poll poll = runtime.getPoll();
        final List<PollOption> options = poll.getOptions();

        return List.of();
    }

    private Component emptyLine() { return Component.newline(); }

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
