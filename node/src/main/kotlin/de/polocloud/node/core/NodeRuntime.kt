package de.polocloud.node.core

import de.polocloud.node.bootstrap.properties.NodeProperties
import de.polocloud.node.communication.registration.cli.CliRegistrationService
import de.polocloud.node.communication.cli.session.CliSessionManager
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.identity.NodeIdentityService
import de.polocloud.node.identity.provider.FileBasedNodeIdProvider
import de.polocloud.node.identity.provider.NodeIdProvider
import de.polocloud.node.core.lifecycle.NodeLifecycle
import de.polocloud.node.communication.registration.node.RegistrationManager

class NodeRuntime(
    val launchProperties: NodeProperties,
    val configurations: NodeConfigurations
) {
    val nodeId: NodeIdProvider = FileBasedNodeIdProvider()

    val cliSessionManager = CliSessionManager()
    val cliRegistrationService = CliRegistrationService(
        configurations.cluster,
        cliSessionManager
    )

    val registrationManager = RegistrationManager(
        configurations.cluster,
        cliRegistrationService
    )

    val identityService = NodeIdentityService(
        nodeId,
        registrationManager,
        cliRegistrationService,
        cliSessionManager
    )

    val lifecycle = NodeLifecycle(this)
}