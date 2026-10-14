package practice2.text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TextFileNioMain {
    private static final List<String> SAMPLE_LINES = List.of(
            "первая",
            "вторая",
            "третья)");

    private TextFileNioMain() {
    }

    public static void main(String[] args) throws IOException {
        Path file = args.length == 0
                ? Path.of("data", "text", "sample.txt")
                : Path.of(args[0]);

        writeSample(file);
        System.out.println("содержимое " + file.toAbsolutePath().normalize());
        readLines(file).forEach(System.out::println);
    }

    public static void writeSample(Path file) throws IOException {
        Path parent = file.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, SAMPLE_LINES, StandardCharsets.UTF_8);
    }

    public static List<String> readLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }
}
