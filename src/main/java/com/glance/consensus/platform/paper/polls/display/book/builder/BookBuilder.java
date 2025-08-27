package com.glance.consensus.platform.paper.polls.display.book.builder;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builder utility for creating interactive written books using Adventure {@link Component}s
 * <p>
 * Provides a fluent API to define a book's title, author, and content pages
 * <p>
 * Each page is represented as a list of Components, which are automatically joined
 * with newlines when written to the {@link BookMeta}
 * <p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * ItemStack book = new BookBuilder()
 *     .setTitle("Poll Results")
 *     .setAuthor("Server")
 *     .addPage(Component.text("Option 1: ✔"), Component.text("Option 2: ✖"))
 *     .itemStack();
 * }</pre>
 *
 * @author Cammy
 */
public final class BookBuilder {

    private Component title = Component.text("Untitled");
    private Component author = Component.text("Unknown");
    private final List<List<Component>> pages = new ArrayList<>();

    public BookBuilder setTitle(String title) { this.title = Component.text(title); return this; }
    public BookBuilder setTitle(Component title) { this.title = title; return this; }
    public BookBuilder setAuthor(String author) { this.author = Component.text(author); return this; }
    public BookBuilder setAuthor(Component author) { this.author = author; return this; }

    public BookBuilder addPage(Component... components) {
        List<Component> list = new ArrayList<>(components.length);
        list.addAll(Arrays.asList(components));
        pages.add(list);
        return this;
    }

    public BookBuilder addLineToPage(int pageIndex, Component line) {
        ensurePage(pageIndex);
        pages.get(pageIndex).add(line);
        return this;
    }

    public BookBuilder insertLineInPage(int pageIndex, int lineIndex, Component line) {
        ensurePage(pageIndex);
        List<Component> p = pages.get(pageIndex);
        if (lineIndex >= 0 && lineIndex <= p.size()) p.add(lineIndex, line);
        return this;
    }

    public BookBuilder removeLineFromPage(int pageIndex, int lineIndex) {
        if (pageIndex >= 0 && pageIndex < pages.size()) {
            List<Component> p = pages.get(pageIndex);
            if (lineIndex >= 0 && lineIndex < p.size()) p.remove(lineIndex);
        }
        return this;
    }

    private void ensurePage(int pageIndex) {
        while (pages.size() <= pageIndex) pages.add(new ArrayList<>());
    }

    public static Component withCommand(Component base, String command) {
        return base.clickEvent(ClickEvent.runCommand(command));
    }

    public static Component withUrl(Component base, String url) {
        return base.clickEvent(ClickEvent.openUrl(url));
    }

    public static Component withCopy(Component base, String value) {
        return base.clickEvent(ClickEvent.copyToClipboard(value));
    }

    public static Component withChangePage(Component base, int page) {
        return base.clickEvent(ClickEvent.changePage(page));
    }

    public static Component withCallback(Component base, ClickCallback<Audience> cb) {
        return base.clickEvent(ClickEvent.callback(cb));
    }

    public ItemStack itemStack() {
        ItemStack book = ItemStack.of(Material.WRITTEN_BOOK);
        book.editMeta(meta -> {
            if (meta instanceof BookMeta bm) {
                bm.title(this.title);
                bm.author(this.author);
                for (List<Component> page : pages) {
                    bm.addPages(Component.join(JoinConfiguration.newlines(), page));
                }
            }
        });
        return book;
    }

}
