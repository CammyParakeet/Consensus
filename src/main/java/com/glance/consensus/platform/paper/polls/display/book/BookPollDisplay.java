package com.glance.consensus.platform.paper.polls.display.book;

import com.glance.consensus.platform.paper.polls.display.PollDisplay;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

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
        ItemStack book = PollBookViews.buildVotingBook(player, runtime, rules);

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

}
