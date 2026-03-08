package de.polocloud.runner;

import de.polocloud.runner.utils.Manifests;

/**
 * Entry point for the PoloCloud runtime.
 * <p>
 * This launcher is responsible for:
 * <ul>
 *     <li>Validating the runtime environment</li>
 *     <li>Resolving the current runtime version from the manifest</li>
 *     <li>Starting the PoloCloud process</li>
 *     <li>Exiting the JVM with the process exit code</li>
 * </ul>
 */
final class PolocloudRuntimeLauncher {

    /**
     * Utility class – instantiation is not allowed.
     */
    private PolocloudRuntimeLauncher() {
        throw new UnsupportedOperationException("This is a boot class");
    }

    /**
     * Launches the PoloCloud runtime.
     *
     * @param args command-line arguments (currently unused)
     */
    public static void main(String[] args) {
        PolocloudParameters.ensureCacheDirectory();

        final PolocloudRuntimeBootValidator validator = new PolocloudRuntimeBootValidator();

        if (!validator.isValid()) {
            System.exit(1);
        }

        final String runtimeVersion = Manifests
                .readOwnManifest()
                .getMainAttributes()
                .getValue(PolocloudParameters.VERSION_ENV);

        System.setProperty(PolocloudParameters.VERSION_ENV, runtimeVersion);

        final PolocloudProcess process = new PolocloudProcess();
        final int status = process.start();

        System.exit(status);
    }
}
