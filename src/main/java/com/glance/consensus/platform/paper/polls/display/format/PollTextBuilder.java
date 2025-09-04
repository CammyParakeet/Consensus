package com.glance.consensus.platform.paper.polls.display.format;

import com.glance.consensus.platform.paper.polls.display.PollDisplayNavigator;
import com.glance.consensus.platform.paper.polls.display.book.builder.BookUtils;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.utils.RuleUtils;
import com.glance.consensus.platform.paper.utils.ComponentUtils;
import com.glance.consensus.platform.paper.utils.Mini;
import lombok.Data;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

@UtilityClass
public class PollTextBuilder {

    public final int TARGET_OPTIONS = 6;
    public final int LINES_PER_OPTION_MISSING = 2;

    @Data
    public final class Options {
        private final boolean preview;
        private final boolean results;
        private final int targetOptions;
        private final int linesPerOptionMissing;
        private final boolean includeRulesHover;
        private final boolean includeSelectionHover;
        private final boolean addVoteClick;
        private final VoteBadgeUtils.VoteStyle voteStyle;
        private final VoteBadgeUtils.Theme theme;

        public static Options preview() {
            return new Options(
                    true,
                    false,
                    TARGET_OPTIONS,
                    LINES_PER_OPTION_MISSING,
                    true,
                    false,
                    false,
                    VoteBadgeUtils.VoteStyle.CHECKBOX,
                    VoteBadgeUtils.Theme.defaultVote());
        }

        public static Options bookVoting() {
            return new Options(
                    false,
                    false,
                    TARGET_OPTIONS,
                    LINES_PER_OPTION_MISSING,
                    false,
                    true,
                    true,
                    VoteBadgeUtils.VoteStyle.CHECKBOX,
                    VoteBadgeUtils.Theme.defaultVote());
        }

        public static Options bookResults() {
            return new Options(
                    false,
                    true,
                    TARGET_OPTIONS,
                    LINES_PER_OPTION_MISSING,
                    false,
                    false,
                    false,
                    VoteBadgeUtils.VoteStyle.CHECKBOX,
                    VoteBadgeUtils.Theme.defaultResults());
        }
    }

    public List<Component> formatQuestion(
            @NotNull Poll poll,
            @NotNull PollRules finalRules,
            @NotNull Options options
    ) {
        List<Component> out = new ArrayList<>();

        var parsed = center(poll.getQuestionRaw());
        Component line = parsed.value();

        final boolean showHint = options.includeSelectionHover && !options.preview && !options.results;
        final Component selectionHint;
        if (showHint) {
            final boolean multi = finalRules.multipleChoice() && finalRules.maxSelections() > 1;
            final String amountMsg = multi ? "one or multiple answers" : "one answer";
            selectionHint = Mini.parseMini("<gray>Select " + amountMsg + " to vote for</gray>");
        } else {
            selectionHint = Component.empty();
        }

        List<Component> hoverLines = new ArrayList<>(4);

        // If the title was truncated, add the full question on a new line
        if (parsed.truncated()) {
            hoverLines.add(Mini.parseMini(poll.getQuestionRaw()));
        }

        List<Component> activity = activityLore(poll, !hoverLines.isEmpty());

        if (options.preview && !activity.isEmpty()) {
            int keep = (!hoverLines.isEmpty()) ? 2 : 1; // spacer+badge OR badge only
            if (activity.size() > keep) {
                activity = activity.subList(0, keep);
            }
        }
        hoverLines.addAll(activity);

        // Only add the selection hint if present
        if (!ComponentUtils.isVisuallyEmpty(selectionHint)) {
           hoverLines.add(selectionHint);
        }

        Component hover = Component.join(JoinConfiguration.newlines(), hoverLines);
        line = line.hoverEvent(hover);

        out.add(line);
        return out;
    }

    public @NotNull List<Component> activityLore(@NotNull Poll poll, boolean addEmpty) {
        List<Component> out = new ArrayList<>(addEmpty ? 4 : 3);
        if (addEmpty) out.add(Component.empty());

        final String stateLabel = poll.isClosed() ? "Closed" : "Open";
        out.add(Mini.parseMini(
                VoteBadgeUtils.buildBadge(stateLabel, !poll.isClosed(), VoteBadgeUtils.Theme.defaultActivity())
        ));

        final Instant now = Instant.now();
        if (!poll.isClosed()) {
            final Instant closesAt = poll.getClosesAt();
            if (now.isBefore(closesAt)) {
                final Duration remaining = Duration.between(now, closesAt);
                out.add(Mini.parseMini("<gray>Closes in:</gray> <yellow>" + PollTextBuilder.formatDuration(remaining) + "</yellow>"));
            }
        } else {
            final Instant closedAt = poll.getClosesAt();
            final Duration since = Duration.between(closedAt, now).abs();
            out.add(Mini.parseMini("<gray>Closed:</gray> <yellow>" + briefAgo(since) + " ago</yellow>"));
        }

        return out.stream()
                .map(c -> c.decoration(TextDecoration.ITALIC, false))
                .toList();
    }

