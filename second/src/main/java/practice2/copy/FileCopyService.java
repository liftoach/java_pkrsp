package practice2.copy;

import org.apache.commons.io.FileUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FileCopyService {
    private static final int BUFFER_SIZE = 64 * 1024;

    private FileCopyService() {
    }

    public static void createFile(Path file, long sizeBytes) throws IOException {
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("Размер файла не может быть отрицательным");
        }
        Path parent = file.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        byte[] block = new byte[1024 * 1024];
        for (int i = 0; i < block.length; i++) {
            block[i] = (byte) (i * 31 + 17);
        }

        try (OutputStream output = Files.newOutputStream(file)) {
            long remaining = sizeBytes;
            while (remaining > 0) {
                int length = (int) Math.min(block.length, remaining);
                output.write(block, 0, length);
                remaining -= length;
            }
        }
    }

    public static void copy(CopyMethod method, Path source, Path destination) throws IOException {
        Path parent = destination.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        switch (method) {
            case FILE_STREAMS -> copyUsingStreams(source, destination);
            case FILE_CHANNEL -> copyUsingFileChannel(source, destination);
            case APACHE_COMMONS_IO -> FileUtils.copyFile(source.toFile(), destination.toFile());
            case FILES_CLASS -> Files.copy(
                    source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copyUsingStreams(Path source, Path destination) throws IOException {
        try (FileInputStream input = new FileInputStream(source.toFile());
             FileOutputStream output = new FileOutputStream(destination.toFile())) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
    }

    private static void copyUsingFileChannel(Path source, Path destination) throws IOException {
        try (FileChannel input = FileChannel.open(source);
             FileChannel output = FileChannel.open(
                     destination,
                     java.nio.file.StandardOpenOption.CREATE,
                     java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                     java.nio.file.StandardOpenOption.WRITE)) {
            long position = 0;
            while (position < input.size()) {
                long transferred = input.transferTo(position, input.size() - position, output);
                if (transferred <= 0) {
                    throw new IOException("FileChannel не смог продолжить копирование");
                }
                position += transferred;
            }
        }
    }
}
