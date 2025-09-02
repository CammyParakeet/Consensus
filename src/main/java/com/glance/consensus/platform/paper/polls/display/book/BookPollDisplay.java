package com.glance.consensus.platform.paper.polls.display.book;

import com.glance.consensus.platform.paper.polls.display.PollDisplay;
import com.glance.consensus.platform.paper.polls.display.PollDisplayNavigator;
import com.glance.consensus.platform.paper.polls.display.book.builder.BookBuilder;
import com.glance.consensus.platform.paper.polls.display.book.builder.BookUtils;
import com.glance.consensus.platform.paper.polls.display.format.AlignmentUtils;
import com.glance.consensus.platform.paper.polls.display.format.PollTextBuilder;
import com.glance.consensus.platform.paper.polls.display.format.VoteBadgeUtils;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.polls.utils.RuleUtils;
import com.glance.consensus.platform.paper.utils.ComponentUtils;
import com.glance.consensus.platform.paper.utils.Mini;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Slf4j
@Singleton
public final class BookPollDisplay implements PollDisplay {

    private final int MAX_RESULTS_BAR_LEN = 21;

    private final PollDisplayNavigator navigator;

    @Inject
    public BookPollDisplay(
        @NotNull final PollDisplayNavigator navigator
    ) {
        this.navigator = navigator;
    }

    @Override
    public @NotNull Mode mode() {
        return Mode.BOOK;
    }

    @Override
    public void openVoting(
        @NotNull Player player,
        @NotNull PollRuntime runtime,
        @NotNull PollRules rules
    ) {
        refresh(player, runtime, RefreshCause.MANUAL);
    }

    @Override
    public void openResults(@NotNull Player player, @NotNull PollRuntime runtime, @NotNull PollRules ctx) {
        ItemStack book = buildResultsBook(player, runtime);
        player.openBook(book);
    }

    @Override
    public void refresh(@NotNull Player player, @NotNull PollRuntime runtime, @NotNull RefreshCause cause) {
        final PollRules effective = RuleUtils.effectiveRules(player, runtime.getPoll().getRules());

        final boolean resultsOnly = shouldShowResultsOnly(player, runtime, effective);
        final ItemStack book = resultsOnly
                ? buildResultsBook(player, runtime)
                : buildVotingBook(player, runtime, effective);

        player.openBook(book);
    }

    /**
     * Results; only if:
     * <li>poll is closed, OR</li>
     * <li>resubmissions are disabled AND the viewer has already "used up" their votes
     * (single-choice: has any vote; multi-choice: has >= maxSelections)</li>
     */
    private boolean shouldShowResultsOnly(
            @NotNull Player viewer,
            @NotNull PollRuntime runtime,
            @NotNull PollRules rules
    ) {
        final Poll poll = runtime.getPoll();
        if (poll.isClosed()) return true;

        if (!rules.allowResubmissions()) {
            final Set<Integer> sel = runtime.selectionSnapshot(viewer.getUniqueId());
            if (sel.isEmpty()) return false;

            if (!rules.multipleChoice()) {
                // single-choice: any vote consumes your one submission
                return true;
            }

            final int max = Math.max(1, rules.maxSelections());
            // multi-choice: if they've met or exceeded the cap, they’re "done"
            return sel.size() >= max;
        }

        return false;
    }

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
        List<Component> page = new ArrayList<>(PollTextBuilder.formatQuestion(
                poll,
                finalRules,
                PollTextBuilder.Options.bookVoting())
        );

        /* Handling Poll Answers */
        page.addAll(PollTextBuilder.formatAnswers(
            poll,
            PollTextBuilder.Options.bookVoting(),
            viewer,
            runtime.selectionSnapshot(viewer.getUniqueId()),
            (a, index) -> {
                if (!(a instanceof Player p)) return;
                this.navigator.handleVoteClick(p, runtime, index);
            }
        ));

        builder.addPage(page);

        if (finalRules.canViewResults()) {
            builder.addPage(formatResultsPage(viewer, runtime));
        }

