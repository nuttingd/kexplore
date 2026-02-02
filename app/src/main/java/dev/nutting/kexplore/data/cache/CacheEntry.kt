package dev.nutting.kexplore.data.cache

import dev.nutting.kexplore.data.model.ResourceSummary
import kotlinx.serialization.Serializable

@Serializable
data class CacheEntry(
    val items: List<ResourceSummary>,
    val lastUpdatedMillis: Long,
)
