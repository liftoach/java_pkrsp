package practice;

public final class SequentialArrayProcessor implements ArrayProcessor {
    private final ArrayOperation operation;

    public SequentialArrayProcessor(ArrayOperation operation) {
        this.operation = operation;
    }

    @Override
    public long calculate(int[] values) {
        return RangeCalculator.calculate(values, 0, values.length, operation);
    }

    @Override
    public String name() {
        return "Последовательно";
    }
}
