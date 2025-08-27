package com.glance.consensus.platform.paper.utils;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Mini {

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
