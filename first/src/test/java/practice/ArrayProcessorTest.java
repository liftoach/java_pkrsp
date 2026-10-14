package practice;

import java.util.List;

public final class ArrayProcessorTest {
    private ArrayProcessorTest() {
    }

    public static void main(String[] args) {
        checkOperation(ArrayOperation.SUM, 4);
        checkOperation(ArrayOperation.MAX, 4);
        checkOperation(ArrayOperation.MIN, -4);
        checkSingleElement();
        System.out.println("Все тесты успешно пройдены");
    }

    private static void checkOperation(ArrayOperation operation, long expected) {
        int[] values = {-4, -2, 0, 1, 2, 3, 4};
        check(createProcessors(operation), values, expected);
    }

    private static void checkSingleElement() {
        for (ArrayOperation operation : ArrayOperation.values()) {
            check(createProcessors(operation), new int[] {7}, 7);
        }
    }

    private static List<ArrayProcessor> createProcessors(ArrayOperation operation) {
        return List.of(
                new SequentialArrayProcessor(operation),
                new ThreadArrayProcessor(4, operation),
                new FutureArrayProcessor(4, operation),
                new ForkJoinArrayProcessor(4, 2, operation));
    }

    private static void check(
            List<ArrayProcessor> processors,
            int[] values,
            long expected
    ) {
        try {
            for (ArrayProcessor processor : processors) {
                long actual = processor.calculate(values);
                if (actual != expected) {
                    throw new AssertionError(
                            processor.name() + ": ожидалось " + expected + ", получено " + actual);
                }
            }
        } finally {
            processors.forEach(ArrayProcessor::close);
        }
    }
}
