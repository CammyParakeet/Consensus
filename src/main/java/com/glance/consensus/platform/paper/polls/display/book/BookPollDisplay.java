package com.glance.consensus.platform.paper.polls.display.book;

import com.glance.consensus.platform.paper.polls.display.PollDisplay;
import com.glance.consensus.platform.paper.polls.display.book.builder.BookBuilder;
import com.glance.consensus.platform.paper.polls.display.format.PollTextFormatter;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.utils.Mini;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
        return builder.itemStack();
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


}
