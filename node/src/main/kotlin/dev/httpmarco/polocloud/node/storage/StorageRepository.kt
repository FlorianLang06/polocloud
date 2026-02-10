package dev.httpmarco.polocloud.node.storage

import dev.httpmarco.polocloud.node.QueryLayer
import org.slf4j.LoggerFactory

/**
 * Repository for accessing storage data of type [T].
 *
 * Handles searching across all configured [StorageSource]s in the [StorageLoader],
 * considering both the priority of [QueryLayer]s and the allowed [QueryMethod]s.
 *
 * @param T Type of the stored objects
 * @param loader The StorageLoader containing all storage sources
 * @param type The class of the stored object
 */
class StorageRepository<T>(
    private val loader: StorageLoader,
    private val type: Class<T>
) {

    private val logger = LoggerFactory.getLogger(StorageRepository::class.java)

    /**
     * Order of layers for querying.
     * Queries start from the fastest sources: LOCAL -> FILESYSTEM -> CACHE -> DATABASE.
     */
    private val layerOrder = listOf(
        QueryLayer.LOCAL,
        QueryLayer.FILESYSTEM,
        QueryLayer.CACHE,
        QueryLayer.DATABASE
    )

    /**
     * Finds all objects of type [T] across all relevant [StorageSource]s.
     *
     * The query order is determined by the layer priority:
     * [LOCAL] -> [FILESYSTEM] -> [CACHE] -> [DATABASE].
     *
     * Items returned from slower sources are optionally written into the local layer
     * when using [QueryMethod.FASTEST_FIRST] for faster future access.
     *
     * @param method The query strategy ([QueryMethod]) to use
     * @return List of all found objects
     */
    fun findAll(method: QueryMethod): List<T> {
        val results = mutableListOf<T>()
        logger.debug("Loading ${method.name} for type ${type.simpleName} from storage sources...")

        // Filter only sources that allow this query method
        val relevantSources = loader.sources.filter { it.allowedMethods().contains(method) }

        // Sort sources by layer priority
        val orderedSources = relevantSources.sortedBy { layerOrder.indexOf(it.layer()) }

        for (source in orderedSources) {
            logger.debug("Loading ${method.name} from source '${source.javaClass.name}' at layer ${source.layer()}...")

            val items = source.findAll<T>(this)
            if (items.isNotEmpty()) {
                results += items
                logger.debug("Source '${source.javaClass.name}' returned ${items.size} items.")

                // Cache-fill: only for FASTEST_FIRST and if the source is not LOCAL
                if (method == QueryMethod.FASTEST_FIRST && source.layer() != QueryLayer.LOCAL) {
                    loader.sources
                        .filter { it.layer() == QueryLayer.LOCAL && it.allowedMethods().contains(QueryMethod.FASTEST_FIRST) }
                        .forEach { cacheSource ->
                            items.forEach {
                                //todo
                                //cacheSource.put(groupId(), it)
                            }
                        }
                }
            } else {
                logger.debug("Source '${source.javaClass.name}' returned no items.")
            }
        }

        logger.debug("Finished loading ${method.name} for type ${type.simpleName}. Total items: ${results.size}")
        return results
    }

    /**
     * Returns the group ID for this repository type.
     *
     * Typically, the simple class name of [T].
     */
    private fun groupId(): String = type.simpleName
}