    public List<Component> formatAnswers(
        @NotNull Poll poll,
        @NotNull Options options,
        @Nullable Player player,
        @Nullable Set<Integer> viewerVotes,
        @Nullable BiConsumer<Audience, Integer> onClick
    ) {
        List<Component> answers = new ArrayList<>();

        if (!options.preview) {
            // Center if we have missing answers
            int answerCount = poll.getOptions().size();
            int missing = Math.max(0, TARGET_OPTIONS - answerCount);
            int extraPad = ((missing * LINES_PER_OPTION_MISSING) / 2);
            for (int i = 0; i < extraPad; i++) answers.add(Component.empty());
        }

        final Set<Integer> voted = (viewerVotes != null) ? viewerVotes : Set.of();

        for (var opt : poll.getOptions()) {
            answers.add(BookUtils.SIDE_DIVIDER);

            final int idx = opt.index();
            final boolean selected = voted.contains(idx);

            final String badge = VoteBadgeUtils.voteBadgeRaw(
                    selected,
                    VoteBadgeUtils.VoteStyle.CHECKBOX,
                    VoteBadgeUtils.Theme.defaultVote());

            var answerParsed = sides(badge, opt.labelRaw());

            Component tooltip = opt.tooltipRaw() != null
                    ? Mini.parseMini(opt.tooltipRaw())
                    : Component.empty();
            boolean hasTooltip = !ComponentUtils.isVisuallyEmpty(tooltip);
            Component fullLabel = Mini.parseMini(opt.labelRaw());

            Component hoverComp;
            if (answerParsed.truncated()) {
                // Always show full label when the displayed label was truncated
                hoverComp = hasTooltip
                        ? Component.text().append(fullLabel, fullNewline(), tooltip).build()
                        : fullLabel;
            } else {
                // Not truncated: only show tooltip if present
                hoverComp = hasTooltip ? tooltip : Component.empty();
            }

            final Component actionHint = options.preview ? Component.empty() : buildActionHint(poll, player, selected);
            final Component fullHoverComp;
            if (ComponentUtils.isVisuallyEmpty(actionHint)) {
                fullHoverComp = hoverComp;
            } else if (ComponentUtils.isVisuallyEmpty(hoverComp)) {
                fullHoverComp = actionHint;
            } else {
                fullHoverComp = Component.text()
                        .append(hoverComp, fullNewline(), actionHint)
                        .build();
            }

            Component line = answerParsed.value().hoverEvent(fullHoverComp);

            if (options.addVoteClick && onClick != null) {
                line = line.clickEvent(ClickEvent.callback((Audience a) -> {
                    try {
                        onClick.accept(a, idx);
                    } catch (Throwable ignored) {}
                }));
            }

            answers.add(line);
        }

        answers.add(BookUtils.SIDE_DIVIDER);

        return answers;
    }

    private static @NotNull String briefAgo(@NotNull Duration d) {
        long s = Math.max(0, d.getSeconds());
        long days = s / 86400; s %= 86400;
        long hrs  = s / 3600;  s %= 3600;
        long mins = s / 60;
        if (days > 0) return days + "d";
        if (hrs  > 0) return hrs  + "h";
        if (mins > 0) return mins + "m";
        return "now";
    }

    private Component buildActionHint(
        @NotNull Poll poll,
        @Nullable Player player,
        boolean selected
    ) {
        var rules = RuleUtils.effectiveRules(player, poll.getRules());
        String msg;
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

        if (rules.canViewResults()) {
            msg += "\n<dark_gray>Turn the page to view results";
        }

        return Mini.parseMini(msg);
    }

    /** Simple duration formatter: 1h 23m (or 12m, 42s) */
    public String formatDuration(Duration d) {
        if (d.isNegative() || d.isZero()) return "now";
        long seconds = d.getSeconds();

        long days = seconds / 86_400; seconds %= 86_400;
        long hours = seconds / 3_600; seconds %= 3_600;
        long minutes = seconds / 60;  seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    private Component fullNewline() {
        return Component.text("\n\n");
    }

    private TruncationUtils.TruncateResult sides(@NotNull String left, @NotNull String right) {
        return  AlignmentUtils.alignSides(left, right);
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
