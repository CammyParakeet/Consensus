package com.glance.consensus.platform.paper.utils;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Mini {

    /**
     * Parse a MiniMessage string with control over newline handling
     * <ul>
     *   <li>When {@code allowNewlines=true}: parse normally.</li>
     *   <li>When {@code allowNewlines=false}: strip out any {@code <newline>} tags and
     *       any literal {@code \r} / {@code \n} characters.</li>
     * </ul>
     *
     * @param raw MiniMessage string
     * @param acceptNewLines whether to allow {@code <newline>} tags and literal newlines
     * @return rendered component
     */
    public Component parseMini(@NotNull String raw, boolean acceptNewLines) {
        String sanitized = raw;
        if (!acceptNewLines) {
            sanitized = sanitized
                    .replace("<newline>", "")
                    .replace("</newline>", "")
                    .replace("\r", "")
                    .replace("\n", "");
        }
        return MiniMessage.miniMessage().deserialize(sanitized);
    }

    /**
     * Utility to parse raw MiniMessage strings into components
     *
     * @param raw MiniMessage string
     * @return rendered component
     */
    public Component parseMini(@NotNull String raw) {
        return MiniMessage.miniMessage().deserialize(raw);
    }

}
