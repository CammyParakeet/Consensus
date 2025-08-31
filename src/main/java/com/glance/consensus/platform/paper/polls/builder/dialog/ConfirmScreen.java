package com.glance.consensus.platform.paper.polls.builder.dialog;

import com.glance.consensus.platform.paper.polls.builder.PollBuildNavigator;
import com.glance.consensus.platform.paper.polls.builder.PollBuildScreen;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.runtime.PollManager;
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
public final class ConfirmScreen implements PollBuildScreen {

    private final PollBuildNavigator navigator;
    private final PollManager pollManager;

    @Inject
    public ConfirmScreen(
        @NotNull PollBuildNavigator navigator,
        @NotNull PollManager manager
    ) {
        this.navigator = navigator;
        this.pollManager = manager;
    }

    @Override
    public PollBuildSession.Stage stage() {
        return PollBuildSession.Stage.CONFIRM;
    }

    private ItemStack getConfirmIcon() {
        ItemStack icon = ItemStack.of(Material.LECTERN);
        icon.editMeta(meta -> {
            meta.displayName(Component.text("Review & Submit", NamedTextColor.GOLD));
        });

        return icon;
    }

    @Override
    public void open(
        @NotNull Player player,
        @NotNull PollBuildSession session
    ) {
        // TODO poll build validation?

        @NotNull Poll poll = pollManager.buildFromSession(player, session);
        // todo build preview

        var dialog = Dialog.create(b -> b.empty()
            // todo config for some of these specifics?
            .base(DialogBase.builder(Component.text("Poll Builder (Submit)"))
                .body(List.of(
                    DialogBody.item(getConfirmIcon()).build(),
                    DialogBody.plainMessage(
                            Component.text("Please review your poll, then click Submit.",
                                    NamedTextColor.AQUA)),
                    DialogBody.plainMessage(
                            Component.text("Below is a preview of your poll"))
                ))
                .build())
            .type(DialogType.confirmation(
                ActionButton.create(
                    Component.text("Submit"),
                    Component.text("Click to submit your current Poll Builder"),
                    180,
                    DialogAction.customClick((view, audience) -> {
                        this.pollManager.registerPoll(player, poll);
                    },
                    ClickCallback.Options.builder()
                            .uses(1)
                            .lifetime(ClickCallback.DEFAULT_LIFETIME)
                            .build()
                    )
                ),
                ActionButton.create(
                    Component.text("Exit"),
                    Component.text("This will exit the builder wizard"),
                    180,
                    null
                )
            ))
        );

        player.showDialog(dialog);
    }

}
