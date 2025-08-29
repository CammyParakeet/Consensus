package com.glance.consensus.platform.paper.polls.persistence.file;

import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.persistence.PollStorage;
import com.glance.consensus.platform.paper.polls.persistence.config.PollStorageConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Singleton
public class FlatFilePollStorage implements PollStorage {

    private final Gson gson;
    private final File baseDir; // polls
    private final File votesDir; // polls/votes
    private final Object ioLock = new Object();

    @Inject
    public FlatFilePollStorage (
        @NotNull Plugin plugin,
        @NotNull PollStorageConfig cfg
    ) {
        if (cfg.getBackend() != PollStorageConfig.Backend.FLATFILE) {
            throw new IllegalStateException("Configured backend is " + cfg.getBackend() +
                    " but we're trying to load the flatfile backend!");
        }

        this.baseDir = new File(plugin.getDataFolder(), cfg.getFlatFileDir());
        this.votesDir = new File(baseDir, "votes");
        this.baseDir.mkdirs();
        this.votesDir.mkdirs();

        this.gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting() // todo remove in prod
            .create();
    }

    /* ---- Paths ---- */

    private File pollFile(UUID pollId) {
        return new File(baseDir, pollId.toString() + ".json");
    }

    private File votesFile(UUID pollId) {
        return new File(votesDir, pollId.toString() + "-votes.json");
    }

    /* ---- DTOs ---- */

    // Stored poll JSON (no per-answer votes; those live in votes JSON)
    private static final class PollData {
        String id;
        @Nullable String readableId;
        String owner;
        String questionRaw;
        Long createdAt;
        Long closesAt;
        @Nullable Long closedAt;
        boolean closed;
        boolean multipleChoice;
        Integer maxSelections;
        boolean allowResubmissions;
        List<AnswerRecord> answers = new ArrayList<>();
    }

    private record AnswerRecord(
        int idx,
        @NotNull String labelRaw,
        @Nullable String tooltipRaw
    ) {}

    private static final Type VOTES_MAP_TYPE = new TypeToken<Map<String, Set<Integer>>>() {}.getType();

    private PollData toData(@NotNull Poll poll) {
        PollData pd = new PollData();
        pd.id = poll.getId().toString();
        pd.readableId = poll.getPollIdentifier();
        pd.owner = poll.getOwner().toString();
        pd.questionRaw = poll.getQuestionRaw();
        pd.createdAt = poll.getCreatedAt().toEpochMilli();
        pd.closesAt = poll.getClosesAt().toEpochMilli();
        pd.closedAt = poll.getClosedAt() != null ? poll.getClosedAt().toEpochMilli() : null;
        pd.closed = poll.isClosed();
        pd.multipleChoice = poll.getRules().multipleChoice();
        pd.maxSelections = poll.getRules().maxSelections();
        pd.allowResubmissions = poll.getRules().allowResubmissions();

        for (var a : poll.getOptions()) {
            AnswerRecord ar = new AnswerRecord(a.index(), a.labelRaw(), a.tooltipRaw());
            pd.answers.add(ar);
        }
        return pd;
    }

    private Poll fromData(PollData pd) {
        List<PollOption> opts = new ArrayList<>(pd.answers.size());
        for (var ar : pd.answers) {
            opts.add(new PollOption(ar.idx, ar.labelRaw, ar.tooltipRaw, 0)); // votes applied separately
        }
        var rules = new PollRules(pd.multipleChoice, pd.maxSelections, pd.allowResubmissions);
        Poll p = new Poll(
            UUID.fromString(pd.id),
            pd.readableId != null ? pd.readableId : pd.id,
            UUID.fromString(pd.owner),
            pd.questionRaw,
            Instant.ofEpochMilli(pd.createdAt),
            Instant.ofEpochMilli(pd.closesAt),
            pd.closedAt != null ? Instant.ofEpochMilli(pd.closedAt) : null,
            opts,
            rules
        );
        p.setClosed(pd.closed);
        return p;
    }

    /* ---- Core Store ---- */

