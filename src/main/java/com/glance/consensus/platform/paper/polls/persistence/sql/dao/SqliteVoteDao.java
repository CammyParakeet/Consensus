package com.glance.consensus.platform.paper.polls.persistence.sql.dao;

import com.glance.consensus.platform.paper.polls.persistence.sql.data.SelectionRow;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

@RegisterConstructorMapper(SelectionRow.class)
public interface SqliteVoteDao {

    @SqlUpdate("""
        DELETE FROM voter_selection
        WHERE poll_id = :pid AND voter_id = :vid
    """)
    void deleteVoter(@Bind("pid") String pollId, @Bind("vid") String voterId);

    @SqlBatch("""
        INSERT OR IGNORE INTO voter_selection (poll_id, voter_id, option_idx)
        VALUES (:pid, :vid, :idx)
    """)
    void insertSelections(
        @Bind("pid") String pollId,
        @Bind("vid") String voterId,
        @Bind("idx") List<Integer> selections
    );

    //@RegisterBeanMapper(SelectionRow.class)
    @SqlQuery("""
        SELECT voter_id, option_idx
        FROM voter_selection
        WHERE poll_id = :pid
        ORDER BY voter_id, option_idx
    """)
    List<SelectionRow> findSelections(@Bind("pid") String pollId);

    @SqlUpdate("""
        DELETE FROM voter_selection WHERE poll_id = :pid
    """)
    void deleteAllForPoll(@Bind("pid") String pollId);

}
