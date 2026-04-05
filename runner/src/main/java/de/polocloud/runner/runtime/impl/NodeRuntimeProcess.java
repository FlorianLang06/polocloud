package de.polocloud.runner.runtime.impl;

import de.polocloud.runner.runtime.AbstractRuntimeProcess;

public final class NodeRuntimeProcess extends AbstractRuntimeProcess {
    @Override
    protected String getArtifactId() {
        return "node";
    }

    @Override
    protected String getName() {
        return "PoloCloud Node";
    }
}
