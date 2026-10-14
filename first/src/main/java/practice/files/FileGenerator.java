package practice.files;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

public final class FileGenerator implements Runnable {
    private final BlockingQueue<GeneratedFile> outputQueue;
    private final int fileCount;
    private final Random random;

    public FileGenerator(BlockingQueue<GeneratedFile> outputQueue, int fileCount, long seed) {
        this.outputQueue = outputQueue;
        this.fileCount = fileCount;
        this.random = new Random(seed);
    }

    @Override
    public void run() {
        FileType[] types = FileType.values();
        try {
            for (int id = 1; id <= fileCount; id++) {
                Thread.sleep(random.nextInt(901) + 100L);
                GeneratedFile file = new GeneratedFile(
                        id,
                        types[random.nextInt(types.length)],
                        random.nextInt(91) + 10,
                        false);

                // put сам ждет свободное место если очередь уже заполнена
                outputQueue.put(file);
                System.out.printf("создан файл #%d %s размер %d%n", id, file.type(), file.size());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
