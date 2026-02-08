package dev.httpmarco.polocloud.node.cluster

import dev.httpmarco.polocloud.node.database.DatabaseIdentifier

class Node() {
    @setparam:DatabaseIdentifier
    var identifier: String = ""

    constructor(identifier: String) : this() {
        this.identifier = identifier
    }
}
