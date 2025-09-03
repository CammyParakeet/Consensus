package com.glance.consensus.platform.paper.polls.persistence.sql.data;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public record SelectionRow(
    @ColumnName("voter_id") String voterId,
    @ColumnName("option_idx") int optionIdx
) {}
