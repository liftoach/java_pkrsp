package practice;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public final class Main {
    private static final int DEFAULT_SIZE = 10_000;
    private static final int DEFAULT_REPETITIONS = 1;

    private Main() {
    }

    public static void main(String[] args) {
        ArrayOperation operation = parseOperation(args, 0, ArrayOperation.MAX);
        int threadCount = parsePositiveArg(
                args, 1, Runtime.getRuntime().availableProcessors(), "количество потоков");
        int repetitions = parsePositiveArg(args, 2, DEFAULT_REPETITIONS, "число повторений");
        int size = parsePositiveArg(args, 3, DEFAULT_SIZE, "размер массива");

        int[] values = createArray(size, 42L);
        int threshold = Math.max(100, size / Math.max(1, threadCount * 8));

        List<ArrayProcessor> processors = List.of(
                new SequentialArrayProcessor(operation),
                new ThreadArrayProcessor(threadCount, operation),
                new FutureArrayProcessor(threadCount, operation),
                new ForkJoinArrayProcessor(threadCount, threshold, operation));

        System.out.printf(
                "Операция: %s; элементов: %,d; потоков: %d; повторений: %d%n%n",
                operation.title(), size, threadCount, repetitions);

        Long expected = null;
        try {
            for (ArrayProcessor processor : processors) {
                Measurement measurement = measure(processor, values, repetitions);
                if (expected == null) {
                    expected = measurement.result();
                } else if (measurement.result() != expected) {
                    throw new IllegalStateException(
                            processor.name() + " вернул " + measurement.result()
                                    + ", ожидалось " + expected);
                }

                System.out.printf(
                        "%-20s результат: %d, среднее время: %.3f мс, пик heap: %.1f КиБ%n",
                        processor.name(),
                        measurement.result(),
                        measurement.averageMilliseconds(),
                        measurement.peakHeapBytes() / 1024.0);
            }
        } finally {
            processors.forEach(ArrayProcessor::close);
        }

        System.out.println("\nПамять указана приблизительно: измеряется рост занятой heap-памяти JVM");
    }

    private static Measurement measure(
            ArrayProcessor processor,
            int[] values,
            int repetitions
    ) {
        long totalNanos = 0;
        long peakHeapBytes = 0;
        long result = 0;

        for (int i = 0; i < repetitions; i++) {
            HeapMonitor monitor = new HeapMonitor();
            monitor.start();
            long started = System.nanoTime();
            try {
                result = processor.calculate(values);
            } finally {
                totalNanos += System.nanoTime() - started;
                peakHeapBytes = Math.max(peakHeapBytes, monitor.stopAndGetGrowth());
            }
        }

        return new Measurement(
                result,
                totalNanos / (double) repetitions / 1_000_000.0,
                peakHeapBytes);
    }

    private static int[] createArray(int size, long seed) {
        Random random = new Random(seed);
        int[] values = new int[size];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextInt(20_001) - 10_000;
        }
        return values;
    }

    private static ArrayOperation parseOperation(
            String[] args,
            int index,
            ArrayOperation defaultValue
    ) {
        if (args.length <= index) {
            return defaultValue;
        }
        try {
            return ArrayOperation.valueOf(args[index].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Операция должна быть SUM, MAX или MIN");
        }
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

    private record Measurement(long result, double averageMilliseconds, long peakHeapBytes) {
    }

    private static final class HeapMonitor {
        private final Runtime runtime = Runtime.getRuntime();
        private final long baseline = usedHeap();
        private final AtomicLong peak = new AtomicLong(baseline);
        private final AtomicBoolean running = new AtomicBoolean();
        private Thread sampler;

        void start() {
            running.set(true);
            sampler = new Thread(() -> {
                while (running.get()) {
                    peak.accumulateAndGet(usedHeap(), Math::max);
                    LockSupport.parkNanos(1_000_000);
                }
            }, "heap-monitor");
            sampler.setDaemon(true);
            sampler.start();
        }

        long stopAndGetGrowth() {
            running.set(false);
            try {
                sampler.join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Ожидание замера памяти прервано", exception);
            }
            peak.accumulateAndGet(usedHeap(), Math::max);
            return Math.max(0, peak.get() - baseline);
        }

        private long usedHeap() {
            return runtime.totalMemory() - runtime.freeMemory();
        }
    }
}
