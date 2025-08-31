package com.glance.consensus.platform.paper.commands.impl.debug;

import com.glance.consensus.platform.paper.commands.engine.CommandHandler;
import com.glance.consensus.platform.paper.polls.display.book.PollBookViews;
import com.glance.consensus.platform.paper.polls.runtime.PollManager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.annotations.Command;
import org.jetbrains.annotations.NotNull;

@Singleton
@AutoService(CommandHandler.class)
public class BookDebugCommands implements CommandHandler {

    private final PollManager pollManager;

    @Inject
    public BookDebugCommands(
        @NotNull final PollManager pollManager
    ) {
        this.pollManager = pollManager;
    }

    @Command("c debug hover-book")
    public void hoverBook(
        @NotNull Player player
    ) {
        Component option = Component.text("- Vote YES")
                .hoverEvent(HoverEvent.showText(Component.text("Tooltip for YES")));

        Component option2 = Component.text("- Vote NO")
                .hoverEvent(HoverEvent.showText(Component.text("Tooltip for NO")));

        Component option3 = Component.text("- Vote SAM")
                .hoverEvent(player.asHoverEvent(d -> d.name(Component.text("Test?"))));

        Component option4 = Component.text("- Vote COM")
                .hoverEvent(ItemStack.of(Material.GOLDEN_APPLE).asHoverEvent());

        Component page = Component.text("Poll Options: \n\n")
                .append(option).append(Component.newline())
                .append(option2).append(Component.newline())
                .append(option3).append(Component.newline())
                .append(option4);

        Book book = Book.book(
           Component.text("Poll Test"),
           Component.text("Poll Wizard"),
           page
        );

        player.openBook(book);
    }

    @Command("c debug poll-vote")
    public void pollVoteBook(
        @NotNull Player player
    ) {
        var polls = pollManager.active();
        if (polls == null) return;

        var poll = polls.stream().findFirst();
        if (poll.isEmpty()) {
            player.sendMessage("No active poll");
            return;
        }

        ItemStack book = PollBookViews.buildVotingBook(player, poll.get(), null);
        player.openBook(book);
    }

}
