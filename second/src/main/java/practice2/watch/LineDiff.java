package practice2.watch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LineDiff(List<String> added, List<String> removed) {
    public LineDiff {
        added = List.copyOf(added);
        removed = List.copyOf(removed);
    }

    public static LineDiff between(List<String> oldLines, List<String> newLines) {
        return new LineDiff(
                subtract(newLines, oldLines),
                subtract(oldLines, newLines));
    }

    // считаем строки как multiset чтобы одинаковые строки не терялись
    private static List<String> subtract(List<String> left, List<String> right) {
        Map<String, Integer> available = new LinkedHashMap<>();
        for (String line : right) {
            available.merge(line, 1, Integer::sum);
        }

        List<String> result = new ArrayList<>();
        for (String line : left) {
            int count = available.getOrDefault(line, 0);
            if (count == 0) {
                result.add(line);
            } else if (count == 1) {
                available.remove(line);
            } else {
                available.put(line, count - 1);
            }
        }
        return result;
    }
}
