package practice;

public enum ArrayOperation {
    SUM("сумма") {
        @Override
        long calculateRange(int[] values, int from, int to) {
            long result = 0;
            for (int i = from; i < to; i++) {
                result += values[i];
                pause();
            }
            return result;
        }

        @Override
        long combine(long left, long right) {
            long result = left + right;
            pause();
            return result;
        }
    },
    MAX("максимум") {
        @Override
        long calculateRange(int[] values, int from, int to) {
            requireNotEmpty(from, to);
            long result = values[from];
            for (int i = from + 1; i < to; i++) {
                result = Math.max(result, values[i]);
                pause();
            }
            return result;
        }

        @Override
        long combine(long left, long right) {
            long result = Math.max(left, right);
            pause();
            return result;
        }
    },
    MIN("минимум") {
        @Override
        long calculateRange(int[] values, int from, int to) {
            requireNotEmpty(from, to);
            long result = values[from];
            for (int i = from + 1; i < to; i++) {
                result = Math.min(result, values[i]);
                pause();
            }
            return result;
        }

        @Override
        long combine(long left, long right) {
            long result = Math.min(left, right);
            pause();
            return result;
        }
    };

    private final String title;

    ArrayOperation(String title) {
        this.title = title;
    }

    abstract long calculateRange(int[] values, int from, int to);

    abstract long combine(long left, long right);

    public String title() {
        return title;
    }

    private static void requireNotEmpty(int from, int to) {
        if (from >= to) {
            throw new IllegalArgumentException("Для поиска минимума или максимума массив не должен быть пустым");
        }
    }

    // по условию после каждой операции ждем 1 мс
    static void pause() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Вычисление прервано", exception);
        }
    }
}
