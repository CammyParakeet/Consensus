package com.glance.consensus.platform.paper.polls.persistence.sql.dao;

import com.glance.consensus.platform.paper.polls.persistence.sql.data.PollRow;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterConstructorMapper(PollRow.class)
public interface SqlitePollDao {

    /* ---- Schema ---- */

    @SqlUpdate("""
            PRAGMA foreign_keys = ON;
            """)
    void pragmaFK();

    @SqlUpdate("""
        CREATE TABLE IF NOT EXISTS polls (
            id TEXT PRIMARY KEY,
            readable_id TEXT,
            owner TEXT NOT NULL,
            question_raw TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            closes_at INTEGER NOT NULL,
            closed_at INTEGER,
            closed INTEGER NOT NULL DEFAULT 0,
            multiple_choice INTEGER NOT NULL,
            max_selections INTEGER NOT NULL,
            allow_resubmissions INTEGER NOT NULL,
            show_results INTEGER NOT NULL
        );
    """)
    void createPolls();

    @SqlUpdate("""
        CREATE TABLE IF NOT EXISTS poll_answers (
            poll_id TEXT NOT NULL,
            idx INTEGER NOT NULL,
            label_raw TEXT NOT NULL,
            tooltip_raw TEXT,
            PRIMARY KEY (poll_id, idx),
            FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
        );
    """)
    void createAnswers();

    @SqlUpdate("""
        CREATE TABLE IF NOT EXISTS voter_selection (
            poll_id TEXT NOT NULL,
            voter_id TEXT NOT NULL,
            option_idx INTEGER NOT NULL,
            PRIMARY KEY (poll_id, voter_id, option_idx),
            FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
        );
    """)
    void createVotes();

    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_polls_active ON polls(closed, closes_at)")
    void idxPollsActive();

    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_polls_closed_at ON polls(closed_at)")
    void idxPollsClosedAt();

    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_votes_poll ON voter_selection(poll_id)")
    void idxVotesPoll();

    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_votes_poll_voter ON voter_selection(poll_id, voter_id)")
    void idxVotesPollVoter();

    /* ---- Poll CRUD ---- */

    @SqlUpdate("""
        INSERT INTO polls(
            id, readable_id, owner, question_raw,
            created_at, closes_at, closed_at, closed,
            multiple_choice, max_selections, allow_resubmissions, show_results
        ) VALUES (
            :id, :readableId, :owner, :questionRaw,
            :createdAt, :closesAt, :closedAt, :closed,
            :multipleChoice, :maxSelections, :allowResubmissions, :showResults
        )
        ON CONFLICT(id) DO UPDATE SET
            readable_id = excluded.readable_id,
            owner = excluded.owner,
            question_raw = excluded.question_raw,
            created_at = excluded.created_at,
            closes_at = excluded.closes_at,
            closed_at = excluded.closed_at,
            closed = excluded.closed,
            multiple_choice = excluded.multiple_choice,
            max_selections = excluded.max_selections,
            allow_resubmissions = excluded.allow_resubmissions,
            show_results = excluded.show_results
    """)
    void upsertPoll(
        @Bind("id") String id,
        @Bind("readableId") String readableId,
        @Bind("owner") String owner,
        @Bind("questionRaw") String questionRaw,
        @Bind("createdAt") long createdAt,
        @Bind("closesAt") long closesAt,
        @Bind("closedAt") Long closedAt,
        @Bind("closed") int closed,
        @Bind("multipleChoice") int multipleChoice,
        @Bind("maxSelections") int maxSelections,
        @Bind("allowResubmissions") int allowResubmissions,
        @Bind("showResults") int showResults
    );

    @SqlUpdate("DELETE FROM poll_answers WHERE poll_id = :id")
    void deleteAnswers(@Bind("id") String pollId);

    @SqlUpdate("DELETE FROM polls WHERE id = :id")
    void deletePoll(@Bind("id") String pollId);

    @SqlUpdate("""
        UPDATE polls
        SET closed = 1, closed_at = :closedAt
        WHERE id = :id
    """)
    void closePoll(
        @Bind("id") String pollId,
        @Bind("closedAt") long closedAt
    );

    //@RegisterBeanMapper(PollRow.class)
    @SqlQuery("SELECT * FROM polls WHERE id = :id")
    Optional<PollRow> findPoll(@Bind("id") String pollId);

    //@RegisterBeanMapper(PollRow.class)
    @SqlQuery("""
        SELECT * FROM polls
        WHERE closed = 0 AND (closes_at IS NULL or closes_at > :now)
    """)
    List<PollRow> findActive(@Bind("now") long nowMillis);

    @SqlUpdate("""
        UPDATE polls
        SET closed = 1, closed_at = COALESCE(closed_at, closes_at)
        WHERE closed = 0 AND closes_at IS NOT NULL AND closes_at <= :now
    """)
    int closeOverdue(@Bind("now") long nowMillis);

    //@RegisterBeanMapper(PollRow.class)
    @SqlQuery("""
        SELECT * FROM polls
        WHERE (closed = 0 AND (closes_at IS NULL OR closes_at > :now))
            OR (closed = 1 AND closed_at IS NOT NULL AND closed_at >= :cutoff)
    """)
    List<PollRow> findRecent(@Bind("now") long nowMillis, @Bind("cutoff") long cutoffMillis);

}
