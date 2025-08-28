package com.glance.consensus.platform.paper.polls.persistence.file;

import ca.spottedleaf.concurrentutil.completable.Completable;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollAnswer;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.persistence.PollStorage;
import com.glance.consensus.platform.paper.polls.persistence.config.PollStorageConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
    private static final class PollRecord {
        String id;
        String owner;
        String questionRaw;
        long createdAt;
        long closesAt;
        boolean closed;
        boolean multipleChoice;
        int maxSelections;
        boolean allowResubmissions;
        List<AnswerRecord> answers = new ArrayList<>();
    }

    private static final class AnswerRecord {
        int idx;
        String labelRaw;
        String tooltipRaw; // nullable
    }

    private static final Type VOTES_MAP_TYPE = new TypeToken<Map<String, Set<Integer>>>() {}.getType();

    private PollRecord toRecord(Poll poll) {
        PollRecord r = new PollRecord();
        r.id = poll.getId().toString();
        r.owner = poll.getOwner().toString();
        r.questionRaw = poll.getQuestionRaw();
        r.createdAt = poll.getCreatedAt().toEpochMilli();
        r.closesAt = poll.getClosesAt().toEpochMilli();
        r.closed = poll.isClosed();
        r.multipleChoice = poll.getRules().multipleChoice();
        r.maxSelections = poll.getRules().maxSelections();
        r.allowResubmissions = poll.getRules().allowResubmissions();

        for (var a : poll.getOptions()) {
            AnswerRecord ar = new AnswerRecord();
            ar.idx = a.index();
            ar.labelRaw = a.labelRaw();
            ar.tooltipRaw = a.tooltipRaw();
            r.answers.add(ar);
        }
        return r;
    }

    private Poll fromRecord(PollRecord r) {
        List<PollAnswer> opts = new ArrayList<>(r.answers.size());
        for (var ar : r.answers) {
            opts.add(new PollAnswer(ar.idx, ar.labelRaw, ar.tooltipRaw, 0)); // votes applied separately
        }
        var rules = new PollRules(r.multipleChoice, r.maxSelections, r.allowResubmissions);
        Poll p = new Poll(
            UUID.fromString(r.id),
            UUID.fromString(r.owner),
            r.questionRaw,
            Instant.ofEpochMilli(r.createdAt),
            Instant.ofEpochMilli(r.closesAt),
            opts,
            rules
        );
        p.setClosed(r.closed);
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

    private Optional<PollRecord> loadPollRecord(UUID pollId) {
        File f = pollFile(pollId);
        if (!f.exists()) return Optional.empty();
        try (Reader r = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            PollRecord pr = gson.fromJson(r, PollRecord.class);
            return Optional.ofNullable(pr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void savePollRecord(PollRecord pr) {
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
                savePollRecord(toRecord(poll));
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
                return loadPollRecord(pollId).map(this::fromRecord);
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
                        PollRecord pr = gson.fromJson(r, PollRecord.class);
                        if (pr == null) continue;
                        if (!pr.closed && pr.closesAt > now) out.add(fromRecord(pr));
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
                var opt = loadPollRecord(pollId);
                if (opt.isEmpty()) return;
                var pr = opt.get();
                pr.closed = true;
                pr.closesAt = closedAt.toEpochMilli(); // keep closure moment
                savePollRecord(pr);
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
