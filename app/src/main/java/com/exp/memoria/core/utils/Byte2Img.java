package com.exp.memoria.core.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Byte2Img {

    /**
     * Saves a byte array as an image file.
     *
     * @param imageBytes The byte array of the image.
     * @param filePath   The full path where the image will be saved (e.g., /path/to/your/image.png).
     * @throws IOException If an I/O error occurs during writing.
     */
    public static void saveBytesAsImage(byte[] imageBytes, String filePath) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image byte array cannot be null or empty.");
        }

        File outputFile = new File(filePath);

        // Ensure parent directories exist
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Could not create parent directories for file: " + filePath);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(imageBytes);
        }
    }
}
