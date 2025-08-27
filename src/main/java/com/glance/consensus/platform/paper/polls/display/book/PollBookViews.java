package com.glance.consensus.platform.paper.polls.display.book;

import com.glance.consensus.platform.paper.polls.display.book.builder.BookBuilder;
import com.glance.consensus.platform.paper.polls.display.book.builder.ClickMode;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.utils.Mini;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PollBookViews {

    public ItemStack buildVotingBook(
        @NotNull Player viewer,
        @NotNull PollRuntime runtime,
        @NotNull ClickMode mode
    ) {
        var poll = runtime.getPoll();

        var builder = new BookBuilder()
            .setTitle(Mini.parseMini("Poll: " + poll.getQuestionRaw()))
            .setAuthor(Mini.parseMini(poll.getOwner().toString()));

        List<Component> page = new ArrayList<>();

    }


}
