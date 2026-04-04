package de.polocloud.runner;

import de.polocloud.runner.classloader.PolocloudClassLoader;
import de.polocloud.runner.expender.ExpenderRuntimeCache;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Controls the lifecycle of the Polocloud CLI process.
 * <p>
 * The CLI is executed inside the current JVM using a dedicated
 * {@link PolocloudClassLoader} to allow dynamic dependency management
 * and clean restarts without spawning a new JVM.
 *
 * <p>
 * This class is fully compatible with Java 8.
 */
public final class PolocloudProcess {

    /**
     * Starts the Polocloud CLI.
     *
     * @return {@code 0} if the CLI started successfully, {@code 1} otherwise
     */
    public int start() {
        try {
            prepareRuntimeEnvironment();
            invokeMain(createApplicationClassLoader());
            return 0;
        } catch (Exception e) {
            System.err.println("Failed to start Polocloud CLI");
            e.printStackTrace(System.err);
        }
        return 1;
    }

    /**
     * Prepares all required runtime components before the CLI is started.
     *
     * @throws IOException if runtime preparation fails
     */
    private void prepareRuntimeEnvironment() throws IOException {
        ExpenderRuntimeCache.migrateCacheFiles();
        ensureBootstrapLibrariesPresent();
    }

    /**
     * Creates the {@link PolocloudClassLoader} used to execute the CLI.
     *
     * @return a new application class loader
     * @throws IOException if classpath URLs cannot be created
     */
    private PolocloudClassLoader createApplicationClassLoader() throws IOException {
        List<Path> classpath = getApplicationClasspath();
        List<URL> urls = new ArrayList<>(classpath.size());

        for (Path path : classpath) {
            urls.add(path.toUri().toURL());
        }

        return new PolocloudClassLoader(urls.toArray(new URL[0]));
    }

    /**
     * Resolves all classpath entries required to launch the CLI.
     *
     * <p>This includes:</p>
     * <ul>
     *     <li>Bootstrap dependencies (Kotlin)</li>
     *     <li>Application modules (common, cli)</li>
     * </ul>
     *
     * @return ordered list of classpath entries
     */
    private List<Path> getApplicationClasspath() {
        List<Path> elements = new ArrayList<>();

        elements.add(PolocloudParameters.bootKotlin());

        elements.add(PolocloudParameters.expenderRuntimeCache("common"));
        elements.add(PolocloudParameters.expenderRuntimeCache("node"));

        return elements;
    }

    /**
     * Invokes the CLI main method via reflection.
     *
     * @param classLoader the class loader used to load the CLI
     */
    private void invokeMain(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, InterruptedException {
        String mainClassName = resolveMainClassName();
        Class<?> mainClass = Class.forName(mainClassName, true, classLoader);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        Thread mainThread = new Thread(() -> {
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                mainMethod.invoke(null, (Object) new String[0]);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access main method", e);
            } catch (InvocationTargetException e) {
                // Root Cause weitergeben
                throw new RuntimeException("Exception in main()", e.getCause());
            }
        }, "Polocloud-Main-Thread");

        mainThread.setUncaughtExceptionHandler((t, ex) -> ex.printStackTrace(System.err));
        mainThread.start();
        mainThread.join();
    }

    /**
     * Resolves the fully qualified main class name of the CLI artifact.
     *
     * @return the CLI main class name
     */
    private String resolveMainClassName() {
        return Objects.requireNonNull(
                ExpenderRuntimeCache.findElementByArtifactId("cli"),
                "CLI artifact not found in runtime cache"
        ).mainClass();
    }

    /**
     * Ensures that all bootstrap dependencies are present locally.
     *
     * @throws IOException if downloading fails
     */
    private void ensureBootstrapLibrariesPresent() throws IOException {
        ensureLibrary(
                PolocloudParameters.bootKotlin(),
                PolocloudParameters.kotlinDownloadUrl()
        );
    }

    /**
     * Ensures that a library exists locally, downloading it if necessary.
     *
     * @param target the target file path
     * @param url    the download URL
     * @throws IOException if download fails
     */
    private void ensureLibrary(Path target, String url) throws IOException {
        if (Files.exists(target)) return;

        Files.createDirectories(target.getParent());

        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Closes the class loader quietly.
     *
     * @param classLoader the class loader to close
     */
    private void closeClassLoaderQuietly(PolocloudClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }

        try {
            classLoader.close();
        } catch (Exception e) {
            System.err.println("Failed to close PolocloudClassLoader");
            e.printStackTrace(System.err);
        }
    }
}
