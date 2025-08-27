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
public final class AnswerScreen implements PollBuildScreen {

    private final PollBuildNavigator navigator;

    private static final String K_ANSWER = "poll_answer";
    private static final String K_HOVER_TEXT = "answer_hover_text";

    @Inject
    public AnswerScreen(
        @NotNull final PollBuildNavigator navigator
    ) {
        this.navigator = navigator;
    }

    @Override
    public PollBuildSession.Stage stage() {
        return PollBuildSession.Stage.ANSWER;
    }

    @Override
    public void open(@NotNull Player player, @NotNull PollBuildSession session) {
        boolean editingExistingAnswer = session.getEditingIndex() < session.answerCount();

        DialogType type = editingExistingAnswer
            ? DialogType.multiAction(List.of(
                    getAdjustUpButton(session),
                    getAdjustDownButton(session),
                    getRemoveButton(session),
                    getDiscardButton(session)
                ))
                .columns(2)
                .exitAction(getConfirmButton(session))
                .build()
            : DialogType.confirmation(getConfirmButton(session), getDiscardButton(session));

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
                .build()
            )
            .type(type)
        );

        player.showDialog(dialog);
    }

    private ActionButton getAdjustUpButton(@NotNull PollBuildSession session) {
        boolean atFirstPos = session.getEditingIndex() == 0;
        String usageMsgUp = atFirstPos
                ? "This answer cannot move any higher in the list"
                : "This will raise the answer up one place in the list";

        return ActionButton.create(
            Component.text("Move Up"),
            Component.text(usageMsgUp),
            120,
                DialogAction.customClick((v, a) -> {
                    if (!(a instanceof Player p)) return;

                    if (!atFirstPos) session.moveEditingUp();
                    this.navigator.open(p, PollBuildSession.Stage.GENERAL);
                }, ClickCallback.Options.builder()
                        .uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build()));
    }

    private ActionButton getAdjustDownButton(@NotNull PollBuildSession session) {
        boolean atLastPos = session.getEditingIndex() == session.answerCount() - 1;
        String usageMsgDown = atLastPos
                ? "This answer cannot move any lower in the list"
                : "This will lower the answer down one place in the list";

        return ActionButton.create(
                Component.text("Move Down"),
                Component.text(usageMsgDown),
                120,
                DialogAction.customClick((v, a) -> {
                    if (!(a instanceof Player p)) return;

                    if (!atLastPos) session.moveEditingDown();
                    this.navigator.open(p, PollBuildSession.Stage.GENERAL);
                }, ClickCallback.Options.builder()
                        .uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
        );
    }

    private ActionButton getConfirmButton(@NotNull PollBuildSession session) {
        boolean isEditing = session.getEditingIndex() < session.answerCount();
        String info = isEditing
                ? "Update this answer to the poll"
                : "Save this answer to the poll";

        return ActionButton.create(
            Component.text("Confirm"),
            Component.text(info),
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

                session.upsertAnswer(session.getEditingIndex(), answer, tooltip);
                this.navigator.open(p, PollBuildSession.Stage.GENERAL);
            }, ClickCallback.Options.builder()
                    .uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
        );
    }

    private ActionButton getDiscardButton(@NotNull PollBuildSession session) {
        boolean isEditing = session.getEditingIndex() < session.answerCount();

        String msg = isEditing ? "Cancel" : "Discard";
        String info = isEditing
            ? "Cancel without saving this answers changes"
            : "Cancel without saving this answer";

        return ActionButton.create(
            Component.text(msg),
            Component.text(info),
            120,
            DialogAction.customClick((v, a) -> {
                if (a instanceof Player p) this.navigator.open(p, PollBuildSession.Stage.GENERAL);
            }, ClickCallback.Options.builder()
                    .uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
        );
    }

    private ActionButton getRemoveButton(@NotNull PollBuildSession session) {
        return ActionButton.create(
                Component.text("Remove"),
                Component.text("This will delete the answer from the poll"),
                120,
                DialogAction.customClick((v, a) -> {
                    if (!(a instanceof  Player p)) return;
                    session.removeAnswer(session.getEditingIndex());
                    this.navigator.open(p, PollBuildSession.Stage.GENERAL);
                }, ClickCallback.Options.builder()
                        .uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
        );
    }

    private List<DialogInput> getInputs(@NotNull PollBuildSession session) {
        String currentLabel = "";
        String currentTooltip = "";

        int editingIndex = session.getEditingIndex();
        if (editingIndex < session.answerCount()) {
            var option = session.getAnswers().get(session.getEditingIndex());
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
                .initial(currentTooltip != null ? currentTooltip : "")
                .labelVisible(true)
                .width(300)
                .maxLength(512)
                .multiline(TextDialogInput.MultilineOptions.create(6, 100))
                .build()
        );
    }

}
