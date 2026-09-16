package practice;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public final class ForkJoinArrayProcessor implements ArrayProcessor {
    private final int parallelism;
    private final int threshold;
    private final ForkJoinPool pool;

    public ForkJoinArrayProcessor(int parallelism, int threshold) {
        if (parallelism <= 0 || threshold <= 0) {
            throw new IllegalArgumentException("Параллелизм и порог должны быть положительными");
        }
        this.parallelism = parallelism;
        this.threshold = threshold;
        this.pool = new ForkJoinPool(parallelism);
    }

    @Override
    public long calculate(int[] values) {
        return pool.invoke(new SumTask(values, 0, values.length, threshold));
    }

    @Override
    public String name() {
        return "ForkJoin (" + parallelism + ")";
    }

    @Override
    public void close() {
        pool.shutdownNow();
    }

    private static final class SumTask extends RecursiveTask<Long> {
        private final int[] values;
        private final int from;
        private final int to;
        private final int threshold;

        private SumTask(int[] values, int from, int to, int threshold) {
            this.values = values;
            this.from = from;
            this.to = to;
            this.threshold = threshold;
        }

        @Override
        protected Long compute() {
            if (to - from <= threshold) {
                return RangeCalculator.calculate(values, from, to);
            }

            int middle = from + (to - from) / 2;
            SumTask left = new SumTask(values, from, middle, threshold);
            SumTask right = new SumTask(values, middle, to, threshold);
            left.fork();
            long rightResult = right.compute();
            return left.join() + rightResult;
        }
    }
}
