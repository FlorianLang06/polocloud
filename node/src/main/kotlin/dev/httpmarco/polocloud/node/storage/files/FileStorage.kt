package dev.httpmarco.polocloud.node.storage.files

import dev.httpmarco.polocloud.node.QueryLayer
import dev.httpmarco.polocloud.node.storage.QueryMethod
import dev.httpmarco.polocloud.node.storage.StorageRepository
import dev.httpmarco.polocloud.node.storage.StorageSource

class FileStorage<T> : StorageSource<T> {

    override fun allowedMethods(): List<QueryMethod> {
        TODO("Not yet implemented")
    }

    override fun layer(): QueryLayer {
        TODO("Not yet implemented")
    }

    override fun <T> findAll(repo: StorageRepository<T>): List<T> {
        TODO("Not yet implemented")
    }


}