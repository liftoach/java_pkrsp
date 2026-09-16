package practice;

import java.util.List;

public final class ArrayProcessorTest {
    private ArrayProcessorTest() {
    }

    public static void main(String[] args) {
        List<ArrayProcessor> processors = List.of(
                new SequentialArrayProcessor(),
                new ThreadArrayProcessor(4),
                new FutureArrayProcessor(4),
                new ForkJoinArrayProcessor(4, 2));

        try {
            check(processors, new int[0], 0);
            check(processors, new int[] {-4, -2, 0, 1, 2, 3, 4}, 20);
            check(processors, new int[] {2}, 4);
            check(processors, new int[] {1, 3, 5, 7}, 0);
            System.out.println("Все тесты успешно пройдены.");
        } finally {
            processors.forEach(ArrayProcessor::close);
        }
    }

    private static void check(List<ArrayProcessor> processors, int[] values, long expected) {
        for (ArrayProcessor processor : processors) {
            long actual = processor.calculate(values);
            if (actual != expected) {
                throw new AssertionError(
                        processor.name() + ": ожидалось " + expected + ", получено " + actual);
            }
        }
    }
}
