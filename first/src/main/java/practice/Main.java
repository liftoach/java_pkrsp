package practice;

import java.util.List;
import java.util.Random;

public final class Main {
    private static final int DEFAULT_SIZE = 5_000_000;
    private static final int DEFAULT_REPETITIONS = 5;

    private Main() {
    }

    public static void main(String[] args) {
        int size = parsePositiveArg(args, 0, DEFAULT_SIZE, "размер массива");
        int threadCount = parsePositiveArg(args, 1, Runtime.getRuntime().availableProcessors(), "количество потоков");
        int repetitions = parsePositiveArg(args, 2, DEFAULT_REPETITIONS, "число повторений");

        int[] values = createArray(size, 42L);
        int threshold = Math.max(10_000, size / Math.max(1, threadCount * 8));

        List<ArrayProcessor> processors = List.of(
                new SequentialArrayProcessor(),
                new ThreadArrayProcessor(threadCount),
                new FutureArrayProcessor(threadCount),
                new ForkJoinArrayProcessor(threadCount, threshold));

        System.out.printf("Элементов: %,d; потоков: %d; повторений: %d%n%n",
                size, threadCount, repetitions);

        try {
            long expected = processors.get(0).calculate(values);
            for (ArrayProcessor processor : processors) {
                long actual = processor.calculate(values);
                if (actual != expected) {
                    throw new IllegalStateException(
                            processor.name() + " вернул " + actual + ", ожидалось " + expected);
                }

                double milliseconds = benchmark(processor, values, repetitions);
                System.out.printf("%-20s результат: %d, среднее время: %.3f мс%n",
                        processor.name(), actual, milliseconds);
            }
        } finally {
            processors.forEach(ArrayProcessor::close);
        }
    }

    private static double benchmark(ArrayProcessor processor, int[] values, int repetitions) {
        long totalNanos = 0;
        for (int i = 0; i < repetitions; i++) {
            long started = System.nanoTime();
            processor.calculate(values);
            totalNanos += System.nanoTime() - started;
        }
        return totalNanos / (double) repetitions / 1_000_000.0;
    }

    private static int[] createArray(int size, long seed) {
        Random random = new Random(seed);
        int[] values = new int[size];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextInt(2_001) - 1_000;
        }
        return values;
    }

    private static int parsePositiveArg(String[] args, int index, int defaultValue, String label) {
        if (args.length <= index) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(args[index]);
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Некорректный аргумент (" + label + "): " + args[index]);
        }
    }
}
