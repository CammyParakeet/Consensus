package com.glance.consensus.platform.paper.polls.menu;

import com.glance.consensus.platform.paper.polls.display.PollDisplayNavigator;
import com.glance.consensus.platform.paper.polls.display.format.PollTextBuilder;
import com.glance.consensus.platform.paper.polls.display.format.VoteBadgeUtils;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollListOption;
import com.glance.consensus.platform.paper.polls.menu.utils.SlotSpec;
import com.glance.consensus.platform.paper.polls.runtime.PollManager;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.utils.Mini;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.triumphteam.gui.container.GuiContainer;
import dev.triumphteam.gui.element.GuiItem;
import dev.triumphteam.gui.paper.Gui;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import dev.triumphteam.nova.MutableState;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
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
        List<PollRuntime> index = buildIndex(options.scope);
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
                MutableState<Integer> page = comp.remember(0);
                MutableState<PollListOption> scope = comp.remember(options.scope);

                comp.render(container -> {
                    List<PollRuntime> currentIndex = buildIndex(scope.get());

                    fillBorder(container);

                    renderPolls(container, player, currentIndex, page.get(), perPage, slots);

                    renderButtons(container, page, maxPage, scope, () -> {});
                });
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

    private void renderPolls(
        @NotNull GuiContainer<Player, ItemStack> container,
        @NotNull Player viewer,
        @NotNull List<PollRuntime> polls,
        int page,
        int perPage,
        List<Integer> slots
    ) {
        if (slots.isEmpty()) return;

        final int start = Math.max(0, page) * perPage;
        final int end = Math.min(start + perPage, polls.size());

        for (int i = start; i < end; i++) {
            final int slot = slots.get(i - start);
            final PollRuntime rt = polls.get(i);

            final GuiItem<Player, ItemStack> icon = buildPollIcon(viewer, rt);
            setAtSlot(container, slot, icon);
        }
    }

    private GuiItem<Player, ItemStack> buildPollIcon(Player viewer, PollRuntime rt) {
        boolean closed = rt.getPoll().isClosed();
        Material mat = closed ? Material.BOOK : Material.MAP;
        final ItemStack stack = ItemStack.of(mat);
        stack.editMeta(meta -> {
            var parsed = Mini.parseMini("<reset>" + rt.getPoll().getQuestionRaw())
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(parsed);

            // Lore = activity lines (badge + closes/closed info)
            meta.lore(PollTextBuilder.activityLore(rt.getPoll(), true));

            if (closed) meta.setEnchantmentGlintOverride(true);
        });

        return ItemBuilder
            .from(stack)
            .asGuiItem((p, ctx) -> this.navigator.openVoting(p, rt));
    }



    /** Border everywhere - inner grid + buttons */
    private void fillBorder(GuiContainer<Player, ItemStack> container) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        pane.editMeta(m -> m.displayName(Mini.parseMini("<gray> ")));
        GuiItem<Player, ItemStack> filler = ItemBuilder.from(pane).asGuiItem();
        final int size = ROWS * 9;
        for (int i = 0; i < size; i++) setAtSlot(container, i, filler);
    }

    private void renderButtons(
        GuiContainer<Player, ItemStack> container,
        MutableState<Integer> page,
        int maxPage,
        MutableState<PollListOption> scope,
        Runnable onScopeChange
    ) {
        placeIf(page.get() > 0, container, BTN_PREV, () -> {
            ItemStack it = ItemStack.of(Material.ARROW);
            it.editMeta(m -> m.displayName(Mini.parseMini("<yellow>Previous Page")
                    .decoration(TextDecoration.ITALIC, false)));

            return ItemBuilder.from(it).asGuiItem((p, ctx) -> page.update(v -> Math.max(0, v - 1)));
        });

        placeIf(true, container, BTN_FILTER, () -> {
            final PollListOption currentScope = scope.get();
            final String label = switch (currentScope) {
                case ACTIVE -> "<green>Showing: Open</green> <gray>(click to toggle)</gray>";
                case CLOSED -> "<yellow>Showing: Closed</yellow> <gray>(click to toggle)</gray>";
                case ALL -> "<aqua>Showing: All</aqua> <gray>(click to toggle)</gray>";
            };

            ItemStack it = ItemStack.of(Material.COMPARATOR);
            it.editMeta(m -> m.displayName(Mini.parseMini(label).decoration(TextDecoration.ITALIC, false)));
            return ItemBuilder.from(it).asGuiItem((p, ctx) -> {
               scope.update(PollsMenu::nextScope);
               page.set(0);
               onScopeChange.run();
            });
        });

        placeIf(page.get() < maxPage, container, BTN_NEXT, () -> {
            ItemStack it = ItemStack.of(Material.ARROW);
            it.editMeta(m -> m.displayName(Mini.parseMini("<yellow>Next Page")
                    .decoration(TextDecoration.ITALIC, false)));
            return ItemBuilder.from(it).asGuiItem((p, ctx) -> page.update(v -> Math.min(maxPage, v + 1)));
        });
    }

    /* ---- Slot Utils ---- */

    private void setAtSlot(
            GuiContainer<Player, ItemStack> container,
            int slot,
            GuiItem<Player, ItemStack> item
    ) {
        final int row = (slot / 9) + 1;
        final int col = (slot % 9) + 1;
        container.setItem(row, col, item);
    }

    private void placeIf(
            boolean condition,
            GuiContainer<Player, ItemStack> container,
            SlotSpec spec,
            Supplier<GuiItem<Player, ItemStack>> supplier
    ) {
        if (!condition || spec == null) return;
        for (int slot : spec.resolve(ROWS)) setAtSlot(container, slot, supplier.get());
    }

    /* ---- Further Utils ---- */

    private static PollListOption nextScope(PollListOption s) {
        return switch (s) {
            case ACTIVE -> PollListOption.CLOSED;
            case CLOSED -> PollListOption.ALL;
            case ALL -> PollListOption.ACTIVE;
        };
    }

}
