package com.serwylo.jarverifier;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtils {

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int read = input.read(buffer);
        while (read != -1) {
            output.write(buffer, 0, read);
            read = input.read(buffer);
        }
    }

    public static void vacuumStream(InputStream input) throws IOException {
        byte[] buffer = new byte[4096];
        int read = input.read(buffer);
        while (read != -1) {
            read = input.read(buffer);
        }
    }
}
