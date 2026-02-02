package dev.nutting.kexplore.ui.screen.crd

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nutting.kexplore.data.kubernetes.CrdRepository
import dev.nutting.kexplore.ui.components.ContentStateHost
import dev.nutting.kexplore.ui.components.KeyValueRow
import dev.nutting.kexplore.ui.components.LabelChipGroup
import dev.nutting.kexplore.ui.components.MetadataCard
import dev.nutting.kexplore.ui.components.SectionHeader
import dev.nutting.kexplore.ui.components.YamlView
import dev.nutting.kexplore.data.model.ResourceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrdInstanceDetailScreen(
    crdRepository: CrdRepository?,
    crdName: String,
    namespace: String?,
    name: String,
    onBack: () -> Unit,
    viewModel: CrdInstanceDetailViewModel,
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "YAML")

    LaunchedEffect(crdRepository, crdName, namespace, name) {
        viewModel.loadInstance(crdRepository, crdName, namespace, name)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (tabs.getOrNull(selectedTab)) {
                "Overview" -> OverviewTab(state = state, onRetry = {
                    viewModel.loadInstance(crdRepository, crdName, namespace, name)
                })
                "YAML" -> YamlView(yaml = state.yaml)
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun OverviewTab(
    state: CrdInstanceDetailState,
    onRetry: () -> Unit,
) {
    ContentStateHost(state = state.resource, onRetry = onRetry) { resource ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            MetadataCard(
                name = resource.metadata.name,
                namespace = resource.metadata.namespace ?: "",
                uid = resource.metadata.uid ?: "",
                creationTimestamp = resource.metadata.creationTimestamp ?: "",
                status = ResourceStatus.Unknown,
            )

            val labels = resource.metadata.labels ?: emptyMap()
            if (labels.isNotEmpty()) {
                SectionHeader("Labels")
                LabelChipGroup(labels = labels)
            }

            val annotations = resource.metadata.annotations ?: emptyMap()
            if (annotations.isNotEmpty()) {
                SectionHeader("Annotations")
                annotations.forEach { (k, v) ->
                    KeyValueRow(k, v)
                }
            }

            // Flatten spec
            val spec = resource.additionalProperties["spec"]
            if (spec is Map<*, *>) {
                SectionHeader("Spec")
                flattenMap(spec as Map<String, Any?>, "").forEach { (k, v) ->
                    KeyValueRow(k, v)
                }
            }

            // Flatten status
            val status = resource.additionalProperties["status"]
            if (status is Map<*, *>) {
                SectionHeader("Status")
                flattenMap(status as Map<String, Any?>, "").forEach { (k, v) ->
                    KeyValueRow(k, v)
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun flattenMap(map: Map<String, Any?>, prefix: String): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    for ((key, value) in map) {
        val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
        when (value) {
            is Map<*, *> -> result.addAll(flattenMap(value as Map<String, Any?>, fullKey))
            is List<*> -> result.add(fullKey to value.joinToString(", "))
            null -> result.add(fullKey to "null")
            else -> result.add(fullKey to value.toString())
        }
    }
    return result
}
