package com.glance.consensus.platform.paper.polls.display.book.utils;

import com.glance.consensus.platform.paper.utils.ComponentUtils;
import com.glance.consensus.platform.paper.utils.Mini;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class TruncationUtils {

    /* Span aware truncation */

    public TruncateResult truncateMiniMsg(@NotNull String raw, int maxWidthPx, @Nullable String trail) {
        return truncateComponent(Mini.parseMini(raw, false), maxWidthPx, trail);
    }

    public TruncateResult truncateComponent(@NotNull Component component, int maxWidthPx, @Nullable String trail) {
        if (AlignmentUtils.pixelWidth(component) <= maxWidthPx) {
            return new TruncateResult(component, false);
        }

        // If even an ellipsis cannot fit, return just ellipsis in top-level style
        boolean topBold = ComponentUtils.resolveBold(component, false);
        if (trail != null && !trail.isEmpty()) {
            int trailWidth = AlignmentUtils.glyphWidth(trail.charAt(0), topBold);
            if (maxWidthPx < trailWidth) {
                return new TruncateResult(Component.text(trail).style(component.style()), true);
            }
        }

        LineBudget budget = new LineBudget(maxWidthPx);
        State state = new State(); // tracks if we actually truncated
        Component out = truncateNode(component,false, budget, state, trail);

        return new TruncateResult(out, state.truncated);
    }

    /* internals */

    /**
     * Recursively walk node; only add a character if it still leaves room for an ellipsis
     * at the current style (bold-aware). This guarantees ellipsis on any truncation point.
     */
    private Component truncateNode(
            @NotNull Component node,
            boolean inheritedBold,
            @NotNull LineBudget budget,
            @NotNull State state,
            @Nullable String trail
    ) {
        if (!(node instanceof TextComponent tc)) return Component.empty();

        boolean bold = ComponentUtils.resolveBold(node, inheritedBold);
        boolean hasTrail = trail != null;

        String s = tc.content();
        TextComponent.Builder tb = Component.text().style(tc.style());

        int trailWidth = hasTrail ? AlignmentUtils.glyphWidth(trail.charAt(0), bold) : 0;

        for (int i = 0; i < s.length(); i++) {
            int w = AlignmentUtils.glyphWidth(s.charAt(i), bold);
            if (!budget.hasRoom(w + trailWidth)) {
                // not enough space to keep this char and still fit the ellipsis -> truncate here
                if (budget.hasRoom(trailWidth) && !state.trailUsed) {
                    if (hasTrail) {
                        tb.append(Component.text(trail).style(tc.style()));
                        state.trailUsed = true;
                    }
                    budget.consume(trailWidth);
                }
                state.truncated = true;
                return tb.build(); // stop at truncation point; no children
            }
            // accept this char
            tb.append(Component.text(String.valueOf(s.charAt(i))).style(tc.style()));
            budget.consume(w);
        }

        // All text fit; now children
        for (Component child : tc.children()) {
            if (budget.remaining <= 0) break;
            Component built = truncateNode(child, bold, budget, state, trail);
            if (!ComponentUtils.isVisuallyEmpty(built)) tb.append(built);
            if (state.truncated && state.trailUsed) break; // stop after truncation
        }
        return tb.build();
    }

    private static final class LineBudget {
        int remaining;
        LineBudget(int remaining) { this.remaining = Math.max(0, remaining); }
        boolean hasRoom(int width) { return remaining - width >= 0; }
        void consume(int width) { remaining = Math.max(0, remaining - Math.max(0, width)); }
    }

    private static final class State {
        boolean truncated = false;
        boolean trailUsed = false;
    }

    public record TruncateResult(Component value, boolean truncated) {}

}
