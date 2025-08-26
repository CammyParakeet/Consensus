package com.glance.consensus.platform.paper.polls.builder.dialog;

import com.glance.consensus.platform.paper.polls.builder.PollBuildNavigator;
import com.glance.consensus.platform.paper.polls.builder.PollBuildScreen;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import com.glance.consensus.platform.paper.polls.builder.PollBuilderSessions;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
@AutoService(PollBuildScreen.class)
public final class CreateAnswerScreen implements PollBuildScreen {

    private final PollBuildNavigator navigator;
    private final PollBuilderSessions sessions;

    private static final String K_ANSWER = "poll_answer";
    private static final String K_HOVER_TEXT = "answer_hover_text";

    @Inject
    public CreateAnswerScreen(
        @NotNull final PollBuildNavigator navigator,
        @NotNull final PollBuilderSessions sessions
    ) {
        this.sessions = sessions;
        this.navigator = navigator;
    }

    @Override
    public PollBuildSession.Stage stage() {
        return PollBuildSession.Stage.OPTIONS;
    }

    @Override
    public void open(@NotNull Player player, @NotNull PollBuildSession session) {
        var dialog = Dialog.create(b -> b.empty()
            .base(DialogBase.builder(Component.text("Poll Builder (Answer Editor)"))
                .body(List.of(
                    DialogBody.item(ItemStack.of(Material.PAPER)).build(),
                    DialogBody.plainMessage(Component.text("Configure this poll answer!")),
                    DialogBody.plainMessage(Component.text("About Formatting?",
                                    TextColor.color(219, 219, 219))
                        .hoverEvent(HoverEvent.showText(buildFormattingHelp()))),
                    DialogBody.plainMessage(Component.empty())
                ))
                .inputs(getInputs(session))
                .build())
            .type(DialogType.confirmation(
                ActionButton.create(
                    Component.text("Confirm"),
                    Component.text("This will add the configured answer to the poll"),
                    120,
                    DialogAction.customClick((v, a) -> {
                        if (!(a instanceof Player p)) return;

                        String answer = v.getText(K_ANSWER);
                        String tooltip = v.getText(K_HOVER_TEXT);
                        if (tooltip == null) tooltip = "";

                        if (answer == null || answer.isBlank()) {
                            p.sendMessage(Component.text("Answer Cannot Be Empty!", NamedTextColor.RED));
                            return;
                        }

                        // todo index stuff

                        session.upsertOption(session.getEditingIndex(), answer, tooltip);
                        this.navigator.open(p, PollBuildSession.Stage.GENERAL);
                    }, ClickCallback.Options.builder()
                            .uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                ),
                ActionButton.create(
                    Component.text("Discard"),
                    Component.text("This discards the current answer"),
                    120,
                        DialogAction.customClick((v, a) -> {
                            if (a instanceof Player p) this.navigator.open(p, PollBuildSession.Stage.GENERAL);
                        }, ClickCallback.Options.builder()
                                .uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                )
            ))
        );

        player.showDialog(dialog);
    }

    private List<DialogInput> getInputs(@NotNull PollBuildSession session) {
        String currentLabel = "";
        String currentTooltip = "";

        int editingIndex = session.getEditingIndex();
        if (editingIndex < session.optionCount()) {
            var option = session.getOptions().get(session.getEditingIndex());
            currentLabel = option.labelRaw();
            currentTooltip = option.tooltipRaw();
        }

        return List.of(
            DialogInput.text(K_ANSWER, Component.text("Answer")
                    .hoverEvent(HoverEvent.showText(Component.text("Supports MiniMessage Tags"))))
                .initial(currentLabel)
                .labelVisible(true)
                .width(300)
                .maxLength(96)
                .build(),

            DialogInput.text(K_HOVER_TEXT, Component.text("Answer Tooltip"))
                .initial(currentTooltip)
                .labelVisible(true)
                .width(300)
                .maxLength(512)
                .multiline(TextDialogInput.MultilineOptions.create(6, 100))
                .build()
        );
    }

}
