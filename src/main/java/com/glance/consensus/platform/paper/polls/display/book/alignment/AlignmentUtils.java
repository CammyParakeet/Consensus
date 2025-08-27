package com.glance.consensus.platform.paper.polls.display.book.alignment;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods for text alignment and pixel-width calculations in Minecraft books
 * <p>
 * Minecraft book pages have a limited horizontal width (~114 pixels)
 * This class provides methods to:
 * <ul>
 *   <li>Estimate the pixel width of plain text with optional bold styling</li>
 *   <li>Insert padding spaces to left-, center-, or right-align text</li>
 *   <li>Rebuild Adventure {@link Component}s with applied alignment</li>
 * </ul>
 *
 * <p>Supports standard Latin characters, digits, whitespace, and provides a fallback
 * width for unknown or emoji characters</p>
 *
 * @author Cammy
 */
@UtilityClass
public class AlignmentUtils {

    private final int MAX_PIXEL_WIDTH = 114; // approx book line width

    public int calculateTextWidth(@NotNull String text, boolean bold) {
        int sum = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int baseWidth;
            switch (c) {
                case ' ':
                case '\t': baseWidth = 4; break;
                case 'l': case 'i': case 't': case 'I': case '1': baseWidth = 5; break;
                case 'W': case 'M': case 'O': case 'Q': baseWidth = 7; break;
                default:
                    if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) baseWidth = 6;
                    else baseWidth = 14; // emoji/fallback
            }
            sum += bold ? baseWidth + 1 : baseWidth;
        }
        return sum;
    }

    public String alignText(@NotNull String text, TextAlign align, boolean bold) {
        int width = calculateTextWidth(text, bold);
        if (width >= MAX_PIXEL_WIDTH || align == TextAlign.LEFT) return text;

        int remaining = MAX_PIXEL_WIDTH - width;
        int spaces = (align == TextAlign.CENTER) ? (remaining / 8) : (remaining / 4);
        return Spacing.custom(spaces) + text;
    }

    public Component alignComponent(@NotNull Component component, TextAlign align) {
        if (!(component instanceof TextComponent tc)) return component;

        boolean bold = tc.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE;
        String aligned = alignText(tc.content(), align, bold);
        TextComponent.Builder b = Component.text().content(aligned).style(tc.style());
        for (Component child : tc.children()) b.append(child);
        return b.build();
    }

}
