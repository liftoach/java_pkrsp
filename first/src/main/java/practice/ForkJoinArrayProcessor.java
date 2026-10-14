package practice;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public final class ForkJoinArrayProcessor implements ArrayProcessor {
    private final int parallelism;

    // порог после которого нет смысла дальше дробить задачу
    private final int threshold;
    private final ArrayOperation operation;
    private final ForkJoinPool pool;

    public ForkJoinArrayProcessor(int parallelism, int threshold, ArrayOperation operation) {
        if (parallelism <= 0 || threshold <= 0) {
            throw new IllegalArgumentException("Параллелизм и порог должны быть положительными");
        }
        this.parallelism = parallelism;
        this.threshold = threshold;
        this.operation = operation;
        this.pool = new ForkJoinPool(parallelism);
    }

    @Override
    public long calculate(int[] values) {
        // запускаем задачу на весь массив и ждем результат
        return pool.invoke(new ArrayTask(values, 0, values.length, threshold, operation));
    }

    @Override
    public String name() {
        return "ForkJoin (" + parallelism + ")";
    }

    @Override
    public void close() {
        pool.shutdownNow();
    }

    private static final class ArrayTask extends RecursiveTask<Long> {
        private final int[] values;
        private final int from;
        private final int to;
        private final int threshold;
        private final ArrayOperation operation;

        private ArrayTask(
                int[] values,
                int from,
                int to,
                int threshold,
                ArrayOperation operation
        ) {
            this.values = values;
            this.from = from;
            this.to = to;
            this.threshold = threshold;
            this.operation = operation;
        }

        @Override
        protected Long compute() {
            // маленький кусок быстрее посчитать обычным циклом
            if (to - from <= threshold) {
                return RangeCalculator.calculate(values, from, to, operation);
            }

            int middle = from + (to - from) / 2;
            ArrayTask left = new ArrayTask(values, from, middle, threshold, operation);
            ArrayTask right = new ArrayTask(values, middle, to, threshold, operation);

            // левую часть отдаем пулу правую считаем сами
            left.fork();
            long rightResult = right.compute();

            // ждем левую часть и складываем результаты
            return operation.combine(left.join(), rightResult);
        }
    }
}
