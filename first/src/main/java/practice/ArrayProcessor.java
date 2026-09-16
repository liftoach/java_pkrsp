package practice;

public interface ArrayProcessor extends AutoCloseable {
    long calculate(int[] values);

    String name();

    @Override
    default void close() {}
}
