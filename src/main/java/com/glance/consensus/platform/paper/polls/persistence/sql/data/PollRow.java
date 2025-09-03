package com.glance.consensus.platform.paper.polls.persistence.sql.data;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public record PollRow(
   @ColumnName("id") String id,
   @ColumnName("readable_id") String readableId,
   @ColumnName("owner") String owner,
   @ColumnName("question_raw") String questionRaw,
   @ColumnName("created_at") long createdAt,
   @ColumnName("closes_at") long closesAt,
   @ColumnName("closed_at") Long closedAt,
   @ColumnName("closed") int closed,
   @ColumnName("multiple_choice") int multipleChoice,
   @ColumnName("max_selections") int maxSelections,
   @ColumnName("allow_resubmissions") int allowResubmissions,
   @ColumnName("show_results") int showResults
) {}
