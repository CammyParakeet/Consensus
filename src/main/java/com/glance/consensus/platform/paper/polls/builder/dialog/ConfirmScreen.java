package com.glance.consensus.platform.paper.polls.builder.dialog;

import com.glance.consensus.platform.paper.polls.builder.PollBuildNavigator;
import com.glance.consensus.platform.paper.polls.builder.PollBuildScreen;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import com.glance.consensus.platform.paper.polls.display.book.builder.BookUtils;
import com.glance.consensus.platform.paper.polls.display.format.AlignmentUtils;
import com.glance.consensus.platform.paper.polls.display.format.PollTextFormatter;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.runtime.PollManager;
import com.glance.consensus.platform.paper.utils.Mini;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
        icon.editMeta(meta ->
            meta.displayName(Component.text("Review & Submit", NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false)));

        return icon;
    }

    @Override
    public void open(
        @NotNull Player player,
        @NotNull PollBuildSession session
    ) {
        final Poll poll;

        try {
            poll = pollManager.buildFromSession(player, session);
        } catch (Exception e) {
            showInvalidDialog(player, session);
            return;
        }

        PollRules rules = poll.getRules();
        var formatOpt = PollTextFormatter.Options.preview();

        List<Component> preview = new ArrayList<>();
        preview.add(emptyLine());
        preview.addAll(PollTextFormatter.formatQuestion(poll, rules, formatOpt));
        preview.add(emptyLine());
        preview.addAll(PollTextFormatter.formatAnswers(poll, formatOpt, Set.of()));
        preview.add(emptyLine());

        List<DialogBody> previewBodies = new ArrayList<>();
        previewBodies.add(DialogBody.item(getConfirmIcon()).build());
        previewBodies.add(DialogBody.plainMessage(
                Component.text("Please review your poll, then click Submit.",
                        NamedTextColor.AQUA).hoverEvent(buildFormattingHelp()), 300));
        previewBodies.add(DialogBody.plainMessage(emptyLine()));
        previewBodies.add(DialogBody.plainMessage(BookUtils.DIVIDER));

        for (Component c : preview) {
            previewBodies.add(DialogBody.plainMessage(c));
        }
        previewBodies.add(DialogBody.plainMessage(Mini.parseMini("<u><gray>Poll Rules")
                .hoverEvent(buildRulesHover(rules))));
        previewBodies.add(DialogBody.plainMessage(emptyLine()));
        previewBodies.add(DialogBody.plainMessage(BookUtils.DIVIDER));
        previewBodies.add(DialogBody.plainMessage(emptyLine()));
        previewBodies.add(DialogBody.plainMessage(emptyLine()));

        Dialog dialog = Dialog.create(b -> b.empty()
            // todo config for some of these specifics?
            .base(DialogBase.builder(Component.text("Poll Builder (Submit)"))
                .body(previewBodies)
                .build())
            .type(DialogType.multiAction(List.of(
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
                        Component.text("Go Back"),
                        Component.text("Return to the builder wizard"),
                        180,
                        DialogAction.customClick((v, a) -> {
                            this.navigator.open(player, PollBuildSession.Stage.GENERAL);
                        }, ClickCallback.Options.builder().build())
                    )
                ))
                .exitAction(
                    ActionButton.create(
                        Component.text("Exit"),
                        Component.text("This will exit the builder wizard"),
                        180,
                        null
                ))
                .build()
            )
        );

        player.showDialog(dialog);
    }

    private Component buildRulesHover(@NotNull PollRules r) {
        return Component.text()
                .append(Component.text("Multiple choice: ", NamedTextColor.GRAY))
                .append(Component.text(r.multipleChoice() ? "Yes" : "No",
                        r.multipleChoice() ? NamedTextColor.GREEN : NamedTextColor.RED))
                .appendNewline()
                .append(Component.text("Max selections: ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(r.maxSelections()), NamedTextColor.AQUA))
                .appendNewline()
                .append(Component.text("Show results: ", NamedTextColor.GRAY))
                .append(Component.text(r.canViewResults() ? "Yes" : "No",
                        r.canViewResults() ? NamedTextColor.GREEN : NamedTextColor.RED))
                .appendNewline()
                .append(Component.text("Allow resubmission: ", NamedTextColor.GRAY))
                .append(Component.text(r.allowResubmissions() ? "Yes" : "No",
                        r.allowResubmissions() ? NamedTextColor.GREEN : NamedTextColor.RED))
                .build();
    }

    /* Invalid Screen */

    private void showInvalidDialog(
        @NotNull Player player,
        @NotNull PollBuildSession session
    ) {
        ValidationSummary summary = validate(session);
        List<DialogBody> body = buildInvalidBody(session, summary);

        var dialog = Dialog.create(b -> b.empty()
            .base(DialogBase.builder(Component.text("Poll Builder (Issues Found)"))
                .body(body)
                .build())
            .type(DialogType.confirmation(
                ActionButton.create(
                    Component.text("Go Back"),
                    Component.text("Return to the builder wizard to fix any issues or make changes"),
                    180,
                    DialogAction.customClick((v, a) -> {
                        this.navigator.open(player, PollBuildSession.Stage.GENERAL);
                    }, ClickCallback.Options.builder().build())
                ),
                ActionButton.create(
                    Component.text("Discard"),
                    Component.text("Abandon this draft and exit the builder"),
                    180,
                    null
                )
            ))
        );

        player.showDialog(dialog);
    }

    private List<DialogBody> buildInvalidBody(
        @NotNull PollBuildSession session,
        @NotNull ValidationSummary summary
    ) {
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(
                Component.text("Found problem(s) with your poll:", NamedTextColor.RED)));
        body.add(DialogBody.plainMessage(Component.empty()));

        if (summary.missingQuestion) {
            body.add(DialogBody.plainMessage(Component.text("• The question is empty.", NamedTextColor.GRAY)));
        }

        if (summary.notEnoughQuestions) {
            body.add(DialogBody.plainMessage(Component.text("• You need at least 2 options.", NamedTextColor.GRAY)));
        }

        if (summary.multiMaxTooHigh) {
            body.add(DialogBody.plainMessage(Component.text(
                    "• Max selections exceeds the number of options ("
                            + session.getMaxSelections() + " > " + summary.optionCount + ").",
                    NamedTextColor.GRAY)));
        }

        body.add(DialogBody.plainMessage(Component.empty()));
        body.add(DialogBody.plainMessage(Component.text("Use ‘Go Back’ to correct these issues!", NamedTextColor.AQUA)));

        return body;
    }

    private ValidationSummary validate(@NotNull PollBuildSession session) {
        ValidationSummary summary = new ValidationSummary();

        String q = Objects.requireNonNullElse(session.getQuestionRaw(), "").trim();
        summary.missingQuestion = q.isBlank();

        List<PollOption> options = session.getAnswers();
        summary.optionCount = options.size();
        summary.notEnoughQuestions = summary.optionCount < 2;

        int maxSel = session.getMaxSelections();
        summary.multiMaxTooHigh = session.isMultipleChoice() && maxSel > summary.optionCount;

        return summary;
    }

    @Getter
    @Setter
    private static final class ValidationSummary {
        boolean missingQuestion;
        boolean notEnoughQuestions;
        boolean multiMaxTooHigh;
        int optionCount;
    }

}
