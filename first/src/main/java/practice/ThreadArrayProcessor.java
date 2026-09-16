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

        // потоков больше чем элементов не создаем
        int workersCount = Math.min(threadCount, values.length);
        Worker[] workers = new Worker[workersCount];

        // размер куска с округлением вверх
        int partSize = (values.length + workersCount - 1) / workersCount;

        // каждому потоку даем свой кусок массива и сразу запускаем
        for (int i = 0; i < workersCount; i++) {
            int from = i * partSize;
            int to = Math.min(from + partSize, values.length);
            workers[i] = new Worker(values, from, to);
            workers[i].start();
        }

        long result = 0;
        try {
            // ждем все потоки и складываем их результаты
            for (Worker worker : workers) {
                worker.join();
                result += worker.result;
            }
        } catch (InterruptedException exception) {
            // возвращаем флаг прерывания чтобы код выше тоже его увидел
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
            // воркер считает только свой диапазон
            result = RangeCalculator.calculate(values, from, to);
        }
    }
}
