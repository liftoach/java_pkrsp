package practice2.copy;

import practice2.checksum.ChecksumCalculator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public final class FileCopyBenchmarkMain {
    private static final int DEFAULT_SIZE_MB = 100;

    private FileCopyBenchmarkMain() {
    }

    public static void main(String[] args) throws IOException {
        int sizeMb = parseSize(args);
        Path directory = args.length >= 2
                ? Path.of(args[1])
                : Path.of("data", "copy");
        Files.createDirectories(directory);

        Path source = directory.resolve("source-" + sizeMb + "mb.bin");
        long expectedSize = sizeMb * 1024L * 1024L;
        System.out.printf("создаем исходный файл %,d МБ%n", sizeMb);
        FileCopyService.createFile(source, expectedSize);
        int sourceChecksum = ChecksumCalculator.calculate(source);

        List<CopyResult> results = new ArrayList<>();
        for (CopyMethod method : CopyMethod.values()) {
            Path destination = directory.resolve(method.name().toLowerCase() + ".bin");
            Files.deleteIfExists(destination);
            CopyResult result = measure(method, source, destination);
            verifyCopy(destination, expectedSize, sourceChecksum);
            results.add(result);
            System.out.printf(
                    "%-32s %10.3f мс  пик heap: %8.1f КиБ%n",
                    method.title(), result.milliseconds(), result.peakHeapBytes() / 1024.0);
        }

        CopyResult fastest = results.stream()
                .min(Comparator.comparingDouble(CopyResult::milliseconds))
                .orElseThrow();
        CopyResult lowestHeap = results.stream()
                .min(Comparator.comparingLong(CopyResult::peakHeapBytes))
                .orElseThrow();

        System.out.println("\nбыстрее в этом запуске: " + fastest.method().title());
        System.out.println("минимальный рост heap: " + lowestHeap.method().title());
        System.out.println("результат зависит от диска os cache jvm и фоновой нагрузки");
    }

    private static CopyResult measure(
            CopyMethod method,
            Path source,
            Path destination
    ) throws IOException {
        HeapMonitor monitor = new HeapMonitor();
        monitor.start();
        long started = System.nanoTime();
        long elapsedNanos;
        long peakHeapBytes;
        try {
            FileCopyService.copy(method, source, destination);
        } finally {
            elapsedNanos = System.nanoTime() - started;
            peakHeapBytes = monitor.stopAndGetGrowth();
        }
        return new CopyResult(method, elapsedNanos / 1_000_000.0, peakHeapBytes);
    }

    private static void verifyCopy(Path file, long expectedSize, int expectedChecksum)
            throws IOException {
        long actualSize = Files.size(file);
        int actualChecksum = ChecksumCalculator.calculate(file);
        if (actualSize != expectedSize || actualChecksum != expectedChecksum) {
            throw new IOException("Копия не совпадает с исходным файлом: " + file);
        }
    }

    private static int parseSize(String[] args) {
        if (args.length == 0) {
            return DEFAULT_SIZE_MB;
        }
        try {
            int size = Integer.parseInt(args[0]);
            if (size <= 0) {
                throw new NumberFormatException();
            }
            return size;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Размер должен быть положительным числом мегабайт");
        }
    }

    private record CopyResult(CopyMethod method, double milliseconds, long peakHeapBytes) {
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
