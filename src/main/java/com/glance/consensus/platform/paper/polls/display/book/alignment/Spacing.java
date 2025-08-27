package com.glance.consensus.platform.paper.polls.display.book.alignment;

/**
 * Represents predefined spacing values (as strings of spaces) for use in text layout
 * <p>
 * Useful for indenting or padding lines in book pages or chat messages
 *
 * <p><b>Predefined constants:</b></p>
 * <ul>
 *   <li>{@link #NONE} - no spacing</li>
 *   <li>{@link #SMALL} - 2 spaces</li>
 *   <li>{@link #TAB} - 4 spaces</li>
 *   <li>{@link #DOUBLE_TAB} - 8 spaces</li>
 * </ul>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * String padded = Spacing.TAB.space + "Indented text";
 * }</pre>
 *
 * @author Cammy
 */
public enum Spacing {
    NONE(""),
    SMALL("  "),
    TAB("    "),
    DOUBLE_TAB("        ");

    public final String space;
    Spacing(String s) { this.space = s; }

    public static String custom(int amount) {
        return " ".repeat(Math.max(0, amount));
    }
}
