package practice2.checksum;

import practice2.text.TextFileNioMain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChecksumMain {
    private ChecksumMain() {
    }

    public static void main(String[] args) throws IOException {
        boolean defaultFile = args.length == 0;
        Path file = defaultFile
                ? Path.of("data", "text", "sample.txt")
                : Path.of(args[0]);

        if (defaultFile && Files.notExists(file)) {
            TextFileNioMain.writeSample(file);
            System.out.println("стандартный файл не найден поэтому создали "
                    + file.toAbsolutePath().normalize());
        } else if (Files.notExists(file)) {
            throw new IllegalArgumentException(
                    "Файл не найден: " + file.toAbsolutePath().normalize());
        }

        int checksum = ChecksumCalculator.calculate(file);
        System.out.printf(
                "16-битная checksum файла %s: 0x%04X (%d)%n",
                file.toAbsolutePath().normalize(), checksum, checksum);
    }
}
