package practice2.watch;

import java.util.List;

public record FileSnapshot(long size, int checksum, List<String> lines) {
    public FileSnapshot {
        lines = List.copyOf(lines);
    }
}