        return builder.itemStack();
    }

    public ItemStack buildResultsBook(
            @NotNull Player viewer,
            @NotNull PollRuntime runtime
    ) {
        final var poll = runtime.getPoll();

        var builder = new BookBuilder()
            .setTitle(Mini.parseMini("Results: " + poll.getQuestionRaw()))
            .setAuthor(Mini.parseMini(poll.getOwner().toString()));

        builder.addPage(formatResultsPage(viewer, runtime));
        return builder.itemStack();
    }

    private @NotNull List<Component> formatResultsPage(
        @NotNull Player viewer,
        @NotNull PollRuntime runtime
    ) {
        final Poll poll = runtime.getPoll();
        final PollRules rules = RuleUtils.effectiveRules(viewer, poll.getRules());
        final List<PollOption> options = poll.getOptions();

        final Set<Integer> viewerVotes = runtime.selectionSnapshot(viewer.getUniqueId());

        final int totalVotes = Math.max(0, options.stream().mapToInt(PollOption::votes).sum());

        List<Component> page = new ArrayList<>(PollTextBuilder.formatQuestion(
                poll,
                rules,
                PollTextBuilder.Options.bookResults())
        );

        int answerCount = options.size();
        int missing = Math.max(0, PollTextBuilder.TARGET_OPTIONS - answerCount);
        int extraPad = (missing * PollTextBuilder.LINES_PER_OPTION_MISSING) / 2;
        for (int i = 0; i < extraPad; i++) page.add(Component.empty());

        for (var opt : options) {
            page.add(BookUtils.SIDE_DIVIDER);

            final int votes = opt.votes();
            final boolean selected = viewerVotes.contains(opt.index());

            String badge = VoteBadgeUtils.resultsBadgeRaw(votes, totalVotes,
                    VoteBadgeUtils.ResultsMode.COUNT, selected, VoteBadgeUtils.Theme.defaultResults());

            var aligned = AlignmentUtils.alignSides(badge, opt.labelRaw());

            Component optHover = buildOptionResultsHover(opt, votes, totalVotes, selected);

            // todo truncated situation
            page.add(aligned.value().hoverEvent(optHover));
        }

        page.add(BookUtils.SIDE_DIVIDER);

        return page;
    }

    private @NotNull Component buildOptionResultsHover(
        @NotNull PollOption opt,
        int votes,
        int totalVotes,
        boolean selected
    ) {
        double pct = totalVotes == 0 ? 0.0 : (votes * 100.0) / totalVotes;
        int barLength = (int) Math.round((pct / 100.0) * MAX_RESULTS_BAR_LEN);

        Component tooltip = (opt.tooltipRaw() != null) ? Mini.parseMini(opt.tooltipRaw()) : Component.empty();

        Component stats = Component.text()
                .append(Mini.parseMini("<gray>[</gray>" + barBlock(barLength) + "<gray>]</gray> "))
                .append(Mini.parseMini("<yellow>" + votes + "</yellow>"))
                .append(Component.text(" "))
                .append(Mini.parseMini("<gray>(" + String.format(Locale.ROOT, "%.1f", pct) + "%)</gray>"))
                .appendNewline()
                .append(Mini.parseMini("<gray>Your vote:</gray> " + (selected ? "<green>Yes</green>" : "<red>No</red>")))
                .build();

        var hover = Component.text()
                .append(Mini.parseMini("<white><bold>" + opt.labelRaw() + "</bold></white>"))
                .append(Component.text("\n"));

        if (!ComponentUtils.isVisuallyEmpty(tooltip)) {
            hover.append(Component.text("\n")).append(tooltip).append(Component.text("\n"));
        }

        hover.append(Component.text("\n")).append(stats);

        return hover.build();
    }

    private @NotNull String barBlock(int len) {
        if (len <= 0) return "<dark_gray>·</dark_gray>";
        char[] arr = new char[len];
        Arrays.fill(arr, '█');
        return new String(arr);
    }

}
