package practice;

final class RangeCalculator {
    private RangeCalculator() {
    }

    static long calculate(int[] values, int fromInclusive, int toExclusive) {
        long result = 0;
        for (int i = fromInclusive; i < toExclusive; i++) {
            int value = values[i];
            if (value > 0 && value % 2 == 0) {
                result += (long) value * value;
            }
        }
        return result;
    }
}
