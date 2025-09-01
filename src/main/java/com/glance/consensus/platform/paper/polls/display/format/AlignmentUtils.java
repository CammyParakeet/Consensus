package com.glance.consensus.platform.paper.polls.display.format;

import com.glance.consensus.platform.paper.utils.ComponentUtils;
import com.glance.consensus.platform.paper.utils.Mini;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods for text utils and pixel-width calculations in Minecraft books
 * <p>
 * Minecraft book pages have a limited horizontal width (~114 pixels)
 * This class provides methods to:
 * <ul>
 *   <li>Estimate the pixel width of plain text with optional bold styling</li>
 *   <li>Insert padding spaces to left-, center-, or right-align text</li>
 *   <li>Rebuild Adventure {@link Component}s with applied utils</li>
 * </ul>
 *
 * <p>Supports standard Latin characters, digits, whitespace, and provides a fallback
 * width for unknown or emoji characters</p>
 *
 * @author Cammy
 */
@Slf4j
@UtilityClass
public class AlignmentUtils {

    private final String ELLIPSIS = "…";
    public final int MAX_PIXEL_WIDTH = 110; // approx book line width

    public int pixelWidth(@NotNull Component component) {
        return widthOfComponent(component, false);
    }

    public Component alignMini(
            @NotNull String raw,
            @NotNull TextAlign align
    ) {
        return alignMini(raw, align, MAX_PIXEL_WIDTH);
    }

    public Component alignMini(
        @NotNull String raw,
        @NotNull TextAlign align,
        int maxWidthPx
    ) {
        Component parsed = Mini.parseMini(raw);
        return alignComponent(parsed, align, maxWidthPx);
    }

    public Component alignComponent(
        @NotNull Component component,
        @NotNull TextAlign align,
        int maxWidthPx
    ) {
        int width = pixelWidth(component);
        if (align == TextAlign.LEFT || width >= maxWidthPx) {
            return component;
        }
        int remaining = Math.max(0, maxWidthPx - width);
        int spaces = (align == TextAlign.CENTER) ? (remaining/8) : (remaining/4);
        if (spaces == 0) return component;
        Component pad = Component.text(" ".repeat(spaces));
        return pad.append(component);
    }

    public TruncationUtils.TruncateResult alignAndTruncate(
        @NotNull String rawMiniMsg,
        @NotNull TextAlign align
    ) {
        TruncationUtils.TruncateResult result = TruncationUtils
                .truncateMiniMsg(rawMiniMsg, MAX_PIXEL_WIDTH, ELLIPSIS);
        Component c = result.value();
        Component aligned = alignComponent(c, align, MAX_PIXEL_WIDTH);

        return new TruncationUtils.TruncateResult(aligned, result.truncated());
    }

    public TruncationUtils.TruncateResult alignSides(
        @NotNull String leftRaw,
        @NotNull String rightRaw
    ) {
        final Component left = Mini.parseMini(leftRaw);
        final Component right = Mini.parseMini(rightRaw);

        final int spacePx = glyphWidth(' ', false);
        final int leftPx = pixelWidth(left);
        final int rightPx = pixelWidth(right);

        final int minTotalPx = leftPx + spacePx + rightPx;

        if (minTotalPx > MAX_PIXEL_WIDTH) {
            return alignAndTruncate(leftRaw + " " + rightRaw, TextAlign.LEFT);
        }

        int remainingPx = MAX_PIXEL_WIDTH - (leftPx + rightPx);
        int gapSpaces = Math.max(1, remainingPx / spacePx);

        Component gap = Component.text(" ".repeat(gapSpaces));
        Component combined = Component.empty().append(left).append(gap).append(right);

        return new TruncationUtils.TruncateResult(combined, false);
    }

    public int widthOfComponent(@NotNull Component component, boolean inheritedBold) {
        boolean bold = ComponentUtils.resolveBold(component, inheritedBold);
        int sum = 0;
        if (component instanceof TextComponent tc) {
            String s = tc.content();
            sum += widthOfString(s, bold);
        }
        for (Component child : component.children()) {
            int got = widthOfComponent(child, bold);
            sum += got;
        }

        return sum;
    }

    public int widthOfString(String s, boolean bold) {
        int sum = 0;
        for (int i = 0; i < s.length(); i++) sum += glyphWidth(s.charAt(i), bold);
        return sum;
    }

    // width table for character widths
    public int glyphWidth(char c, boolean bold) {
        int baseWidth;
        switch (c) {
            case ' ':
            case '\t', 'Q': baseWidth = 4; break;
            case '!': case '|': baseWidth = 2; break;
            case 'l': case 'i': case 't': case 'I': case '1': case '?': baseWidth = 5; break;
            case 'W': case 'M': case 'O': baseWidth = 7; break;
            default:
                if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) baseWidth = 6;
                else baseWidth = 4; // emoji/fallback
        }
        return bold ? baseWidth + 1 : baseWidth;
    }

}
