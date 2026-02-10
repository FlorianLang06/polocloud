package dev.httpmarco.polocloud.node.storage

import dev.httpmarco.polocloud.node.QueryLayer

interface StorageSource<T> : StorageActions {

    fun allowedMethods() : List<QueryMethod>

    fun layer() : QueryLayer

}