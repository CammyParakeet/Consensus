package com.glance.consensus.platform.paper.polls.builder.dialog;

import com.glance.consensus.platform.paper.polls.builder.PollBuildNavigator;
import com.glance.consensus.platform.paper.polls.builder.PollBuildScreen;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import com.glance.consensus.platform.paper.polls.builder.PollBuilderSessions;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
@AutoService(PollBuildScreen.class)
public final class OverrideScreen implements PollBuildScreen {

    private final PollBuildNavigator navigator;

    @Inject
    public OverrideScreen(
        @NotNull final PollBuildNavigator navigator
    ) {
        this.navigator = navigator;
    }

    @Override
    public PollBuildSession.Stage stage() {
        return PollBuildSession.Stage.OVERRIDE;
    }

    private ItemStack getOverrideIcon() {
        ItemStack icon = ItemStack.of(Material.BARRIER);
        icon.editMeta(meta -> {
           meta.displayName(Component.empty());
        });

        return icon;
    }

    @Override
    public void open(
        @NotNull Player player,
        @NotNull PollBuildSession session
    ) {
        var dialog = Dialog.create(b -> b.empty()
                // todo config for some of these specifics?
            .base(DialogBase.builder(Component.text("Poll Builder"))
                .body(List.of(
                        DialogBody.item(getOverrideIcon()).build(),
                        DialogBody.plainMessage(
                            Component.text("Looks like you have an unfinished poll!")
                                .color(NamedTextColor.DARK_AQUA)),
                        DialogBody.plainMessage(
                            Component.text("Continue where you left off, or begin a new one?"))
                ))
                .build())
            .type(DialogType.confirmation(
                ActionButton.create(
                        Component.text("Resume"),
                        Component.text("Click to resume your current Poll Builder"),
                        180,
                        DialogAction.customClick((view, audience) -> {
                                    audience.closeDialog();
                                    this.navigator.open(player, PollBuildSession.Stage.GENERAL);
                                },
                                ClickCallback.Options.builder()
                                        .uses(1)
                                        .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                        .build()
                        )
                ),
                ActionButton.create(
                    Component.text("Start New"),
                    Component.text("This will override your existing Poll Builder"),
                    180,
                    DialogAction.customClick((view, audience) -> {
                        audience.closeDialog();
                        this.navigator.clear(player.getUniqueId());
                        this.navigator.open(player, PollBuildSession.Stage.GENERAL);
                    },
                    ClickCallback.Options.builder()
                        .uses(1)
                        .lifetime(ClickCallback.DEFAULT_LIFETIME)
                        .build()
                    )
                )
            ))
        );

        player.showDialog(dialog);
    }

}
