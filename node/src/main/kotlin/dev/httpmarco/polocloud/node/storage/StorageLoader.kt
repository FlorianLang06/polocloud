package dev.httpmarco.polocloud.node.storage

class StorageLoader {

    val sources = ArrayList<StorageSource<*>>()

    fun addSource(source: StorageSource<*>) {
        this.sources.add(source)
    }

    fun <T> generateRepository(entry: Class<T>): StorageRepository<T> {
        return StorageRepository(this, entry)
    }
}