package com.glance.consensus.platform.paper.commands.impl.core;

import com.glance.consensus.platform.paper.commands.engine.CommandHandler;
import com.glance.consensus.platform.paper.polls.display.book.PollBookViews;
import com.glance.consensus.platform.paper.polls.display.book.builder.ClickMode;
import com.glance.consensus.platform.paper.polls.persistence.PollStorage;
import com.glance.consensus.platform.paper.polls.runtime.PollManager;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.utils.Mini;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
@AutoService(CommandHandler.class)
public class AdminPollCommands implements CommandHandler {

    public enum ListOption {
        ALL, ACTIVE, CLOSED
    }

    private final PollManager manager;
    private final PollStorage storage;

    @Inject
    public AdminPollCommands(
        @NotNull PollManager manager,
        @NotNull PollStorage storage
    ) {
        this.manager = manager;
        this.storage = storage;
    }

    @Suggestions("pollIds")
    public List<String> suggestPollIds(
        final CommandContext<CommandSender> ctx,
        final String input
    ) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        ListOption scope = ListOption.ACTIVE;

        if (ctx.contains("option")) {
            try {
                scope = ListOption.valueOf(ctx.get("option").toString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }

        Collection<PollRuntime> source = switch (scope) {
            case ACTIVE -> manager.active();
            case CLOSED -> manager.closed();
            case ALL -> manager.all();
        };

        var ids = source.stream()
            .map(rt -> rt.getPoll().getId().toString())
            .sorted()
            .collect(Collectors.toCollection(ArrayList::new));

        if (scope == ListOption.ACTIVE || scope == ListOption.ALL) {
            ids.addFirst("latest");
        }

        if (prefix.isEmpty()) return ids;
        return ids.stream()
            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
            .toList();
    }

    @Command("poll create [id]")
    @Permission("consensus.polls.create")
    public void createPoll(
        @NotNull Player player,
        @Nullable @Argument String id
    ) {
        this.manager.startBuildSession(player, id);
    }

    // todo use list option for suggestions
    @Command("poll open <id>")
    @Permission("consensus.polls.open")
    public void openPoll(
        @NotNull Player player,
        @NotNull @Argument(value = "id", suggestions = "pollIds") String id
    ) {
        var targetId = resolvePollId(id);
        if (targetId.isEmpty()) {
            player.sendMessage(mm("<red>Unknown poll id:</red> <gray>" + id + "</gray>"));
            return;
        }

        var rtOpt = manager.get(targetId.get());
        if (rtOpt.isEmpty()) {
            player.sendMessage(mm("<red>No in-memory runtime for poll:</red> <gray>" + targetId.get() + "</gray>"));
            return;
        }

        ItemStack book = PollBookViews.buildVotingBook(player, rtOpt.get(), ClickMode.CALLBACKS);
        player.openBook(book);
    }

    @Command("poll delete <id>")
    @Permission("consensus.polls.delete")
    public void deletePoll(
        @NotNull CommandSender sender,
        @NotNull @Argument(value = "id", suggestions = "pollIds") String id
    ) {
        var targetId = resolvePollId(id);
        if (targetId.isEmpty()) {
            sender.sendMessage("Unknown poll id: " + id);
            return;
        }

        this.storage.deletePoll(targetId.get()).thenRun(() ->
            sender.sendMessage("Deleted poll " + id));
    }

    @Command("poll list <option>")
    @Permission("consensus.polls.list")
    public void listPolls(
        @NotNull CommandSender sender,
        @NotNull @Argument("option") ListOption listOption
    ) {
        Instant now = Instant.now();
        List<PollRuntime> items = switch (listOption) {
            case ACTIVE -> sort(manager.active());
            case CLOSED -> sort(manager.closed());
            case ALL -> sort(manager.all());
        };

        if (items.isEmpty()) {
            sender.sendMessage(switch (listOption) {
                case ACTIVE -> "No active polls";
                case CLOSED -> "No closed polls";
                case ALL -> "No polls found";
            });
            return;
        }

        if (sender instanceof Player player) {
            List<Component> lines = new ArrayList<>();
            lines.add(mm(switch (listOption) {
                case ACTIVE -> "<gold><bold>Active Polls (" + items.size() + ")</bold></gold>";
                case CLOSED -> "<gold><bold>Closed Polls (" + items.size() + ")</bold></gold>";
                case ALL -> "<gold><bold>All Polls (" + items.size() + ")</bold></gold>";
            }));
            lines.add(Component.empty());

            int i = 1;

            for (PollRuntime rt : items) {
                var p = rt.getPoll();
                boolean closed = p.isClosed();
                var status = closed ? "<dark_red>CLOSED</dark_red>" : "<green>ACTIVE</green>";

                String timeBit;
                if (closed) {
                    // If later you track a distinct closedAt, show it here. For now show scheduled closesAt.
                    timeBit = "closed at <aqua>" + fmt(p.getClosesAt()) + "</aqua>";
                } else {
                    var left = Duration.between(now, p.getClosesAt());
                    timeBit = "closes in <aqua>" + humanize(left) + "</aqua>";
                }

                Component row = mm("<yellow>" + (i++) + ".</yellow> " + status + " "
                        + "<white>" + p.getQuestionRaw() + "</white> "
                        + "<gray>[id: " + p.getId() + "]</gray> <dark_gray>•</dark_gray> " + timeBit);

                lines.add(row);
            }

            player.sendMessage(Component.join(JoinConfiguration.newlines(), lines));
        } else {
            String header = switch (listOption) {
                case ACTIVE -> "Active Polls (" + items.size() + ")";
                case CLOSED -> "Closed Polls (" + items.size() + ")";
                case ALL -> "All Polls (" + items.size() + ")";
            };

            sender.sendMessage("=== " + header + " ===");

            int i = 1;
            for (PollRuntime rt : items) {
                var p = rt.getPoll();
                boolean closed = p.isClosed();
                String status = closed ? "CLOSED" : "ACTIVE";

                String timeBit;
                if (closed) {
                    timeBit = "closed at " + fmt(p.getClosesAt());
                } else {
                    var left = Duration.between(now, p.getClosesAt());
                    timeBit = "closes in " + humanize(left);
                }

                sender.sendMessage(
                i++ + ". [" + status + "] " +
                        "\"" + p.getQuestionRaw() + "\" " +
                        "(id=" + p.getId() + ", " + timeBit + ")"
                );
            }
        }
    }

    private Optional<UUID> resolvePollId(String raw) {
        if ("latest".equalsIgnoreCase(raw)) {
            return manager.active().stream()
                .min(Comparator.comparing(rt -> rt.getPoll().getClosesAt()))
                .map(rt -> rt.getPoll().getId());
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    // TODO move these to a proper util

    private static final DateTimeFormatter TF =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private static String fmt(Instant t) { return TF.format(t); }

    private static String humanize(Duration d) {
        if (d.isNegative() || d.isZero()) return "now";
        long s = d.getSeconds();
        long dys = s / 86_400, hrs = (s % 86_400) / 3_600, mins = (s % 3_600) / 60;
        if (dys > 0) return dys + "d " + hrs + "h";
        if (hrs > 0) return hrs + "h " + mins + "m";
        if (mins > 0) return mins + "m";
        return (s % 60) + "s";
    }

    private Component mm(String raw) {
        return Mini.parseMini(raw);
    }

    private List<PollRuntime> sort(Collection<PollRuntime> rawPolls) {
        return rawPolls
            .stream()
            .sorted(Comparator.comparing(rt -> rt.getPoll().getClosesAt()))
            .toList();
    }

}
