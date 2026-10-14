package practice.files;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class FileProcessingMain {
    private static final int DEFAULT_FILE_COUNT = 12;

    private FileProcessingMain() {
    }

    public static void main(String[] args) throws InterruptedException {
        int fileCount = parseFileCount(args);

        // главная очередь по условию вмещает ровно 5 файлов
        BlockingQueue<GeneratedFile> generatedFiles = new ArrayBlockingQueue<>(5);
        Map<FileType, BlockingQueue<GeneratedFile>> processorQueues =
                createProcessorQueues();
        AtomicInteger processedCount = new AtomicInteger();

        Thread generator = new Thread(
                new FileGenerator(generatedFiles, fileCount, 42L),
                "file-generator");
        Thread dispatcher = new Thread(
                new FileDispatcher(generatedFiles, processorQueues, fileCount),
                "file-dispatcher");

        Thread[] processors = new Thread[FileType.values().length];
        for (FileType type : FileType.values()) {
            processors[type.ordinal()] = new Thread(
                    new FileProcessor(type, processorQueues.get(type), processedCount),
                    type + "-processor");
            processors[type.ordinal()].start();
        }

        dispatcher.start();
        generator.start();

        generator.join();
        dispatcher.join();
        for (Thread processor : processors) {
            processor.join();
        }

        if (processedCount.get() != fileCount) {
            throw new IllegalStateException(
                    "Нарушена целостность данных: создано " + fileCount
                            + ", обработано " + processedCount.get());
        }
        System.out.println("готово все " + processedCount.get() + " файлов обработаны");
    }

    private static Map<FileType, BlockingQueue<GeneratedFile>> createProcessorQueues() {
        Map<FileType, BlockingQueue<GeneratedFile>> queues = new EnumMap<>(FileType.class);
        for (FileType type : FileType.values()) {
            queues.put(type, new LinkedBlockingQueue<>());
        }
        return queues;
    }

    private static int parseFileCount(String[] args) {
        if (args.length == 0) {
            return DEFAULT_FILE_COUNT;
        }
        try {
            int count = Integer.parseInt(args[0]);
            if (count <= 0) {
                throw new NumberFormatException();
            }
            return count;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Количество файлов должно быть положительным");
        }
    }
}
