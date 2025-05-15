package dev.httpmarco.polocloud.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public enum OS {

    WINDOWS(";", ".exe"),
    LINUX(":", ""),
    MACOS(":", ""),
    UNKNOWN(":", "");

    private final String processSeparator;
    private final String binaryExtension;

    public static OS detect() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return WINDOWS;
        } else if (osName.contains("nux") || osName.contains("nix") || osName.contains("linux")) {
            return LINUX;
        } else if (osName.contains("mac")) {
            return MACOS;
        } else {
            return UNKNOWN;
        }
    }

    public static String getArchitecture() {
        return System.getProperty("os.arch");
    }
}
