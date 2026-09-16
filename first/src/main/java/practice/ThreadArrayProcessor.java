package practice;

public final class ThreadArrayProcessor implements ArrayProcessor {
    private final int threadCount;

    public ThreadArrayProcessor(int threadCount) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("Количество потоков должно быть положительным");
        }
        this.threadCount = threadCount;
    }

    @Override
    public long calculate(int[] values) {
        if (values.length == 0) {
            return 0;
        }

        int workersCount = Math.min(threadCount, values.length);
        Worker[] workers = new Worker[workersCount];
        int partSize = (values.length + workersCount - 1) / workersCount;

        for (int i = 0; i < workersCount; i++) {
            int from = i * partSize;
            int to = Math.min(from + partSize, values.length);
            workers[i] = new Worker(values, from, to);
            workers[i].start();
        }

        long result = 0;
        try {
            for (Worker worker : workers) {
                worker.join();
                result += worker.result;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание рабочих потоков прервано", exception);
        }
        return result;
    }

    @Override
    public String name() {
        return "Thread (" + threadCount + ")";
    }

    private static final class Worker extends Thread {
        private final int[] values;
        private final int from;
        private final int to;
        private long result;

        private Worker(int[] values, int from, int to) {
            this.values = values;
            this.from = from;
            this.to = to;
        }

        @Override
        public void run() {
            result = RangeCalculator.calculate(values, from, to);
        }
    }
}
