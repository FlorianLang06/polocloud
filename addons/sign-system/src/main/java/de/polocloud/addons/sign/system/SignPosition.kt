package de.polocloud.addons.sign.system

import kotlinx.serialization.Serializable

/** A world-relative block position. Platform-agnostic in shape; only world-based platforms use it. */
@Serializable
data class SignPosition(val x: Int, val y: Int, val z: Int, val world: String)