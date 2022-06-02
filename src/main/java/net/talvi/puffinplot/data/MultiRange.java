package net.talvi.puffinplot.data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents multiple ranges of floating-point values.
 */
public class MultiRange {
    private final List<Range> ranges;

    private MultiRange(List<Range> ranges) {
        this.ranges = ranges;
    }

    /**
     * Initialize a multirange from a string specification, e.g.
     * "5.5", "11.3-19", or "2.5,17-55.99,1000-1010,4".
     *
     * @param s a string specifying ranges
     * @return the MultiRange defined by the supplied string
     */
    public static MultiRange fromString(String s) {
        return new MultiRange(Arrays.stream(s.split(","))
                .map(Range::fromString).collect(Collectors.toList()));
    }

    /**
     * Determine if a value is within one of the ranges of this multirange.
     *
     * @param value a value to check
     * @return true iff the value is within one of this multirange's ranges
     */
    public boolean contains(double value) {
        return ranges.stream().anyMatch(range -> range.contains(value));
    }

    private static class Range {
        public final double from;
        public final double to;

        public Range(double from, double to) {
            this.from = from;
            this.to = to;
        }

        public static Range fromString(String s) {
            if (s.contains("-")) {
                final String[] parts = s.split("-");
                return new Range(Double.parseDouble(parts[0]),
                        Double.parseDouble(parts[1]));
            } else {
                final double value = Double.parseDouble(s);
                return new Range(value, value);
            }
        }

        public boolean contains(double value) {
            return from <= value && value <= to;
        }
    }
}
