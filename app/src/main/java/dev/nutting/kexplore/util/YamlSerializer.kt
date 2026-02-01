package dev.nutting.kexplore.util

import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.client.utils.Serialization

object YamlSerializer {
    fun toYaml(resource: HasMetadata): String =
        Serialization.asYaml(resource)
}
