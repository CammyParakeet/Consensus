package com.glance.consensus.platform.paper.polls.persistence.sql.data;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public record AnswerRow(
   @ColumnName("poll_id") String pollId,
   @ColumnName("idx") int idx,
   @ColumnName("label_raw") String labelRaw,
   @ColumnName("tooltip_raw") String tooltipRaw
) {}
