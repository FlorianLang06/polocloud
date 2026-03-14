package de.polocloud.common.time

import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

@Serializable
data class Duration(val unit: TimeUnit, val value: Long)
