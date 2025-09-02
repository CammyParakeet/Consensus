package com.glance.consensus.platform.paper.polls.menu;

import com.glance.consensus.platform.paper.polls.display.PollDisplayNavigator;
import com.glance.consensus.platform.paper.polls.domain.PollListOption;
import com.glance.consensus.platform.paper.polls.menu.utils.SlotSpec;
import com.glance.consensus.platform.paper.polls.runtime.PollManager;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.utils.Mini;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.triumphteam.gui.paper.Gui;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Singleton
public class PollsMenu {

    private static final int ROWS = 6;

    private static final SlotSpec INNER_SLOTS = SlotSpec.of("rows:2-4 cols:2-8");

    private static final SlotSpec BTN_PREV = SlotSpec.of("row:" + ROWS + " col:4");
    private static final SlotSpec BTN_FILTER = SlotSpec.of("row:" + ROWS + " col:5");
    private static final SlotSpec BTN_NEXT = SlotSpec.of("row:" + ROWS + " col:6");

    /** Options for opening the menu/displaying polls */
    public static final class Options {
        public @NotNull PollListOption scope = PollListOption.ALL;
        // TODO add as needed
    }

    private final Plugin plugin;
    private final PollManager pollManager;
    private final PollDisplayNavigator navigator;

    @Inject
    public PollsMenu(
        @NotNull Plugin plugin,
        @NotNull PollManager pollManager,
        @NotNull PollDisplayNavigator navigator
    ) {
        this.plugin = plugin;
        this.pollManager = pollManager;
        this.navigator = navigator;
    }

    public void open(@NotNull Player player) {
        open(player, new Options());
    }

    public void open(@NotNull Player player, @NotNull Options options) {
        final List<PollRuntime> index = buildIndex(options.scope);
        if (index.isEmpty()) {
            Gui.of(1)
                .title(Mini.parseMini("<red>No Polls found"))
                .build()
                .open(player);
            return;
        }

        final List<Integer> slots = INNER_SLOTS.resolve(ROWS);
        final int perPage = Math.max(1, slots.size());
        final int maxPage = Math.max(0, (index.size() - 1) / perPage);

        Gui gui = Gui.of(ROWS)
            .title(Mini.parseMini("<yellow>Consensus</yellow> <gray>- Polls</gray>"))
            .component(comp -> {

            })
            .build();

        gui.open(player);
    }

    /* ---- Index building / sorting ---- */

    private List<PollRuntime> buildIndex(@NotNull PollListOption scope) {
        List<PollRuntime> active = new ArrayList<>(pollManager.active());
        active.sort(Comparator.comparing(rt -> rt.getPoll().getClosesAt())); // soonest closing first

        List<PollRuntime> closed = pollManager.closed().stream()
                .sorted(Comparator.comparing((PollRuntime rt) -> rt.getPoll().getClosesAt()).reversed())
                .toList();

        return switch (scope) {
            case ACTIVE -> active;
            case CLOSED -> closed;
            case ALL -> {
                List<PollRuntime> all = new ArrayList<>(active.size() + closed.size());
                all.addAll(active);
                all.addAll(closed);
                yield all;
            }
        };
    }

    /* ---- Render passes ---- */

}
