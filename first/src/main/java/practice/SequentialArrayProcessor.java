package practice;

public final class SequentialArrayProcessor implements ArrayProcessor {
    @Override
    public long calculate(int[] values) {
        return RangeCalculator.calculate(values, 0, values.length);
    }

    @Override
    public String name() {
        return "Последовательно";
    }
}
