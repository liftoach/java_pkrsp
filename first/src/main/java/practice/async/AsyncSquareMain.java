package practice.async;

import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class AsyncSquareMain {
    private static final int WORKER_COUNT = 5;

    private AsyncSquareMain() {
    }

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(WORKER_COUNT);
        ExecutorCompletionService<SquareResult> completedTasks =
                new ExecutorCompletionService<>(executor);
        AtomicInteger pendingTasks = new AtomicInteger();
        AtomicBoolean inputFinished = new AtomicBoolean();

        Thread resultPrinter = new Thread(
                () -> printResults(completedTasks, pendingTasks, inputFinished),
                "result-printer");
        resultPrinter.start();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Введите целое число или exit для выхода");
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                if ("exit".equalsIgnoreCase(input)) {
                    break;
                }

                try {
                    int number = Integer.parseInt(input);
                    pendingTasks.incrementAndGet();

                    // submit сразу возвращает Future поэтому можно вводить следующее число
                    Future<SquareResult> ignored = completedTasks.submit(
                            () -> calculateSquare(number));
                    System.out.println("запрос принят для числа " + number);
                } catch (NumberFormatException exception) {
                    System.err.println("нужно ввести целое число или exit");
                }
            }
        } finally {
            inputFinished.set(true);
            executor.shutdown();
            waitForPrinter(resultPrinter);
        }
    }

    private static SquareResult calculateSquare(int number) throws InterruptedException {
        int delaySeconds = ThreadLocalRandom.current().nextInt(1, 6);
        Thread.sleep(delaySeconds * 1_000L);
        return new SquareResult(number, (long) number * number, delaySeconds);
    }

    private static void printResults(
            ExecutorCompletionService<SquareResult> completedTasks,
            AtomicInteger pendingTasks,
            AtomicBoolean inputFinished
    ) {
        while (!inputFinished.get() || pendingTasks.get() > 0) {
            try {
                Future<SquareResult> future = completedTasks.poll(200, TimeUnit.MILLISECONDS);
                if (future == null) {
                    continue;
                }

                try {
                    SquareResult result = future.get();
                    System.out.printf(
                            "%nготово: %d² = %d задержка %d сек%n> ",
                            result.number(), result.square(), result.delaySeconds());
                } catch (ExecutionException exception) {
                    System.err.println("ошибка вычисления: " + exception.getCause().getMessage());
                } finally {
                    pendingTasks.decrementAndGet();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void waitForPrinter(Thread resultPrinter) {
        try {
            resultPrinter.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание результатов прервано", exception);
        }
    }

    private record SquareResult(int number, long square, int delaySeconds) {
    }
}
