package practice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class FutureArrayProcessor implements ArrayProcessor {
    private final int threadCount;
    private final ArrayOperation operation;
    private final ExecutorService executor;

    public FutureArrayProcessor(int threadCount, ArrayOperation operation) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("Количество потоков должно быть положительным");
        }
        this.threadCount = threadCount;
        this.operation = operation;

        // создаем фиксированный пул и переиспользуем его потоки
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    @Override
    public long calculate(int[] values) {
        if (values.length == 0) {
            return RangeCalculator.calculate(values, 0, 0, operation);
        }

        // задач больше чем элементов не создаем
        int tasksCount = Math.min(threadCount, values.length);
        int partSize = (values.length + tasksCount - 1) / tasksCount;
        List<Future<Long>> futures = new ArrayList<>(tasksCount);

        // отправляем каждый кусок в пул и сохраняем будущий результат
        for (int i = 0; i < tasksCount; i++) {
            int from = i * partSize;
            int to = Math.min(from + partSize, values.length);
            futures.add(executor.submit(
                    () -> RangeCalculator.calculate(values, from, to, operation)
            ));
        }

        long result = 0;
        boolean first = true;
        try {
            // get ждет задачу и возвращает ее результат
            for (Future<Long> future : futures) {
                long partialResult = future.get();
                result = first ? partialResult : operation.combine(result, partialResult);
                first = false;
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
        // если одна задача упала остальные тоже больше не нужны
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
        // останавливаем потоки пула после работы
        executor.shutdownNow();
    }
}
