package practice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class FutureArrayProcessor implements ArrayProcessor {
    private final int threadCount;
    private final ExecutorService executor;

    public FutureArrayProcessor(int threadCount) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("Количество потоков должно быть положительным");
        }
        this.threadCount = threadCount;
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    @Override
    public long calculate(int[] values) {
        if (values.length == 0) {
            return 0;
        }

        int tasksCount = Math.min(threadCount, values.length);
        int partSize = (values.length + tasksCount - 1) / tasksCount;
        List<Future<Long>> futures = new ArrayList<>(tasksCount);

        for (int i = 0; i < tasksCount; i++) {
            int from = i * partSize;
            int to = Math.min(from + partSize, values.length);
            futures.add(executor.submit(() -> RangeCalculator.calculate(values, from, to)));
        }

        long result = 0;
        try {
            for (Future<Long> future : futures) {
                result += future.get();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            cancel(futures);
            throw new IllegalStateException("Ожидание Future прервано", exception);
        } catch (ExecutionException exception) {
            cancel(futures);
            throw new IllegalStateException("Ошибка асинхронного вычисления", exception.getCause());
        }
        return result;
    }

    private static void cancel(List<Future<Long>> futures) {
        for (Future<Long> future : futures) {
            future.cancel(true);
        }
    }

    @Override
    public String name() {
        return "Future (" + threadCount + ")";
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
