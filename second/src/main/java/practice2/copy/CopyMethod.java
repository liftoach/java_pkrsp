package practice2.copy;

public enum CopyMethod {
    FILE_STREAMS("FileInputStream/FileOutputStream"),
    FILE_CHANNEL("FileChannel"),
    APACHE_COMMONS_IO("Apache Commons IO"),
    FILES_CLASS("Files class");

    private final String title;

    CopyMethod(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
