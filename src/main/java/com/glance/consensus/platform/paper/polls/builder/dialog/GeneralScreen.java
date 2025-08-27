package com.glance.consensus.platform.paper.polls.builder.dialog;

import com.glance.consensus.platform.paper.polls.builder.PollBuildNavigator;
import com.glance.consensus.platform.paper.polls.builder.PollBuildScreen;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import com.glance.consensus.platform.paper.polls.runtime.PollManager;
import com.glance.consensus.platform.paper.utils.Mini;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
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

import java.util.ArrayList;
import java.util.List;

@Singleton
@AutoService(PollBuildScreen.class)
public final class GeneralScreen implements PollBuildScreen {

    private final PollBuildNavigator navigator;
    private final PollManager pollManager;

    private static final String K_QUESTION = "poll_question";
    private static final String K_PRESET_DURATION = "duration_preset";
    private static final String K_CUSTOM_HOURS = "duration_custom_hours";
    private static final String K_CUSTOM_MINUTES = "duration_custom_minutes";

    private static final String K_MULTI_CHOICE = "multi_choice";
    private static final String K_MAX_SELECTIONS = "max_selections";
    private static final String K_ALLOW_RESUBMIT = "allow_resubmit";

    private static final String K_BUMP_DELTA = "delta";

    private static final Component OPTION_EDIT_SUFFIX = Mini.parseMini(
            "<gold>[</gold><#0d9fbd>Click to edit this answer<gold>]");

