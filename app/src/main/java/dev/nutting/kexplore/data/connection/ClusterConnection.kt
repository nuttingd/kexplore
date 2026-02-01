package dev.nutting.kexplore.data.connection

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClusterConnection(
    val id: String,
    val name: String,
    val server: String,
    val authMethod: AuthMethod,
    val certificateAuthorityData: String? = null,
    val skipTlsVerify: Boolean = false,
)

@Serializable
sealed interface AuthMethod {
    @Serializable
    @SerialName("bearer_token")
    data class BearerToken(val token: String) : AuthMethod

    @Serializable
    @SerialName("client_certificate")
    data class ClientCertificate(
        val clientCertData: String,
        val clientKeyData: String,
    ) : AuthMethod

    @Serializable
    @SerialName("kubeconfig")
    data class Kubeconfig(
        val contextName: String,
        val rawKubeconfig: String,
    ) : AuthMethod
}
