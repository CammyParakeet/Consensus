package com.glance.consensus.platform.paper.utils;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class ComponentUtils {

    public boolean resolveBold(@NotNull Component c, boolean inheritedBold) {
        TextDecoration.State state = c.decoration(TextDecoration.BOLD);
        if (state == TextDecoration.State.TRUE) return true;
        if (state == TextDecoration.State.FALSE) return false;
        return inheritedBold;
    }

    public boolean isVisuallyEmpty(@Nullable Component c) {
        if (c == null) return true;
        if (c instanceof TextComponent tc) {
            if (!tc.content().isEmpty()) return false;
        }
        for (Component child : c.children()) {
            if (!isVisuallyEmpty(child)) return false;
        }
        return true;
    }

}
