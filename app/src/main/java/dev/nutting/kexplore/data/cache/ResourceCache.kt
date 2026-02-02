package dev.nutting.kexplore.data.cache

import android.content.Context
import dev.nutting.kexplore.data.model.ResourceSummary
import dev.nutting.kexplore.data.model.ResourceType
import kotlinx.serialization.json.Json
import java.io.File

class ResourceCache(context: Context) {

    private val cacheDir = File(context.cacheDir, "resource_cache").also { it.mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    fun get(connectionId: String, namespace: String, type: ResourceType): CacheEntry? {
        val file = cacheFile(connectionId, namespace, type)
        if (!file.exists()) return null
        return try {
            json.decodeFromString<CacheEntry>(file.readText())
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    fun put(connectionId: String, namespace: String, type: ResourceType, items: List<ResourceSummary>) {
        val entry = CacheEntry(
            items = items,
            lastUpdatedMillis = System.currentTimeMillis(),
        )
        val file = cacheFile(connectionId, namespace, type)
        file.parentFile?.mkdirs()
        try {
            file.writeText(json.encodeToString(CacheEntry.serializer(), entry))
        } catch (_: Exception) {
            // Ignore write failures
        }
    }

    private fun cacheFile(connectionId: String, namespace: String, type: ResourceType): File {
        val ns = namespace.ifEmpty { "_all" }
        val safeId = connectionId.replace(Regex("[^a-zA-Z0-9-]"), "_")
        return File(cacheDir, "$safeId/${ns}/${type.name}.json")
    }
}