    private Map<String, Set<Integer>> loadVotesRaw(UUID pollId) {
        File f = votesFile(pollId);
        if (!f.exists()) return new HashMap<>();
        try (Reader r = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            Map<String, Set<Integer>> m = gson.fromJson(r, VOTES_MAP_TYPE);
            return (m != null) ? m : new HashMap<>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveVotesRaw(UUID pollId, Map<String, Set<Integer>> votes) {
        File f = votesFile(pollId);
        try (Writer w = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(votes, VOTES_MAP_TYPE, w);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<PollData> loadPollData(UUID pollId) {
        File f = pollFile(pollId);
        if (!f.exists()) return Optional.empty();
        try (Reader r = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            PollData pr = gson.fromJson(r, PollData.class);
            return Optional.ofNullable(pr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void savePollData(PollData pr) {
        File f = pollFile(UUID.fromString(pr.id));
        try (Writer w = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(pr, w);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Integer, Integer> computeTallies(Map<String, Set<Integer>> votesRaw) {
        Map<Integer, Integer> t = new HashMap<>();
        for (Set<Integer> sel : votesRaw.values()) {
            for (int idx : sel) t.merge(idx, 1, Integer::sum);
        }
        return t;
    }

    @Override
    public CompletableFuture<Void> createPoll(@NotNull Poll poll) {
        return CompletableFuture.runAsync(() -> {
            synchronized (ioLock) {
                // write poll file
                savePollData(toData(poll));
                // ensure empty votes file exists
                File vf = votesFile(poll.getId());
                if (!vf.exists()) saveVotesRaw(poll.getId(), new HashMap<>());
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Poll>> loadPoll(@NotNull UUID pollId) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (ioLock) {
                return loadPollData(pollId).map(this::fromData);
            }
        });
    }

    @Override
    public CompletableFuture<List<Poll>> loadActivePolls() {
        return CompletableFuture.supplyAsync(() -> {
            long now = System.currentTimeMillis();
            List<Poll> out = new ArrayList<>();
            synchronized (ioLock) {
                File[] files = baseDir.listFiles((dir, name) -> name.endsWith(".json") && !name.contains("-votes"));
                if (files == null) return out;
                for (File f : files) {
                    try (Reader r = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
                        PollData pd = gson.fromJson(r, PollData.class);
                        if (pd == null) continue;
                        if (!pd.closed && pd.closesAt > now) out.add(fromData(pd));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return out;
        });
    }

    @Override
    public CompletableFuture<List<Poll>> loadRecentPolls(@NotNull Duration retention) {
        final long now = System.currentTimeMillis();
        final long cutoff = retention.isNegative() ? Long.MIN_VALUE : now - retention.toMillis();

        return CompletableFuture.supplyAsync(() -> {
           List<Poll> out = new ArrayList<>();
           synchronized (ioLock) {
               File[] files = baseDir.listFiles((dir, name) -> name.endsWith(".json") && !name.contains("-votes"));
               if (files == null) return out;

               for (File f : files) {
                   try (Reader r = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
                       PollData pd = gson.fromJson(r, PollData.class);
                       if (pd == null) continue;

                       boolean active = !pd.closed && pd.closesAt > now;
                       boolean recentClosed = pd.closed && (pd.closedAt != null) && pd.closedAt >= cutoff;

                       if (active || recentClosed) {
                           out.add(fromData(pd));
                       }
                   } catch (IOException e) {
                       throw new RuntimeException(e);
                   }
               }
           }
           return out;
        });
    }

    @Override
    public CompletableFuture<Void> closePoll(@NotNull UUID pollId, @NotNull Instant closedAt) {
        return CompletableFuture.runAsync(() -> {
            synchronized (ioLock) {
                var opt = loadPollData(pollId);
                if (opt.isEmpty()) return;
                var pd = opt.get();
                pd.closed = true;
                pd.closesAt = closedAt.toEpochMilli(); // keep closure moment
                savePollData(pd);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deletePoll(@NotNull UUID pollId) {
        return CompletableFuture.runAsync(() -> {
            synchronized (ioLock) {
                try {
                    Files.deleteIfExists(pollFile(pollId).toPath());
                    Files.deleteIfExists(votesFile(pollId).toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

}
