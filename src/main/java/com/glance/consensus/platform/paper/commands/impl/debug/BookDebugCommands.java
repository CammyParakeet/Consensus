package com.glance.consensus.platform.paper.commands.impl.debug;

import com.glance.consensus.platform.paper.commands.engine.CommandHandler;
import com.google.auto.service.AutoService;
import com.google.inject.Singleton;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.annotations.Command;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Singleton
@AutoService(CommandHandler.class)
public class BookDebugCommands implements CommandHandler {

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

}
