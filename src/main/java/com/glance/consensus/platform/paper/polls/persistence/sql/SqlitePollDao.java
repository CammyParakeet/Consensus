package com.glance.consensus.platform.paper.polls.persistence.sql;

import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface SqlitePollDao {

    /* ---- Schema ---- */

    @SqlUpdate("""
            PRAGMA foreign_keys = ON;
            """)
    void pragmaFK();

    @SqlUpdate("""
        CREATE TABLE IF NOT EXISTS polls (
            id TEXT PRIMARY KEY
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

}
