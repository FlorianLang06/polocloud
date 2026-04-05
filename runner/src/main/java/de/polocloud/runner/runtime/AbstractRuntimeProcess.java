package de.polocloud.runner.runtime;

import de.polocloud.runner.PolocloudParameters;
import de.polocloud.runner.classloader.PolocloudClassLoader;
import de.polocloud.runner.expender.ExpenderRuntimeCache;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class AbstractRuntimeProcess implements RuntimeProcess {

    @Override
    public int start() {
        try {
            prepareRuntimeEnvironment();
            invokeMain(createClassLoader());
            return 0;
        } catch (Exception e) {
            System.err.println("Failed to start " + getName());
            e.printStackTrace();
        }
        return 1;
    }

    protected abstract String getArtifactId();

    protected abstract String getName();

    protected List<String> getRequiredModules() {
        return Collections.emptyList();
    }

    private void prepareRuntimeEnvironment() throws Exception {
        ExpenderRuntimeCache.migrateCacheFiles();
        ensureBootstrapLibrariesPresent();
    }

    private PolocloudClassLoader createClassLoader() throws Exception {
        List<URL> urls = new ArrayList<>();

        for (Path path : getClasspath()) {
            urls.add(path.toUri().toURL());
        }

        return new PolocloudClassLoader(urls.toArray(new URL[0]));
    }

    private List<Path> getClasspath() {
        List<Path> elements = new ArrayList<>();

        elements.add(PolocloudParameters.bootKotlin());

        for (String module : getRequiredModules()) {
            elements.add(PolocloudParameters.expenderRuntimeCache(module));
        }

        elements.add(PolocloudParameters.expenderRuntimeCache(getArtifactId()));

        return elements;
    }

    private void invokeMain(ClassLoader classLoader) throws Exception {
        String mainClassName = resolveMainClassName();

        Class<?> mainClass = Class.forName(mainClassName, true, classLoader);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        Thread thread = new Thread(() -> {
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                mainMethod.invoke(null, (Object) new String[0]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        thread.start();
        thread.join();
    }

    private String resolveMainClassName() {
        return Objects.requireNonNull(
                ExpenderRuntimeCache.findElementByArtifactId(getArtifactId()),
                getName() + " artifact not found"
        ).mainClass();
    }

    private void ensureBootstrapLibrariesPresent() throws Exception {
        Path target = PolocloudParameters.bootKotlin();
        if (Files.exists(target)) {
            return;
        }

        Files.createDirectories(target.getParent());

        try (InputStream inputStream = new URI(PolocloudParameters.kotlinDownloadUrl()).toURL().openStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
