package dev.nutting.kexplore.data.kubernetes

import dev.nutting.kexplore.data.connection.AuthMethod
import dev.nutting.kexplore.data.connection.ClusterConnection
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder

object KubernetesClientFactory {

    fun createClient(connection: ClusterConnection): KubernetesClient {
        val config = when (val auth = connection.authMethod) {
            is AuthMethod.Kubeconfig -> {
                Config.fromKubeconfig(auth.contextName, auth.rawKubeconfig, null)
            }
            else -> {
                ConfigBuilder()
                    .withMasterUrl(connection.server)
                    .withTrustCerts(connection.skipTlsVerify)
                    .apply {
                        connection.certificateAuthorityData?.let { withCaCertData(it) }
                        when (auth) {
                            is AuthMethod.BearerToken -> withOauthToken(auth.token)
                            is AuthMethod.ClientCertificate -> {
                                withClientCertData(auth.clientCertData)
                                withClientKeyData(auth.clientKeyData)
                            }
                            is AuthMethod.Kubeconfig -> {} // handled above
                        }
                    }
                    .build()
            }
        }

        return KubernetesClientBuilder()
            .withConfig(config)
            .build()
    }
}
