package de.polocloud.runner;

import de.polocloud.runner.runtime.RuntimeMode;
import de.polocloud.runner.runtime.RuntimeProcess;
import de.polocloud.runner.runtime.RuntimeResolver;
import de.polocloud.runner.runtime.impl.CliRuntimeProcess;
import de.polocloud.runner.runtime.impl.NodeRuntimeProcess;
import de.polocloud.runner.utils.Manifests;

final class PolocloudRuntimeLauncher {

    private PolocloudRuntimeLauncher() {}

    public static void main(String[] args) {
        PolocloudParameters.ensureCacheDirectory();

        if (!new PolocloudRuntimeBootValidator().isValid()) {
            System.exit(1);
        }

        String version = Manifests
                .readOwnManifest()
                .getMainAttributes()
                .getValue(PolocloudParameters.VERSION_ENV);

        System.setProperty(PolocloudParameters.VERSION_ENV, version);

        RuntimeMode mode = RuntimeResolver.resolve(args);
        RuntimeProcess process = createProcess(mode);

        int status = process.start();
        System.exit(status);
    }

    private static RuntimeProcess createProcess(RuntimeMode mode) {
        switch (mode) {
            case CLI: return new CliRuntimeProcess();
            case NODE: return new NodeRuntimeProcess();
            default: throw new IllegalStateException("Unsupported mode: " + mode);
        }
    }
}
