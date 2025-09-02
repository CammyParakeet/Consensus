package com.glance.consensus.platform.paper.polls.utils;

import com.glance.consensus.platform.paper.polls.domain.PollRules;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class RuleUtils {

    public final String MULTI_CHOICE_PERM = "consensus.polls.override.multiple";
    public final String RESUBMIT_PERM = "consensus.polls.override.resubmit";
    public final String VIEW_RESULTS_PERM = "consensus.polls.override.results";

    public PollRules effectiveRules(
        @Nullable Player player,
        @NotNull PollRules r
    ) {
        if (player == null) return r;
        boolean multiple = r.multipleChoice() || player.hasPermission(MULTI_CHOICE_PERM);
        boolean resub = r.allowResubmissions() || player.hasPermission(RESUBMIT_PERM);
        boolean canView = r.canViewResults() || player.hasPermission(VIEW_RESULTS_PERM);
        int maxSel = multiple ? Math.max(1, r.maxSelections()) : 1;

        return new PollRules(multiple, maxSel, resub, canView);
    }

}
