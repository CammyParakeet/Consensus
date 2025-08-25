package com.glance.consensus.platform.paper.commands.impl.debug;

import com.glance.consensus.platform.paper.commands.engine.CommandHandler;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import com.glance.consensus.platform.paper.polls.builder.DefaultPollBuilderNavigator;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.annotations.Command;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
@AutoService(CommandHandler.class)
public class DialogDebugCommands implements CommandHandler {

    private final DefaultPollBuilderNavigator navigator;

    @Inject
    public DialogDebugCommands(@NotNull DefaultPollBuilderNavigator navigator) {
        this.navigator = navigator;
    }

    @Command("c open dialog")
    public void openDialog(
        @NotNull Player player
    ) {
        this.navigator.open(player, PollBuildSession.Stage.OVERRIDE);
    }

    @Command("c debug dialog")
    public void debugDialog(
        @NotNull Player player
    ) {
        ItemStack coolItem = ItemStack.of(Material.GREEN_CANDLE);
        coolItem.editMeta(meta -> {
            meta.setEnchantmentGlintOverride(true);
            meta.displayName(Component.text("Really cool cundle")
                    .decorate(TextDecoration.BOLD)
                    .decorate(TextDecoration.UNDERLINED)
                    .decoration(TextDecoration.ITALIC, false)
            );
        });

        Dialog dialog = Dialog.create(b -> {
            b.empty()
                    .base(DialogBase
                            .builder(Component.text("Test Dialog"))
                            .inputs(List.of(
                                DialogInput.numberRange("max_answers",
                                    Component.text("Max Allowed Answers"), 1F, 6F)
                                    .step(1F)
                                    .initial(3F)
                                    .width(256)
                                    .build(),
                                DialogInput.bool("is_segg", Component.text("Am I seggsy?"))
                                    .initial(true)
                                    .onFalse("Falsed")
                                    .onTrue("Truthed")
                                    .build()
                            ))
                            .body(List.of(
                                DialogBody
                                    .item(ItemStack.of(Material.GOLDEN_APPLE))
                                    .showDecorations(true)
                                    .width(80)
                                    .description(DialogBody.plainMessage(Component.text("Oink?")))
                                    .build(),
                                    DialogBody
                                            .item(coolItem)
                                            .showDecorations(true)
                                            .width(120)
                                            .description(DialogBody.plainMessage(Component.text("Mmm cundle")))
                                            .build(),
                                    DialogBody
                                        .plainMessage(Component
                                            .text("Some plain message body", NamedTextColor.GOLD))
                            ))
                            .build()
                    )
                    .type(DialogType.multiAction(List.of(
                        ActionButton
                            .create(
                                Component.text("Action 1"),
                                Component.text("This is a tooltip?"),
                                128,
                                DialogAction.staticAction(ClickEvent.callback(a ->
                                        a.sendMessage(Component.text("You smell like ahh"))))
                            )
                    )).build());
        });

        player.showDialog(dialog);
    }

}
