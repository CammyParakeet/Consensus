package com.glance.consensus.platform.paper.polls.display.book.alignment;

/**
 * Commonly used text symbols for formatting, decoration, and visual markers
 * <p>
 * Can be used in book UIs, chat messages, or GUI item names
 *
 * <p><b>Examples:</b></p>
 * <ul>
 *   <li>{@link #DOT} - •</li>
 *   <li>{@link #STAR} - ★</li>
 *   <li>{@link #CHECK} - ✔</li>
 *   <li>{@link #CROSS} - ✖</li>
 *   <li>{@link #ARROW} - ➜</li>
 * </ul>
 *
 * @author Cammy
 */
public enum Symbols {
    DOT("•"), STAR("★"), UNDERSCORE("_"), UNDERLINED(" "),
    DASH("-"), DASH_THIN("─"), DASH_THICK("═"), DASH_BOLD("━"),
    ARROW("➜"), CHECK("✔"), CROSS("✖"), BLOCK("■");

    public final String s;
    Symbols(String s) { this.s = s; }
}
