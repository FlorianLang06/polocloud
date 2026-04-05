package de.polocloud.runner.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;

public final class FileHelper {

    private FileHelper() {
        // Utility class, prevent instantiation
    }

    public static void hideFile(Path path) {
        DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);

        try {
            if (view != null && !view.readAttributes().isHidden()) {
                view.setHidden(true);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
