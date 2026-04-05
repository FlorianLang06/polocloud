package de.polocloud.runner.runtime.impl;

import de.polocloud.runner.runtime.AbstractRuntimeProcess;

public final class CliRuntimeProcess extends AbstractRuntimeProcess {
    @Override
    protected String getArtifactId() {
        return "cli";
    }

    @Override
    protected String getName() {
        return "PoloCloud CLI";
    }
}
