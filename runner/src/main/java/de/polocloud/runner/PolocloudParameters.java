package de.polocloud.runner;

import de.polocloud.runner.utils.Manifests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.jar.Manifest;

/**
 * Holds constant parameters, paths, and environment variable names
 * used by the Polocloud runner.
 *
 * <p>This class is a utility class and is not intended to be instantiated.</p>
 */
public final class PolocloudParameters {

    /**
     * Environment variable or system property that defines the Polocloud version
     * used by the runner.
     *
     * <p>Example value: {@code 1.0.0}</p>
     */
    public static final String VERSION_ENV = "version";

    /**
     * Name of the initialization control file used by Polocloud to track the initialization state of the CLI or node.
     *
     * <p>This file is stored in the "local" directory and contains metadata about the initialization process,</p>
     *  <p>such as timestamps, status, and any relevant information needed to determine if the CLI or node has been initialized.</p>
     */
    public static final String INITIALIZATION_CONTROL_FILE_NAME = "local/initialization.info";

    /**
     * Path to the runtime cache folder used by Polocloud for temporary and cached files.
     *
     * <p>This folder is created in the current working directory.</p>
     */
    public static final Path EXPENDER_RUNTIME_CACHE = Paths.get(".cache");

    /**
     * Manifest attribute key for the Kotlin runtime version.
     */
    private static final String MANIFEST_KOTLIN_VERSION = "kotlin-version";

    /**
     * Cached manifest instance of the currently running application.
     */
    private static final Manifest MANIFEST = Manifests.readOwnManifest();

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private PolocloudParameters() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Reads a required attribute from the application manifest.
     *
     * @param key the manifest attribute key
     * @return the resolved value
     * @throws IllegalStateException if the attribute is missing
     */
    private static String requireManifestValue(String key) {
        String value = MANIFEST.getMainAttributes().getValue(key);

        if (value == null) {
            throw new IllegalStateException("Missing '" + key + "' in MANIFEST.MF");
        }

        return value;
    }

    /**
     * Resolves the Kotlin runtime version from the manifest.
     *
     * @return the Kotlin version used at runtime
     */
    public static String kotlinVersion() {
        return requireManifestValue(MANIFEST_KOTLIN_VERSION);
    }

    /**
     * Builds the download URL for the Kotlin standard library.
     *
     * @return Maven Central URL for kotlin-stdlib
     */
    public static String kotlinDownloadUrl() {
        String v = kotlinVersion();
        return "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/"
                + v + "/kotlin-stdlib-" + v + ".jar";
    }

    /**
     * Resolves the local cache path of the Kotlin runtime JAR.
     *
     * @return path to kotlin-stdlib JAR inside the cache directory
     */
    public static Path bootKotlin() {
        String v = kotlinVersion();
        return EXPENDER_RUNTIME_CACHE.resolve(Paths.get(
                "org", "jetbrains", "kotlin", "kotlin-stdlib",
                v, "kotlin-stdlib-" + v + ".jar"
        ));
    }

    /**
     * Retrieves the Polocloud version from the system property or environment variable.
     *
     * @return the Polocloud version, or {@code null} if not set
     */
    public static String version() {
        return System.getProperty(VERSION_ENV);
    }

    /**
     * Path to the Polocloud CLI boot JAR file.
     *
     * <p>The path is resolved relative to {@link #EXPENDER_RUNTIME_CACHE} and depends
     * on the Polocloud version returned by {@link #version()}.</p>
     *
     * <p>Example: {@code .cache/dev/httpmarco/polocloud/cli/1.0.0/cli-1.0.0.jar}</p>
     */
    public static Path expenderRuntimeCache(String project) {
        return EXPENDER_RUNTIME_CACHE.resolve(Paths.get(
                "de",
                "polocloud",
                project,
                version(),
                project + "-" + version() + ".jar"
        ));
    }

    /**
     * Ensures the runtime cache directory exists and is marked as hidden on Windows.
     *
     * <p>This method must be called once at launcher startup, before any other component
     * attempts to write into the cache directory.</p>
     *
     * @throws RuntimeException if the cache directory cannot be created
     */
    public static void ensureCacheDirectory() {
        if (Files.exists(EXPENDER_RUNTIME_CACHE)) {
            return;
        }

        try {
            Files.createDirectories(EXPENDER_RUNTIME_CACHE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create cache directory: " + EXPENDER_RUNTIME_CACHE, e);
        }

        try {
            DosFileAttributeView view = Files.getFileAttributeView(EXPENDER_RUNTIME_CACHE, DosFileAttributeView.class);
            if (view != null) {
                view.setHidden(true);
            }
        } catch (IOException ignored) {
            // Not a Windows filesystem – hidden attribute is not supported
        }
    }
}
