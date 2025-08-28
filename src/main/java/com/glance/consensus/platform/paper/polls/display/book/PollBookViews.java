package com.glance.consensus.platform.paper.polls.display.book;

import com.glance.consensus.platform.paper.polls.display.book.utils.AlignmentUtils;
import com.glance.consensus.platform.paper.polls.display.book.utils.TextAlign;
import com.glance.consensus.platform.paper.polls.display.book.builder.BookBuilder;
import com.glance.consensus.platform.paper.polls.display.book.builder.ClickMode;
import com.glance.consensus.platform.paper.polls.display.book.utils.TruncationUtils;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.utils.ComponentUtils;
import com.glance.consensus.platform.paper.utils.Mini;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PollBookViews {

    private final Component SMALL_GAP = Component.text("-                         -");

    private final int TARGET_OPTIONS = 6;
    private final int LINES_PER_OPTION_MISSING = 2;
    private final int BASE_SPACER = 1;

    public ItemStack buildVotingBook(
        @NotNull Player viewer,
        @NotNull PollRuntime runtime,
        @NotNull ClickMode mode
    ) {
        var poll = runtime.getPoll();

        var builder = new BookBuilder()
            .setTitle(Mini.parseMini("Poll: " + poll.getQuestionRaw()))
            .setAuthor(Mini.parseMini(poll.getOwner().toString()));

        List<Component> page = new ArrayList<>();

        /* Handling Poll Question */

        var questionParsed = center(poll.getQuestionRaw());
        Component questionComp;
        if (questionParsed.truncated()) {
            questionComp = questionParsed.value().hoverEvent(Mini.parseMini(poll.getQuestionRaw()));
        } else {
            questionComp = questionParsed.value();
        }
        page.add(questionComp);
        page.add(Component.empty());

        /* Handling Poll Answers */

        int answerCount = poll.getOptions().size();
        int missing = Math.max(0, TARGET_OPTIONS - answerCount);
        int extraPad = (missing * LINES_PER_OPTION_MISSING) / 2;
        for (int i = 0; i < extraPad; i++) {
            page.add(Component.empty());
        }

        for (var opt : poll.getOptions()) {
            page.add(SMALL_GAP);
            Component tt = opt.tooltipRaw() != null
                ? Mini.parseMini(opt.tooltipRaw())
                : Component.empty();

            var answerParsed = center(opt.labelRaw());
            Component hoverComp;
            if (answerParsed.truncated()) {
                if (!ComponentUtils.isVisuallyEmpty(tt)) tt = Component
                        .text("").appendNewline().append(tt);

                hoverComp = Mini.parseMini(opt.labelRaw()).append(tt);
            } else {
                hoverComp = tt;
            }

            page.add(answerParsed.value().hoverEvent(hoverComp));
        }
        page.add(SMALL_GAP);
        
        builder.addPage(page);
        return builder.itemStack();
    }

    // todo
    public ItemStack buildResultsBook(
        @NotNull Player viewer,
        @NotNull PollRuntime runtime
    ) {
        return null;
    }

    private Component emptyLine() { return Component.newline(); }

    private TruncationUtils.TruncateResult center(@NotNull String raw) {
        return AlignmentUtils.alignAndTruncate(raw, TextAlign.CENTER);
    }
    private TruncationUtils.TruncateResult left(@NotNull String raw) {
        return AlignmentUtils.alignAndTruncate(raw, TextAlign.LEFT);
    }
    private TruncationUtils.TruncateResult right(@NotNull String raw) {
        return AlignmentUtils.alignAndTruncate(raw, TextAlign.RIGHT);
    }

}
