package practice2.watch;

import java.io.IOException;
import java.nio.file.Path;

public final class DirectoryWatcherMain {
    private DirectoryWatcherMain() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Path directory = args.length == 0
                ? Path.of("data", "watched")
                : Path.of(args[0]);
        int maxEvents = args.length >= 2 ? parseMaxEvents(args[1]) : 0;

        System.out.println("для остановки нажмите Ctrl+C");
        new DirectoryWatcher(directory).watch(maxEvents);
    }

    private static int parseMaxEvents(String value) {
        try {
            int maxEvents = Integer.parseInt(value);
            if (maxEvents < 0) {
                throw new NumberFormatException();
            }
            return maxEvents;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Число событий должно быть неотрицательным");
        }
    }
}
