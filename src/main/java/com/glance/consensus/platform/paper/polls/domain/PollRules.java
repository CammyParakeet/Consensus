package com.glance.consensus.platform.paper.polls.domain;

public record PollRules(boolean multipleChoice, int maxSelections, boolean allowResubmissions) {}
