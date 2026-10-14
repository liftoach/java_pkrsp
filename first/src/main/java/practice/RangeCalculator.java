package practice;

final class RangeCalculator {
    private RangeCalculator() {
    }

    static long calculate(
            int[] values,
            int fromInclusive,
            int toExclusive,
            ArrayOperation operation
    ) {
        return operation.calculateRange(values, fromInclusive, toExclusive);
    }
}
