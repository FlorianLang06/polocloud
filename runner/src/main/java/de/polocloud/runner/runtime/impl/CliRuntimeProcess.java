package de.polocloud.runner.runtime.impl;

import de.polocloud.runner.runtime.AbstractRuntimeProcess;

import java.util.Arrays;
import java.util.List;

public final class CliRuntimeProcess extends AbstractRuntimeProcess {
    @Override
    protected String getArtifactId() {
        return "cli";
    }

    @Override
    protected String getName() {
        return "PoloCloud CLI";
    }

    @Override
    protected List<String> getRequiredModules() {
        return Arrays.asList(
                "common",
                "proto"
        );
    }
}
