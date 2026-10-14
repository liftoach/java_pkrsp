package practice.files;

public record GeneratedFile(int id, FileType type, int size, boolean poison) {
    public GeneratedFile {
        if (!poison && (size < 10 || size > 100)) {
            throw new IllegalArgumentException("Размер файла должен быть от 10 до 100");
        }
    }

    static GeneratedFile poison(FileType type) {
        return new GeneratedFile(-1, type, 10, true);
    }
}
