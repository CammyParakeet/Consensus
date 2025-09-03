package com.glance.consensus.platform.paper.polls.persistence.sql;

import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.persistence.PollStorage;
import com.glance.consensus.platform.paper.polls.persistence.sql.dao.SqliteAnswerDao;
import com.glance.consensus.platform.paper.polls.persistence.sql.dao.SqlitePollDao;
import com.glance.consensus.platform.paper.polls.persistence.sql.dao.SqliteVoteDao;
import com.glance.consensus.platform.paper.polls.persistence.sql.data.AnswerRow;
import com.glance.consensus.platform.paper.polls.persistence.sql.data.PollRow;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Singleton
public class JdbiPollStorage implements PollStorage {

    private final Plugin plugin;
    private final SqlBootstrap sql;

    @Inject
    public JdbiPollStorage(
        @NotNull final Plugin plugin,
        @NotNull final SqlBootstrap sql
    ) {
        this.plugin = plugin;
        this.sql = sql;

        if (sql.dialect() == SqlBootstrap.Dialect.SQLITE) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("""
                    SQLite driver not found. Options:
                      - Use the '-with-sqlite' jar of the plugin, or
                      - Install a dedicated SQLite driver plugin, or
                      - Switch backend to FLATFILE in config.
                    """);
                throw new RuntimeException(e);
            }
        }

        // TODO

        plugin.getLogger().info("JDBI poll storage initialised (" + sql.dialect() + ")");
    }

    /* ---- Helpers ---- */

    private static int b(boolean value) { return value ? 1 : 0; }

    private static Poll toDomain(PollRow pr, List<AnswerRow> answers) {
        List<PollOption> opts = new ArrayList<>(answers.size());
        for (var a : answers) {
            // reads & aggregates votes later
            opts.add(new PollOption(a.idx(), a.labelRaw(), a.tooltipRaw(), 0));
        }
        PollRules rules = new PollRules(
            pr.multipleChoice() != 0,
                pr.maxSelections(),
                pr.allowResubmissions() != 0,
                pr.showResults() != 0
        );
        Poll p = new Poll(
            UUID.fromString(pr.id()),
            pr.readableId() != null ? pr.readableId() : pr.id(),
            UUID.fromString(pr.owner()),
            pr.questionRaw(),
            Instant.ofEpochMilli(pr.createdAt()),
            Instant.ofEpochMilli(pr.closesAt()),
            (pr.closedAt() == null) ? null : Instant.ofEpochMilli(pr.closedAt()),
            opts,
            rules
        );

        p.setClosed(pr.closed() != 0);
        return p;
    }

    /* ---- Poll API ---- */

    @Override
    public CompletableFuture<Void> createPoll(@NotNull Poll poll) {
        PollRules rules = poll.getRules();
        return CompletableFuture.runAsync(() -> {
            sql.jdbi().useTransaction(h -> {
                var pollDao = h.attach(SqlitePollDao.class);
                var ansDao = h.attach(SqliteAnswerDao.class);

                pollDao.upsertPoll(
                    poll.getId().toString(),
                    poll.getPollIdentifier(),
                    poll.getOwner().toString(),
                    poll.getQuestionRaw(),
                    poll.getCreatedAt().toEpochMilli(),
                    poll.getClosesAt().toEpochMilli(),
                    poll.getClosedAt() == null ? null : poll.getClosedAt().toEpochMilli(),
                    b(poll.isClosed()),
                    b(rules.multipleChoice()),
                    rules.maxSelections(),
                    b(rules.allowResubmissions()),
                    b(rules.canViewResults())
                );

                // replace answers
                pollDao.deleteAnswers(poll.getId().toString());
                if (!poll.getOptions().isEmpty()) {
                    int size=  poll.getOptions().size();
                    List<Integer> idx = new ArrayList<>(size);
                    List<String> labels = new ArrayList<>(size);
                    List<String> tooltips = new ArrayList<>(size);
                    for (var o : poll.getOptions()) {
                        idx.add(o.index());
                        labels.add(o.labelRaw());
                        tooltips.add(o.tooltipRaw());
                    }
                    ansDao.insertAnswers(poll.getId().toString(), idx, labels, tooltips);
                }
            });
        });
    }

    @Override
    public CompletableFuture<Optional<Poll>> loadPoll(@NotNull UUID pollId) {
        return CompletableFuture.supplyAsync(() ->
            sql.jdbi().withHandle(h -> {
                var pollDao = h.attach(SqlitePollDao.class);
                var ansDao = h.attach(SqliteAnswerDao.class);

                var pr = pollDao.findPoll(pollId.toString());
                if (pr.isEmpty()) return Optional.empty();

                var answers = ansDao.findAnswers(pollId.toString());
                return Optional.of(toDomain(pr.get(), answers));
            })
        );
    }

    @Override
    public CompletableFuture<List<Poll>> loadActivePolls() {
        return CompletableFuture.supplyAsync(() ->
            sql.jdbi().withHandle(h -> {
                var pollDao = h.attach(SqlitePollDao.class);
                var ansDao  = h.attach(SqliteAnswerDao.class);

                long now = System.currentTimeMillis();
                var rows = pollDao.findActive(now);
                List<Poll> out = new ArrayList<>(rows.size());
                for (var r : rows) {
                    var answers = ansDao.findAnswers(r.id());
                    out.add(toDomain(r, answers));
                }
                return out;
            })
        );
    }

    @Override
    public CompletableFuture<List<Poll>> loadRecentPolls(@NotNull Duration retention) {
        return CompletableFuture.supplyAsync(() ->
            sql.jdbi().withHandle(h -> {
                var pollDao = h.attach(SqlitePollDao.class);
                var ansDao = h.attach(SqliteAnswerDao.class);

                long now = System.currentTimeMillis();
                long cutoff = retention.isNegative() ? Long.MIN_VALUE : (now - retention.toMillis());

                int autoClosed = pollDao.closeOverdue(now);
                if (autoClosed > 0) {
                    plugin.getLogger().info("Auto-Closed " + autoClosed + " overdue polls");
                }

                var rows = pollDao.findRecent(now, cutoff);
                List<Poll> out = new ArrayList<>(rows.size());
                for (var r : rows) {
                    var answers = ansDao.findAnswers(r.id());
                    out.add(toDomain(r, answers));
                }
                return out;
            })
        );
    }

    @Override
    public CompletableFuture<Void> closePoll(@NotNull UUID pollId, @NotNull Instant closedAt) {
        return CompletableFuture.runAsync(() ->
            sql.jdbi().useExtension(SqlitePollDao.class,
                    dao -> dao.closePoll(pollId.toString(), closedAt.toEpochMilli()))
        );
    }

    @Override
    public CompletableFuture<Void> deletePoll(@NotNull UUID pollId) {
        return CompletableFuture.runAsync(() ->
            sql.jdbi().useTransaction(h -> {
                var pollDao = h.attach(SqlitePollDao.class);
                // ON DELETE CASCADE will clear answers + votes
                pollDao.deletePoll(pollId.toString());
            })
        );
    }

    /* ---- Vote API ---- */

    @Override
    public CompletableFuture<Void> saveVoterSelection(
        @NotNull UUID pollId,
        @NotNull UUID voterId,
        @NotNull Set<Integer> indices
    ) {
        final List<Integer> ordered = indices.stream().sorted().toList();
        return CompletableFuture.runAsync(() ->
            sql.jdbi().useTransaction(h -> {
                var voteDao = h.attach(SqliteVoteDao.class);
                final String pid = pollId.toString();
                final String vid = voterId.toString();

                voteDao.deleteVoter(pid, vid);
                if (!ordered.isEmpty()) {
                    voteDao.insertSelections(pid, vid, ordered);
                }
            })
        );
    }

    @Override
    public CompletableFuture<Map<UUID, Set<Integer>>> loadAllSelections(@NotNull UUID pollId) {
        return CompletableFuture.supplyAsync(() ->
            sql.jdbi().withExtension(SqliteVoteDao.class, dao -> {
                var rows = dao.findSelections(pollId.toString());
                Map<UUID, Set<Integer>> out = new LinkedHashMap<>();
                for (var r : rows) {
                    UUID uid;
                    try { uid = UUID.fromString(r.voterId()); } catch (Exception e) { continue; }
                    out.computeIfAbsent(uid, k -> new LinkedHashSet<>()).add(r.optionIdx());
                }
                out.replaceAll((k, v) -> Collections.unmodifiableSet(v));
                return Collections.unmodifiableMap(out);
            })
        );
    }

}
