package com.glance.consensus.platform.paper.polls.menu.utils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;

/**
 * Describes a set of GUI slot positions using a compact string expression,
 * resolving into concrete slot indices for a given inventory height (rows)
 *
 * <p><b>Grammar</b> (case-insensitive, comma-separated tokens):</p>
 * <ul>
 *   <li><b>Single slot</b>: {@code 0}, {@code 5}, {@code 22}</li>
 *   <li><b>Range</b>: {@code 10-16}</li>
 *   <li><b>Row(s)</b>: {@code row:2} or {@code rows:2-3} (1-based rows; also {@code row:last} / {@code row:-1})</li>
 *   <li><b>Column(s)</b>: {@code col:3} or {@code cols:1-2} (1-based columns)</li>
 *   <li><b>Intersection</b>: {@code row:6 cols:3-7}, {@code rows:2-3 cols:2-8} (box/strip)</li>
 *   <li><b>Specific cell</b>: {@code row:6 col:5} (handled as an intersection of one row & one col)</li>
 * </ul>
 *
 * <p>Rows are clamped to 1..6. Output slots are de-duplicated and sorted</p>
 *
 * <p>This was taken from another project, and will eventually be in a public library</p>
 *
 * @author Cammy
 */
@Getter
@EqualsAndHashCode
public final class SlotSpec {

    /** Original expression as written in config. */
    private final String expr;

    private SlotSpec(@Nullable String expr) {
        this.expr = (expr == null) ? "" : expr.trim();
    }

    public static SlotSpec of(@Nullable String expr) {
        return new SlotSpec(expr);
    }

    /**
     * Resolve this spec to concrete slot indices for a GUI with the given row count
     *
     * @param rows number of inventory rows (1..6)
     * @return list of slot indices suitable for Bukkit inventory APIs
     */
    public List<Integer> resolve(int rows) {
        return parse(expr, rows);
    }

    @Override
    public String toString() {
        return expr;
    }

    // =================
    // Internal parsing
    // =================

    private static List<Integer> parse(@Nullable String spec, int rows) {
        if (spec == null || spec.isBlank()) return List.of();

        final int rowCount = Math.max(1, Math.min(6, rows));
        final int size = rowCount * 9;

        BitSet bs = new BitSet(size);
        for (String token : spec.split(",")) {
            String t = token.trim().toLowerCase(Locale.ROOT);
            if (t.isEmpty()) continue;

            // Intersection first: "row:X cols:A-B" / "rows:X-Y cols:A-B" / "row:last col:5"
            if (hasRowSel(t) && hasColSel(t)) {
                int[] rowsSel = resolveRows(t, rowCount);
                int[] colsSel = resolveCols(t);
                for (int r : rowsSel) {
                    for (int c : colsSel) {
                        int slot = rowColToSlot(r, c, size);
                        if (slot >= 0) bs.set(slot);
                    }
                }
                continue;
            }

            // Full rows: "row:2" or "rows:2-3"
            if (t.startsWith("row:") || t.startsWith("rows:")) {
                String range = t.substring(t.indexOf(':') + 1);
                int[] rowsSel;
                if (t.startsWith("row:")) {
                    int single = parseRowIndex(range, rowCount);
                    rowsSel = (single >= 1 && single <= rowCount) ? new int[]{single} : new int[0];
                } else {
                    rowsSel = parseRange(range);
                }
                for (int r : rowsSel) {
                    if (r < 1 || r > rowCount) continue;
                    int start = (r - 1) * 9;
                    bs.set(start, start + 9);
                }
                continue;
            }

            // Full columns: "col:3" or "cols:1-2"
            if (t.startsWith("col:") || t.startsWith("cols:")) {
                String range = t.substring(t.indexOf(':') + 1);
                int[] colsSel = parseRange(range);
                for (int c : colsSel) {
                    if (c < 1 || c > 9) continue;
                    int col = c - 1;
                    for (int r = 0; r < rowCount; r++) bs.set(r * 9 + col);
                }
                continue;
            }

            // Numeric slot range "10-16" or single "5"
            if (t.contains("-")) {
                String[] ab = t.split("-", 2);
                int a = parseInt(ab[0], -1), b = parseInt(ab[1], -1);
                if (a >= 0 && b >= 0) {
                    int low = Math.min(a, b), high = Math.max(a, b);
                    for (int i = low; i <= high && i < size; i++) bs.set(i);
                }
            } else {
                int i = parseInt(t, -1);
                if (i >= 0 && i < size) bs.set(i);
            }
        }

        List<Integer> out = new ArrayList<>(bs.cardinality());
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) out.add(i);
        return out;
    }

    // ----------------
    // Token utilities
    // ----------------

    private static boolean hasRowSel(String t) {
        return t.contains("row:") || t.contains("rows:");
    }

    private static boolean hasColSel(String t) {
        return t.contains("col:") || t.contains("cols:");
    }

    private static String afterKey(String t, String key) {
        int i = t.indexOf(key);
        if (i < 0) return "";
        int from = i + key.length();
        int to = t.indexOf(' ', from);
        return (to < 0) ? t.substring(from) : t.substring(from, to);
    }

    /** Supports {@code "last"} and {@code "-1"} as the final row */
    private static int parseRowIndex(String raw, int rowCount) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.equals("last") || s.equals("-1")) return rowCount;
        return parseInt(s, -1);
    }

    private static int[] resolveRows(String t, int rowCount) {
        if (t.contains("row:")) {
            int r = parseRowIndex(afterKey(t, "row:"), rowCount);
            return (r >= 1 && r <= rowCount) ? new int[]{r} : new int[0];
        }
        if (t.contains("rows:")) {
            int[] base = parseRange(afterKey(t, "rows:"));
            List<Integer> filtered = new ArrayList<>();
            for (int r : base) if (r >= 1 && r <= rowCount) filtered.add(r);
            return filtered.stream().mapToInt(Integer::intValue).toArray();
        }
        return new int[0];
    }

    private static int[] resolveCols(String t) {
        if (t.contains("col:")) {
            int c = parseInt(afterKey(t, "col:"), -1);
            return (c >= 1 && c <= 9) ? new int[]{c} : new int[0];
        }
        if (t.contains("cols:")) {
            int[] base = parseRange(afterKey(t, "cols:"));
            List<Integer> filtered = new ArrayList<>();
            for (int c : base) if (c >= 1 && c <= 9) filtered.add(c);
            return filtered.stream().mapToInt(Integer::intValue).toArray();
        }
        return new int[0];
    }

    // -------------
    // Math helpers
    // -------------

    private static int rowColToSlot(int row, int col, int size) {
        int rows = size / 9;
        if (row < 1 || row > rows || col < 1 || col > 9) return -1;
        return (row - 1) * 9 + (col - 1);
    }

    /** Parse "a-b" or "n" into an int array (no clamping here) */
    private static int[] parseRange(String range) {
        String r = range.trim();
        if (r.contains("-")) {
            String[] ab = r.split("-", 2);
            int a = parseInt(ab[0], -1), b = parseInt(ab[1], -1);
            if (a < 0 || b < 0) return new int[0];

            int low = Math.min(a, b), high = Math.max(a, b);
            int[] out = new int[high - low + 1];
            for (int i = 0, val = low; val <= high; val++, i++) out[i] = val;
            return out;
        }
        int val = parseInt(r, -1);
        return (val < 0) ? new int[0] : new int[]{val};
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
