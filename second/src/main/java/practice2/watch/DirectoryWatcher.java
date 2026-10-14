package practice2.watch;

import practice2.checksum.ChecksumCalculator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Stream;

public final class DirectoryWatcher {
    private final Path directory;
    private final Map<Path, FileSnapshot> snapshots = new HashMap<>();

    public DirectoryWatcher(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    public void watch(int maxEvents) throws IOException, InterruptedException {
        Files.createDirectories(directory);

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            directory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            initialScan();

            System.out.println("наблюдаем за " + directory);
            int handledEvents = 0;

            while (maxEvents <= 0 || handledEvents < maxEvents) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        System.err.println("часть событий файловой системы потеряна");
                        initialScan();
                        continue;
                    }

                    Path relativePath = (Path) event.context();
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        handleCreate(relativePath);
                    } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        handleModify(relativePath);
                    } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                        handleDelete(relativePath);
                    }
                    handledEvents++;
                    if (maxEvents > 0 && handledEvents >= maxEvents) {
                        break;
                    }
                }

                if (!key.reset()) {
                    System.out.println("каталог больше недоступен");
                    return;
                }
            }
        }
    }

    private void initialScan() throws IOException {
        snapshots.clear();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                snapshots.put(file.getFileName(), readSnapshot(file));
            }
        }
    }

    private void handleCreate(Path relativePath) {
        Path file = directory.resolve(relativePath);
        FileSnapshot snapshot = tryReadStableSnapshot(file);
        if (snapshot != null) {
            snapshots.put(relativePath, snapshot);
            System.out.println("создан файл: " + relativePath);
        }
    }

    private void handleModify(Path relativePath) {
        Path file = directory.resolve(relativePath);
        FileSnapshot newSnapshot = tryReadStableSnapshot(file);
        if (newSnapshot == null) {
            return;
        }

        FileSnapshot oldSnapshot = snapshots.put(relativePath, newSnapshot);
        if (oldSnapshot == null) {
            System.out.println("обнаружен файл: " + relativePath);
            return;
        }
        if (oldSnapshot.equals(newSnapshot)) {
            return;
        }

        LineDiff diff = LineDiff.between(oldSnapshot.lines(), newSnapshot.lines());
        System.out.println("изменен файл: " + relativePath);
        diff.added().forEach(line -> System.out.println("+ " + line));
        diff.removed().forEach(line -> System.out.println("- " + line));
    }

    private void handleDelete(Path relativePath) {
        FileSnapshot snapshot = snapshots.remove(relativePath);
        System.out.println("удален файл: " + relativePath);
        if (snapshot == null) {
            System.out.println("предыдущее состояние файла неизвестно");
            return;
        }

        System.out.printf(
                "последний размер: %d байт checksum: 0x%04X%n",
                snapshot.size(), snapshot.checksum());
    }

    private FileSnapshot tryReadSnapshot(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return readSnapshot(file);
        } catch (IOException exception) {
            System.err.println("не удалось прочитать " + file + ": " + exception.getMessage());
            return null;
        }
    }

    private FileSnapshot tryReadStableSnapshot(Path file) {
        FileSnapshot previous = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            FileSnapshot current = tryReadSnapshot(file);
            if (current == null) {
                return previous;
            }
            if (current.equals(previous)) {
                return current;
            }
            previous = current;
            LockSupport.parkNanos(50_000_000L);
        }
        return previous;
    }

    private static FileSnapshot readSnapshot(Path file) throws IOException {
        long size = Files.size(file);
        int checksum = ChecksumCalculator.calculate(file);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return new FileSnapshot(size, checksum, lines);
    }
}
