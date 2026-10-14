package practice.files;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

public final class FileDispatcher implements Runnable {
    private final BlockingQueue<GeneratedFile> inputQueue;
    private final Map<FileType, BlockingQueue<GeneratedFile>> processorQueues;
    private final int fileCount;

    public FileDispatcher(
            BlockingQueue<GeneratedFile> inputQueue,
            Map<FileType, BlockingQueue<GeneratedFile>> processorQueues,
            int fileCount
    ) {
        this.inputQueue = inputQueue;
        this.processorQueues = processorQueues;
        this.fileCount = fileCount;
    }

    @Override
    public void run() {
        try {
            // отдельный диспетчер не дает обработчику забрать файл чужого типа
            for (int i = 0; i < fileCount; i++) {
                GeneratedFile file = inputQueue.take();
                processorQueues.get(file.type()).put(file);
            }

            for (FileType type : FileType.values()) {
                processorQueues.get(type).put(GeneratedFile.poison(type));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
