package de.polocloud.addons.sign.system

import de.polocloud.shared.service.Service

/**
 * A sign/painting/banner attached to a group, waiting for (or bound to) one of that
 * group's running [Service]s.
 *
 * [service] is `null` while no matching service is running — [SignSystem] then falls
 * back to the layout's `UNKNOWN`/`QUEUED` animation. Plain class (not a data class):
 * [Service] itself is immutable, so [SignSystem] reassigns [service] to the latest
 * instance on every bind/refresh; [SignRegistry] relies on entry identity, not
 * service equality.
 */
class SignEntry(
    val type: SignEntryType,
    val position: SignPosition,
    val group: String,
    val layoutId: String,
) {

    @Volatile
    var service: Service? = null
}