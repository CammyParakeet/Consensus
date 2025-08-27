package com.glance.consensus.utils;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@RequiredArgsConstructor
public final class Pair<A, B> implements Map.Entry<A, B>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public final @Nullable A first;
    public final @Nullable B second;

    public static <A, B> Pair<A, B> of(@Nullable A first, @Nullable B second) {
        return new Pair<>(first, second);
    }

    @Override
    public A getKey() {
        return first;
    }

    @Override
    public B getValue() {
        return second;
    }

    @Override
    public B setValue(B value) {
        throw new UnsupportedOperationException();
    }
}
