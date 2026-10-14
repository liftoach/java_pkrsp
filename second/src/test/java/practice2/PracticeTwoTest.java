package practice2;

import practice2.checksum.ChecksumCalculator;
import practice2.copy.CopyMethod;
import practice2.copy.FileCopyService;
import practice2.text.TextFileNioMain;
import practice2.watch.LineDiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class PracticeTwoTest {
    private PracticeTwoTest() {
    }

    public static void main(String[] args) throws IOException {
        Path directory = Files.createTempDirectory("practice2-test-");
        try {
            testTextFile(directory);
            testChecksum(directory);
            testCopyMethods(directory);
            testLineDiff();
            System.out.println("Все тесты практической №2 успешно пройдены");
        } finally {
            deleteRecursively(directory);
        }
    }

    private static void testTextFile(Path directory) throws IOException {
        Path file = directory.resolve("sample.txt");
        TextFileNioMain.writeSample(file);
        List<String> lines = TextFileNioMain.readLines(file);
        require(lines.size() == 3, "В текстовом файле должно быть три строки");
    }

    private static void testChecksum(Path directory) throws IOException {
        Path file = directory.resolve("checksum.bin");
        Files.write(file, new byte[] {0x12, 0x34, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF});
        require(
                ChecksumCalculator.calculate(file) == 0x56F9,
                "Неверная 16-битная контрольная сумма");
    }

    private static void testCopyMethods(Path directory) throws IOException {
        Path source = directory.resolve("source.bin");
        FileCopyService.createFile(source, 256 * 1024L);

        for (CopyMethod method : CopyMethod.values()) {
            Path destination = directory.resolve(method.name() + ".bin");
            FileCopyService.copy(method, source, destination);
            require(
                    Files.mismatch(source, destination) == -1,
                    "Ошибка копирования методом " + method.title());
        }
    }

    private static void testLineDiff() {
        LineDiff diff = LineDiff.between(
                List.of("a", "b", "b"),
                List.of("b", "c", "b"));
        require(diff.added().equals(List.of("c")), "Неверный список добавленных строк");
        require(diff.removed().equals(List.of("a")), "Неверный список удаленных строк");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
