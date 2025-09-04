package com.glance.consensus.platform.paper.commands.impl.core;

import com.glance.consensus.platform.paper.commands.engine.CommandHandler;
import com.glance.consensus.platform.paper.polls.display.PollDisplayNavigator;
import com.glance.consensus.platform.paper.polls.display.format.PollTextBuilder;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollListOption;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.menu.PollsMenu;
import com.glance.consensus.platform.paper.polls.persistence.PollStorage;
import com.glance.consensus.platform.paper.polls.runtime.PollManager;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.polls.utils.RuleUtils;
import com.glance.consensus.platform.paper.utils.Mini;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
public class PollCommands implements CommandHandler {

    private final PollManager manager;
    private final PollStorage storage;
    private final PollDisplayNavigator displayNavigator;
    private final PollsMenu pollsMenu;

    @Inject
    public PollCommands(
        @NotNull PollManager manager,
        @NotNull PollStorage storage,
        @NotNull PollDisplayNavigator displayNavigator,
        @NotNull PollsMenu pollsMenu
    ) {
        this.manager = manager;
        this.storage = storage;
        this.displayNavigator = displayNavigator;
        this.pollsMenu = pollsMenu;
    }

    @Suggestions("pollIds")
    public List<String> suggestPollIds(
        final CommandContext<CommandSender> ctx,
        final String input
    ) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        PollListOption scope = PollListOption.ACTIVE;

        if (ctx.contains("option")) {
            try {
                scope = PollListOption.valueOf(ctx.get("option").toString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }

        Collection<PollRuntime> source = switch (scope) {
            case ACTIVE -> manager.active();
            case CLOSED -> manager.closed();
            case ALL -> manager.all();
        };

        var ids = source.stream()
            .map(rt -> rt.getPoll().getPollIdentifier())
            .sorted()
            .collect(Collectors.toCollection(ArrayList::new));

        if (scope == PollListOption.ACTIVE || scope == PollListOption.ALL) {
            ids.addFirst("latest");
        }

        if (prefix.isEmpty()) return ids;
        return ids.stream()
            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
            .toList();
    }

    @Command("polls")
    public void pollMenu(
        @NotNull Player player
    ) {
        this.pollsMenu.open(player);
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

        // TODO use the display manager
        //ItemStack book = PollBookViews.buildVotingBook(player, rtOpt.get(), null);
       this.displayNavigator.openVoting(player, rtOpt.get());
    }

    @Command("poll results <id>")
    public void openResults(
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
            player.sendMessage(mm("<red>No in-memory runtime for poll:</red> <gray>" +
                targetId.get() + "</gray>"));
            return;
        }

        PollRuntime rt = rtOpt.get();
        Poll poll = rt.getPoll();
        PollRules effective = RuleUtils.effectiveRules(player, poll.getRules());

        // Allowed paths:
        //  - player is allowed to view results (rule)
        //  - or the poll is already closed (always viewable)
        if (effective.canViewResults() || poll.isClosed()) {
            this.displayNavigator.openResults(player, rt);
            return;
        }

        // Denied: poll is still open AND results are hidden until close
        Instant now = Instant.now();
        Instant closesAt = poll.getClosesAt();
        String eta = now.isBefore(closesAt)
            ? PollTextBuilder.formatDuration(Duration.between(now, closesAt))
            : "soon";

        player.sendMessage(mm(
            "<red>You can’t view results yet.</red> <gray>This poll hides results until it closes.</gray>\n" +
                    "<gray>Closes in:</gray> <yellow>" + eta + "</yellow>"
        ));
    }

    @Command("poll close <id>")
    @Permission("consensus.polls.close")
    public void closePoll(
        @NotNull CommandSender sender,
        @NotNull @Argument(value = "id", suggestions = "pollIds") String id
    ) {
        var targetId = resolvePollId(id);
        if (targetId.isEmpty()) {
            sender.sendMessage("Unknown poll id: " + id);
            return;
        }
        this.manager.close(targetId.get());
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

        this.storage.deletePoll(targetId.get())
            .exceptionally(ex -> {
                sender.sendMessage("Unknown poll " + id);
                return null;
            })
            .thenRun(() -> {
                this.manager.clearLocal(targetId.get());
                sender.sendMessage("Deleted poll " + id);
            });

    }

    @Command("poll list [option]")
    @Permission("consensus.polls.list")
    public void listPolls(
        @NotNull CommandSender sender,
        @Nullable @Argument("option") PollListOption listOption
    ) {
        Instant now = Instant.now();
        var option = listOption != null ? listOption : PollListOption.ALL;
        List<PollRuntime> items = switch (option) {
            case ACTIVE -> sort(manager.active());
            case CLOSED -> sort(manager.closed());
            case ALL -> sort(manager.all());
        };

        if (items.isEmpty()) {
            sender.sendMessage(switch (option) {
                case ACTIVE -> "No active polls";
                case CLOSED -> "No closed polls";
                case ALL -> "No polls found";
            });
            return;
        }

        if (sender instanceof Player player) {
            List<Component> lines = new ArrayList<>();
            lines.add(mm(switch (option) {
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
                    timeBit = "closed at <aqua>" + fmt(p.getClosesAt()) + "</aqua>";
                } else {
                    var left = Duration.between(now, p.getClosesAt());
                    timeBit = "closes in <aqua>" + PollTextBuilder.formatDuration(left) + "</aqua>";
                }

                Component row = Component.text()
                        .append(mm("<yellow>" + (i++) + ".</yellow> " + status + " "))
                        .append(mm("<white>" + p.getQuestionRaw() + "</white> "))
                        .append(idChip(p.getPollIdentifier()))
                        .append(mm(" <dark_gray>•</dark_gray> " + timeBit))
                        .build();

                lines.add(row);
            }

            player.sendMessage(Component.join(JoinConfiguration.newlines(), lines));
        } else {
            String header = switch (option) {
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
                    timeBit = "closes in " + PollTextBuilder.formatDuration(left);
                }

                sender.sendMessage(
                i++ + ". [" + status + "] " +
                        "\"" + p.getQuestionRaw() + "\" " +
                        "(id=" + p.getId() + ", " + timeBit + ")"
                );
            }
        }
    }

    private Component idChip(@NotNull String id) {
        return mm("<gray>[id: <aqua>" + id + "</aqua>]</gray>")
                .hoverEvent(mm("<yellow>Click to copy</yellow>"))
                .clickEvent(ClickEvent.copyToClipboard(id));
    }

    private Optional<UUID> resolvePollId(String raw) {
        if ("latest".equalsIgnoreCase(raw)) {
            return manager.active().stream()
                .min(Comparator.comparing(rt -> rt.getPoll().getCreatedAt()))
                .map(rt -> rt.getPoll().getId());
        }

        Optional<UUID> asUuid = Optional.empty();
        try {
            asUuid = Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {}
        if (asUuid.isPresent()) return asUuid;

        return manager.all()
                .stream()
                .map(PollRuntime::getPoll)
                .filter(p -> raw.trim().equals(p.getPollIdentifier()))
                .sorted(Comparator.comparing(Poll::getCreatedAt).reversed())
                .map(Poll::getId)
                .findFirst();
    }

    // TODO move these to a proper util

    private static final DateTimeFormatter TF =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private static String fmt(Instant t) { return TF.format(t); }

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
