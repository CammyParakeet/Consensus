package com.glance.consensus.platform.paper.polls.persistence.sql.dao;

import com.glance.consensus.platform.paper.polls.persistence.sql.data.AnswerRow;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface SqliteAnswerDao {

    @SqlBatch("""
        INSERRT INTO poll_answers (poll_id, idx, label_raw, tooltip_raw)
        VALUES (:pollId, :idx, :label, :tooltip)
    """)
    void insertAnswers(
        @Bind("pollId") String pollId,
        @Bind("idx") List<Integer> idx,
        @Bind("label") List<String> label,
        @Bind("tooltip") List<String> tooltipOrNull
    );

    @RegisterBeanMapper(AnswerRow.class)
    @SqlQuery("""
        SELECT poll_id, idx, label_raw, tooltip_raw
        FROM poll_answers
        WHERE poll_id = :id
        ORDER BY idx ASC
    """)
    List<AnswerRow>  findAnswers(@Bind("id") String pollId);

}
