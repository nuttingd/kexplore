package dev.nutting.kexplore.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ResourceTypeSerializer : KSerializer<ResourceType> {
    override val descriptor = PrimitiveSerialDescriptor("ResourceType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ResourceType) = encoder.encodeString(value.name)
    override fun deserialize(decoder: Decoder): ResourceType = ResourceType.valueOf(decoder.decodeString())
}

@Serializable
data class ResourceSummary(
    val name: String,
    val namespace: String,
    @Serializable(with = ResourceTypeSerializer::class) val kind: ResourceType,
    val status: ResourceStatus,
    val age: String,
    val labels: Map<String, String> = emptyMap(),
    val readyCount: String? = null,
    val restarts: Int? = null,
)
