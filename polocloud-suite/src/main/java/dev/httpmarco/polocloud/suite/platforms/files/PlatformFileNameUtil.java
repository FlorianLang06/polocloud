package dev.httpmarco.polocloud.suite.platforms.files;

import dev.httpmarco.polocloud.common.OS;
import dev.httpmarco.polocloud.suite.platforms.Platform;
import dev.httpmarco.polocloud.suite.platforms.PlatformVersion;

public class PlatformFileNameUtil {
    public static String platformBootFileName(Platform platform, PlatformVersion version) {
        var fileName = platform.name() + "-" + version.version();
        if (version.buildId() != null) {
            fileName += "-" + version.buildId();
        }
        if (!platform.language().serviceFileExtension().isEmpty()) {
            return fileName + platform.language().serviceFileExtension();
        }

        return fileName + OS.detect().binaryExtension();
    }
}
