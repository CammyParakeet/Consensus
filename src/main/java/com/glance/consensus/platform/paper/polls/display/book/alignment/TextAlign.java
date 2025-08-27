package com.glance.consensus.platform.paper.polls.display.book.alignment;

/**
 * Text alignment modes for rendering within book pages or other constrained UIs
 * <p>
 * Determines how text is padded relative to the maximum available line width
 *
 * <ul>
 *   <li>{@link #LEFT} - No padding (default in Minecraft)</li>
 *   <li>{@link #CENTER} - Centers text horizontally</li>
 *   <li>{@link #RIGHT} - Right-aligns text within the line</li>
 * </ul>
 *
 * @author Cammy
 */
public enum TextAlign {
    LEFT, CENTER, RIGHT
}
