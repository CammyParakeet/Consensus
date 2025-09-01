package com.glance.consensus.platform.paper.polls.display.book;

import com.glance.consensus.platform.paper.polls.display.PollDisplay;
import com.glance.consensus.platform.paper.polls.display.book.builder.BookBuilder;
import com.glance.consensus.platform.paper.polls.display.book.builder.BookUtils;
import com.glance.consensus.platform.paper.polls.display.format.AlignmentUtils;
import com.glance.consensus.platform.paper.polls.display.format.PollTextFormatter;
import com.glance.consensus.platform.paper.polls.display.format.VoteBadgeUtils;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.polls.utils.RuleUtils;
import com.glance.consensus.platform.paper.utils.ComponentUtils;
import com.glance.consensus.platform.paper.utils.Mini;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Singleton
public final class BookPollDisplay implements PollDisplay {

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
        ItemStack book = buildVotingBook(player, runtime, rules);
        player.openBook(book);

        if (rules.canViewResults()) {
            book.editMeta(m -> {
                BookMeta bm = (BookMeta) m;

            });
        }
    }

    @Override
    public void openResults(@NotNull Player player, @NotNull PollRuntime runtime, @NotNull PollRules ctx) {

    }

    @Override
    public void refresh(@NotNull Player player, @NotNull PollRuntime runtime, @NotNull RefreshCause cause) {

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
        List<Component> page = new ArrayList<>(
                PollTextFormatter.formatQuestion(
                        poll,
                        finalRules,
                        PollTextFormatter.Options.voting()));

        // Gap
        page.add(Component.empty());

        /* Handling Poll Answers */ // todo need effective rules?
        page.addAll(PollTextFormatter.formatAnswers(
                poll,
                PollTextFormatter.Options.voting(),
                runtime.selectionSnapshot(viewer.getUniqueId())));

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

        List<Component> page = new ArrayList<>(PollTextFormatter.formatQuestion(
                poll,
                rules,
                PollTextFormatter.Options.voting()));
        // todo add results option

        int answerCount = options.size();
        int missing = Math.max(0, 6 - answerCount);
        int extraPad= (missing * 2) / 2;
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

        String state = poll.isClosed() ? "<red><bold>Closed</bold></red>" : "<green>Active</green>";
        // todo where to add this? probs question

        return page;
    }

    private @NotNull Component buildOptionResultsHover(
        @NotNull PollOption opt,
        int votes,
        int totalVotes,
        boolean selected
    ) {
        int maxBar = 21;
        double pct = totalVotes == 0 ? 0.0 : (votes * 100.0) / totalVotes;
        int barLength = (int) Math.round((pct / 100.0) * maxBar);

        Component tooltip = (opt.tooltipRaw() != null) ? Mini.parseMini(opt.tooltipRaw()) : Component.empty();

        Component stats = Component.text()
                .append(Mini.parseMini("<white><bold>" + opt.labelRaw() + "</bold></white>")).appendNewline()
                .append(Mini.parseMini("<gray>[</gray>" + barBlock(barLength) + "<gray>]</gray> "))
                .append(Mini.parseMini("<yellow>" + votes + "</yellow>"))
                .append(Component.text(" "))
                .append(Mini.parseMini("<gray>(" + String.format(Locale.ROOT, "%.1f", pct) + "%)</gray>")).appendNewline()
                .append(Mini.parseMini("<gray>Your vote:</gray> " + (selected ? "<green>Yes</green>" : "<red>No</red>")))
                .build();

        if (ComponentUtils.isVisuallyEmpty(tooltip)) return stats;
        return Component.text().append(stats).appendNewline().append(tooltip).build();
    }

    private @NotNull String barBlock(int len) {
        if (len <= 0) return "<dark_gray>·</dark_gray>";
        char[] arr = new char[len];
        Arrays.fill(arr, '█');
        return new String(arr);
    }


}
