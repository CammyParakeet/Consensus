package com.glance.consensus.platform.paper.polls.display.book.builder;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;

@UtilityClass
public class BookUtils {

    public final Component DIVIDER = Component.text("-".repeat(19));

    public final Component SIDE_DIVIDER = Component.text("-" + " ".repeat(25) + "-");

}
