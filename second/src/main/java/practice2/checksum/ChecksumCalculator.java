package practice2.checksum;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class ChecksumCalculator {
    private static final int BUFFER_SIZE = 8 * 1024;

    private ChecksumCalculator() {
    }

    public static int calculate(Path file) throws IOException {
        int checksum = 0;
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            while (channel.read(buffer) != -1) {
                buffer.flip();

                // xor делаем по 16-битным словам
                while (buffer.remaining() >= Short.BYTES) {
                    checksum ^= Short.toUnsignedInt(buffer.getShort());
                }

                // сохраняем последний байт если чтение закончилось на нечетной границе
                if (buffer.hasRemaining()) {
                    buffer.compact();
                } else {
                    buffer.clear();
                }
            }
        }

        buffer.flip();
        if (buffer.hasRemaining()) {
            checksum ^= Byte.toUnsignedInt(buffer.get()) << 8;
        }
        return checksum & 0xFFFF;
    }
}
