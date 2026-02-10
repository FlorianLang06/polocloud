package dev.httpmarco.polocloud.node.storage

interface StorageActions {

    fun <T> findAll(repo: StorageRepository<T>): List<T>

}