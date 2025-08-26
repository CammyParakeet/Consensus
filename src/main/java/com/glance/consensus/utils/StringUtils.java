package com.glance.consensus.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class StringUtils {

    public @Nullable String emptyToNull(@Nullable String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

}