    @Inject
    public GeneralScreen(
        @NotNull final PollBuildNavigator navigator,
        @NotNull final PollManager pollManager
    ) {
       this.navigator = navigator;
       this.pollManager = pollManager;
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
        session.setEditingIndex(session.optionCount());

        var dialog = Dialog.create(b -> b.empty()
            .base(DialogBase.builder(Component.text("Poll Builder (General)"))
                .body(List.of(
                    DialogBody.item(ItemStack.of(Material.WRITABLE_BOOK)).build(),
                    DialogBody.plainMessage(Component.text("Lets create a poll!")),
                        DialogBody.plainMessage(Component.text("About Formatting?",
                                        TextColor.color(219, 219, 219))
                                .hoverEvent(HoverEvent.showText(buildFormattingHelp()))),
                    DialogBody.plainMessage(Component.empty())
                ))
                .inputs(this.getInputs(player, session))
                .build()
            )
            .type(DialogType.multiAction(getPollOptionButtons(session))
                .exitAction(ActionButton.create(
                Component.text("Create"),
                Component.text("Click to submit this poll!"),
                120,
                DialogAction.customClick((v, a) -> {
                    if (!(a instanceof Player p)) return;
                    updateSession(session, v, p);
                }, ClickCallback.Options.builder()
                        .uses(1)
                        .lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                ))
                .columns(1)
                .build()
            )
        );

        player.showDialog(dialog);
    }

    private void updateSession(
        @NotNull PollBuildSession session,
        @NotNull DialogResponseView view,
        @NotNull Player player
    ) {
        String question = view.getText(K_QUESTION);
        if (question == null || question.isBlank()) {
            player.sendMessage(Component.text("Poll Question Cannot Be Empty!", NamedTextColor.RED));
            this.navigator.open(player, PollBuildSession.Stage.GENERAL);
            return;
        }

        // Duration
        String presetId = view.getText(K_PRESET_DURATION);
        if (presetId == null || DurationPresets.isCustom(presetId)) {
            Float rawHrs = view.getFloat(K_CUSTOM_HOURS);
            Float rawMins = view.getFloat(K_CUSTOM_MINUTES);
            int hours = (int) Math.floor(rawHrs != null ? rawHrs : 0F);
            int mins = (int) Math.floor(rawMins != null ? rawMins : 0F);

            session.setCustomHours(hours);
            session.setCustomMins(mins);
        } else {
            session.setDurationPresetId(presetId);
        }

        // Answer flags
        Boolean multipleRaw = view.getBoolean(K_MULTI_CHOICE);
        boolean multiple = multipleRaw != null ? multipleRaw : false;

        int optionCount = Math.max(1, session.optionCount());
        Float maxSelRaw = view.getFloat(K_MAX_SELECTIONS);
        int maxSel = (int) Math.max(1, Math.min(optionCount, (maxSelRaw != null) ? maxSelRaw.intValue() : 1F));
        if (!multiple) maxSel = 1;

        Boolean allowResubmitRaw = view.getBoolean(K_ALLOW_RESUBMIT);
        boolean allowResubmit = allowResubmitRaw != null ? allowResubmitRaw : true;

        // Update session
        session.setQuestionRaw(question);
        session.setMultipleChoice(multiple);
        session.setMaxSelections(maxSel);
        session.setAllowResubmission(allowResubmit);
    }

    private List<ActionButton> getPollOptionButtons(@NotNull PollBuildSession session) {
        List<ActionButton> buttons = new ArrayList<>();

        for (int i = 0; i < session.optionCount(); i++) {
            final int idx = i;
            var opt = session.getOptions().get(i);

            var label = Mini.parseMini(opt.labelRaw());
            Component tooltip;
            if (opt.tooltipRaw() != null && !opt.tooltipRaw().isBlank()) {
                tooltip = Mini.parseMini(opt.tooltipRaw())
                        .appendNewline()
                        .appendNewline()
                        .append(OPTION_EDIT_SUFFIX);
            } else {
                tooltip = OPTION_EDIT_SUFFIX;
            }

            Component display = Component.text("Answer " + (idx + 1) + ": ", NamedTextColor.GRAY)
                    .append(label).color(NamedTextColor.WHITE);

            buttons.add(ActionButton.create(display, tooltip, 256,
                    DialogAction.customClick((v, a) -> {
                        if (!(a instanceof Player p)) return;
                        updateSession(session, v, p);
                        session.setEditingIndex(idx);
                        navigator.open(p, PollBuildSession.Stage.OPTIONS);
                    }, ClickCallback.Options.builder()
                            .uses(1)
                            .lifetime(ClickCallback.DEFAULT_LIFETIME).build())));
        }

        var addNew = ActionButton.create(
                Component.text("Add Poll Answer", TextColor.fromHexString("#a6a6a6")),
                Component.text("Click to add a New Poll Answer!"),
                256,
                DialogAction.customClick((v, a) -> {
                    if (!(a instanceof Player p)) return;
                    updateSession(session, v, p);
                    this.navigator.open(p, PollBuildSession.Stage.OPTIONS);
                }, ClickCallback.Options.builder()
                        .uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
        );

        if (buttons.size() < 6) {
            buttons.add(addNew);
        }

        return buttons;
    }

    private List<DialogInput> getInputs(
            @NotNull Player player,
            @NotNull PollBuildSession session
    ) {
        var durationOptions = DurationPresets.asOptions(
                session.getDurationPresetId()
        );
        boolean isCustom = DurationPresets.isCustom(session.getDurationPresetId());

        return List.of(
            DialogInput.text(K_QUESTION, Component.text("Poll Question to Ask"))
                    .initial(session.getQuestionRaw() != null ? session.getQuestionRaw() : "My new poll")
                    .labelVisible(true)
                    .width(300)
                    .maxLength(48)
                    .build(),

            DialogInput.singleOption(K_PRESET_DURATION,
                Component.text("Poll Duration", TextColor.color(219, 219, 219)), durationOptions)
                    .labelVisible(true)
                    .build(),

            // todo parse hrs and mins from custom mins
            DialogInput.numberRange(K_CUSTOM_HOURS, Component.text("Custom Hours"),
                    0, 168)
                    .initial(isCustom ? session.getCustomHours() : 0F)
                    .step(1F)
                    .build(),

            DialogInput.numberRange(K_CUSTOM_MINUTES, Component.text("Custom Minutes"), 0, 59)
                    .initial(isCustom ? session.getCustomMins() : 0F)
                    .step(1F)
                    .build(),

            DialogInput.bool(K_MULTI_CHOICE, Component.text("Allow Multiple Answers"))
                    .initial(session.isMultipleChoice())
                    .build(),

            DialogInput.numberRange(K_MAX_SELECTIONS, Component.text("Max Answers Per Voter"),
                        1, Math.max(1, session.optionCount()))
                    .initial(1F)
                    .step(1F)
                    .build(),

            DialogInput.bool(K_ALLOW_RESUBMIT,
                            Component.text("Allow Resubmission (single-answer only)"))
                    .initial(session.isAllowResubmission())
                    .build()
        );
    }

}