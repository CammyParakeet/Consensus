package com.glance.consensus.platform.paper.polls.builder.dialog;

import com.glance.consensus.platform.paper.polls.builder.PollBuildNavigator;
import com.glance.consensus.platform.paper.polls.builder.PollBuildScreen;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
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
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
@AutoService(PollBuildScreen.class)
public final class GeneralScreen implements PollBuildScreen {

    private final PollBuildNavigator navigator;

    private static final String K_QUESTION = "poll_question";
    private static final String K_PRESET_DURATION = "duration_preset";
    private static final String K_CUSTOM_DURATION = "duration_custom";
    private static final String K_BUMP_DELTA = "delta";

    @Inject
    public GeneralScreen(
        @NotNull final PollBuildNavigator navigator
    ) {
       this.navigator = navigator;
    }

    @Override
    public PollBuildSession.Stage stage() {
        return PollBuildSession.Stage.GENERAL;
    }

    @Override
    public void open(
        @NotNull Player player,
        @NotNull PollBuildSession session
    ) {
        var dialog = Dialog.create(b -> b.empty()
            .base(DialogBase.builder(Component.text("Poll Builder (General)"))
                .body(List.of(
                    DialogBody.item(ItemStack.of(Material.WRITABLE_BOOK)).build(),
                    DialogBody.plainMessage(Component.text("Lets create a poll!")),
                    DialogBody.plainMessage(Component.text("Current duration: " + session.getDurationMinutes()))
                ))
                .inputs(this.getInputs(player, session))
                .build())
                // todo split action buttons into methods
            .type(DialogType.multiAction(List.of(
                    ActionButton.create(
                            Component.text("Apply Quick"),
                            Component.text("Use selected quick pick"),
                            160,
                            DialogAction.customClick((v, a) -> {
                                a.sendMessage(Component.text("Pepe"));
                            }, ClickCallback.Options.builder()
                                    .uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                    ),
                    ActionButton.create(
                            Component.text("Apply Custom"),
                            Component.text("Use the custom minute slider"),
                            160,
                            DialogAction.customClick((v, a) -> {
                                a.sendMessage(Component.text("PoPo"));
                            }, ClickCallback.Options.builder()
                                    .uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                    )
            )).build())
        );

        player.showDialog(dialog);
    }

    private List<DialogInput> getInputs(
            @NotNull Player player,
            @NotNull PollBuildSession session
    ) {
        var durationOptions = DurationPresets.asOptions(
            DurationPresets.bestMatchId(session.getDurationMinutes())
        );

        return List.of(
            DialogInput.text(K_QUESTION, Component.text("Poll Question to Ask"))
                    .initial("My new poll")
                    .labelVisible(true)
                    .width(300)
                    .maxLength(48)
                    .build(),

            DialogInput.singleOption(K_PRESET_DURATION,
                Component.text("Poll Duration"), durationOptions)
                    .labelVisible(true)
                    .build(),

            DialogInput.numberRange(K_CUSTOM_DURATION, Component.text("Custom (minutes)"), 5, 10080)
                    .initial(60F)
                    .step(5F)
                    .build()
        );
    }

}