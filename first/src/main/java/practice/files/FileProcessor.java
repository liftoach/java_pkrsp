package practice.files;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class FileProcessor implements Runnable {
    private final FileType allowedType;
    private final BlockingQueue<GeneratedFile> inputQueue;
    private final AtomicInteger processedCount;

    public FileProcessor(
            FileType allowedType,
            BlockingQueue<GeneratedFile> inputQueue,
            AtomicInteger processedCount
    ) {
        this.allowedType = allowedType;
        this.inputQueue = inputQueue;
        this.processedCount = processedCount;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // take блокируется без нагрузки на cpu пока задач нет
                GeneratedFile file = inputQueue.take();
                if (file.poison()) {
                    return;
                }
                if (file.type() != allowedType) {
                    throw new IllegalStateException("Обработчик получил файл чужого типа");
                }

                long processingTime = file.size() * 7L;
                Thread.sleep(processingTime);
                processedCount.incrementAndGet();
                System.out.printf(
                        "обработан файл #%d %s размер %d за %d мс%n",
                        file.id(), file.type(), file.size(), processingTime);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
