package dev.nutting.kexplore.data.kubernetes

import dev.nutting.kexplore.data.model.ResourceStatus
import dev.nutting.kexplore.data.model.ResourceType
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.api.model.NodeBuilder
import io.fabric8.kubernetes.api.model.NodeConditionBuilder
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder
import io.fabric8.kubernetes.api.model.PersistentVolumeBuilder
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.api.model.PodConditionBuilder
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentConditionBuilder
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscalerBuilder
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscalerConditionBuilder
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResourceMappersTest {

    @Test
    fun `toSummary - Pod maps basic fields`() {
        val pod = PodBuilder()
            .withMetadata(
                ObjectMetaBuilder()
                    .withName("my-pod")
                    .withNamespace("default")
                    .withCreationTimestamp("2025-01-01T00:00:00Z")
                    .withLabels<String, String>(mapOf("app" to "test"))
                    .build()
            )
            .withNewStatus().withPhase("Running").endStatus()
            .withNewSpec().addNewContainer().withName("main").endContainer().endSpec()
            .build()

        val summary = ResourceMappers.toSummary(pod, ResourceType.Pod)

        assertEquals("my-pod", summary.name)
        assertEquals("default", summary.namespace)
        assertEquals(ResourceType.Pod, summary.kind)
        assertEquals(ResourceStatus.Running, summary.status)
        assertEquals(mapOf("app" to "test"), summary.labels)
    }

    @Test
    fun `toSummary - Node maps basic fields`() {
        val node = NodeBuilder()
            .withMetadata(
                ObjectMetaBuilder()
                    .withName("node-1")
                    .withCreationTimestamp("2025-01-01T00:00:00Z")
                    .build()
            )
            .withNewStatus()
            .withConditions(
                NodeConditionBuilder()
                    .withType("Ready")
                    .withStatus("True")
                    .build()
            )
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(node, ResourceType.Node)

        assertEquals("node-1", summary.name)
        assertEquals("", summary.namespace)
        assertEquals(ResourceStatus.Running, summary.status)
    }

    @Test
    fun `inferStatus - Pod Running`() {
        val pod = PodBuilder()
            .withNewMetadata().withName("p").endMetadata()
            .withNewStatus().withPhase("Running").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pod, ResourceType.Pod)
        assertEquals(ResourceStatus.Running, summary.status)
    }

    @Test
    fun `inferStatus - Pod Pending`() {
        val pod = PodBuilder()
            .withNewMetadata().withName("p").endMetadata()
            .withNewStatus().withPhase("Pending").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pod, ResourceType.Pod)
        assertEquals(ResourceStatus.Pending, summary.status)
    }

    @Test
    fun `inferStatus - Pod Failed`() {
        val pod = PodBuilder()
            .withNewMetadata().withName("p").endMetadata()
            .withNewStatus().withPhase("Failed").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pod, ResourceType.Pod)
        assertEquals(ResourceStatus.Failed, summary.status)
    }

    @Test
    fun `inferStatus - Pod Terminating when deletionTimestamp set`() {
        val pod = PodBuilder()
            .withMetadata(
                ObjectMetaBuilder()
                    .withName("p")
                    .withDeletionTimestamp("2025-01-01T00:00:00Z")
                    .build()
            )
            .withNewStatus().withPhase("SomethingElse").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pod, ResourceType.Pod)
        assertEquals(ResourceStatus.Terminating, summary.status)
    }

    @Test
    fun `inferStatus - Deployment available`() {
        val deployment = DeploymentBuilder()
            .withNewMetadata().withName("d").endMetadata()
            .withNewStatus()
            .withConditions(
                DeploymentConditionBuilder()
                    .withType("Available")
                    .withStatus("True")
                    .build()
            )
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(deployment, ResourceType.Deployment)
        assertEquals(ResourceStatus.Running, summary.status)
    }

    @Test
    fun `inferStatus - Deployment not available`() {
        val deployment = DeploymentBuilder()
            .withNewMetadata().withName("d").endMetadata()
            .withNewStatus()
            .withConditions(
                DeploymentConditionBuilder()
                    .withType("Available")
                    .withStatus("False")
                    .build()
            )
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(deployment, ResourceType.Deployment)
        assertEquals(ResourceStatus.Pending, summary.status)
    }

    @Test
    fun `inferStatus - Job succeeded`() {
        val job = JobBuilder()
            .withNewMetadata().withName("j").endMetadata()
            .withNewStatus().withSucceeded(1).endStatus()
            .build()

        val summary = ResourceMappers.toSummary(job, ResourceType.Job)
        assertEquals(ResourceStatus.Succeeded, summary.status)
    }

    @Test
    fun `inferStatus - Job failed`() {
        val job = JobBuilder()
            .withNewMetadata().withName("j").endMetadata()
            .withNewStatus().withFailed(1).endStatus()
            .build()

        val summary = ResourceMappers.toSummary(job, ResourceType.Job)
        assertEquals(ResourceStatus.Failed, summary.status)
    }

    @Test
    fun `inferStatus - Node not ready`() {
        val node = NodeBuilder()
            .withNewMetadata().withName("n").endMetadata()
            .withNewStatus()
            .withConditions(
                NodeConditionBuilder()
                    .withType("Ready")
                    .withStatus("False")
                    .build()
            )
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(node, ResourceType.Node)
        assertEquals(ResourceStatus.Failed, summary.status)
    }

    @Test
    fun `readyCount - Pod returns container ready count`() {
        val pod = PodBuilder()
            .withNewMetadata().withName("p").endMetadata()
            .withNewSpec()
            .addNewContainer().withName("c1").endContainer()
            .addNewContainer().withName("c2").endContainer()
            .endSpec()
            .withNewStatus()
            .withPhase("Running")
            .addNewContainerStatus().withName("c1").withReady(true).withRestartCount(0).endContainerStatus()
            .addNewContainerStatus().withName("c2").withReady(false).withRestartCount(0).endContainerStatus()
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pod, ResourceType.Pod)
        assertEquals("1/2", summary.readyCount)
    }

    @Test
    fun `readyCount - Deployment returns replica count`() {
        val deployment = DeploymentBuilder()
            .withNewMetadata().withName("d").endMetadata()
            .withNewSpec().withReplicas(3).endSpec()
            .withNewStatus().withReadyReplicas(2).endStatus()
            .build()

        val summary = ResourceMappers.toSummary(deployment, ResourceType.Deployment)
        assertEquals("2/3", summary.readyCount)
    }

    @Test
    fun `readyCount - ReplicaSet returns replica count`() {
        val rs = ReplicaSetBuilder()
            .withNewMetadata().withName("rs").endMetadata()
            .withNewSpec().withReplicas(3).endSpec()
            .withNewStatus().withReadyReplicas(3).endStatus()
            .build()

        val summary = ResourceMappers.toSummary(rs, ResourceType.ReplicaSet)
        assertEquals("3/3", summary.readyCount)
    }

    @Test
    fun `readyCount - returns null for resource types without replicas`() {
        val node = NodeBuilder()
            .withNewMetadata().withName("n").endMetadata()
            .withNewStatus()
            .withConditions(
                NodeConditionBuilder().withType("Ready").withStatus("True").build()
            )
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(node, ResourceType.Node)
        assertNull(summary.readyCount)
    }

    @Test
    fun `toDetail - Pod extracts conditions`() {
        val pod = PodBuilder()
            .withNewMetadata()
            .withName("p")
            .withNamespace("ns")
            .withUid("123")
            .withCreationTimestamp("2025-01-01T00:00:00Z")
            .endMetadata()
            .withNewStatus()
            .withPhase("Running")
            .withConditions(
                PodConditionBuilder()
                    .withType("Ready")
                    .withStatus("True")
                    .withReason("PodReady")
                    .build()
            )
            .endStatus()
            .withNewSpec().addNewContainer().withName("main").withImage("nginx").endContainer().endSpec()
            .build()

        val detail = ResourceMappers.toDetail(pod, ResourceType.Pod)

        assertEquals(1, detail.conditions.size)
        assertEquals("Ready", detail.conditions[0].type)
        assertEquals("True", detail.conditions[0].status)
        assertEquals("PodReady", detail.conditions[0].reason)
    }

    @Test
    fun `extractSpec - Pod includes node and pod IP`() {
        val pod = PodBuilder()
            .withNewMetadata()
            .withName("p")
            .withUid("123")
            .withCreationTimestamp("2025-01-01T00:00:00Z")
            .endMetadata()
            .withNewSpec()
            .withNodeName("node-1")
            .withServiceAccountName("default")
            .withRestartPolicy("Always")
            .addNewContainer().withName("main").withImage("nginx").endContainer()
            .endSpec()
            .withNewStatus()
            .withPhase("Running")
            .withPodIP("10.0.0.1")
            .withQosClass("BestEffort")
            .endStatus()
            .build()

        val detail = ResourceMappers.toDetail(pod, ResourceType.Pod)

        assertEquals("node-1", detail.spec["Node"])
        assertEquals("10.0.0.1", detail.spec["Pod IP"])
        assertEquals("BestEffort", detail.spec["QoS Class"])
        assertEquals("default", detail.spec["Service Account"])
        assertEquals("Always", detail.spec["Restart Policy"])
    }

    @Test
    fun `extractSpec - Deployment includes strategy and replicas`() {
        val deployment = DeploymentBuilder()
            .withNewMetadata()
            .withName("d")
            .withUid("456")
            .withCreationTimestamp("2025-01-01T00:00:00Z")
            .endMetadata()
            .withNewSpec()
            .withReplicas(3)
            .withNewStrategy().withType("RollingUpdate").endStrategy()
            .withNewSelector().withMatchLabels<String, String>(mapOf("app" to "web")).endSelector()
            .endSpec()
            .withNewStatus()
            .withReadyReplicas(2)
            .withUpdatedReplicas(3)
            .withAvailableReplicas(2)
            .endStatus()
            .build()

        val detail = ResourceMappers.toDetail(deployment, ResourceType.Deployment)

        assertEquals("RollingUpdate", detail.spec["Strategy"])
        assertEquals("2 ready / 3 desired", detail.spec["Replicas"])
        assertEquals("app=web", detail.spec["Selector"])
    }

    @Test
    fun `extractSpec - ReplicaSet includes replicas and selector`() {
        val rs = ReplicaSetBuilder()
            .withNewMetadata()
            .withName("rs")
            .withUid("789")
            .withCreationTimestamp("2025-01-01T00:00:00Z")
            .endMetadata()
            .withNewSpec()
            .withReplicas(2)
            .withNewSelector().withMatchLabels<String, String>(mapOf("app" to "api")).endSelector()
            .endSpec()
            .withNewStatus()
            .withReadyReplicas(2)
            .withAvailableReplicas(2)
            .endStatus()
            .build()

        val detail = ResourceMappers.toDetail(rs, ResourceType.ReplicaSet)

        assertEquals("2 ready / 2 desired", detail.spec["Replicas"])
        assertEquals("2", detail.spec["Available Replicas"])
        assertEquals("app=api", detail.spec["Selector"])
    }

    // --- StatefulSet ---

    @Test
    fun `inferStatus - StatefulSet running when all replicas ready`() {
        val ss = StatefulSetBuilder()
            .withNewMetadata().withName("ss").endMetadata()
            .withNewSpec().withReplicas(3).endSpec()
            .withNewStatus().withReadyReplicas(3).endStatus()
            .build()

        val summary = ResourceMappers.toSummary(ss, ResourceType.StatefulSet)
        assertEquals(ResourceStatus.Running, summary.status)
    }

    @Test
    fun `inferStatus - StatefulSet pending when not all replicas ready`() {
        val ss = StatefulSetBuilder()
            .withNewMetadata().withName("ss").endMetadata()
            .withNewSpec().withReplicas(3).endSpec()
            .withNewStatus().withReadyReplicas(1).endStatus()
            .build()

        val summary = ResourceMappers.toSummary(ss, ResourceType.StatefulSet)
        assertEquals(ResourceStatus.Pending, summary.status)
    }

    // --- DaemonSet ---

    @Test
    fun `inferStatus - DaemonSet running when all nodes ready`() {
        val ds = DaemonSetBuilder()
            .withNewMetadata().withName("ds").endMetadata()
            .withNewStatus()
            .withDesiredNumberScheduled(5)
            .withNumberReady(5)
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(ds, ResourceType.DaemonSet)
        assertEquals(ResourceStatus.Running, summary.status)
    }

    @Test
    fun `inferStatus - DaemonSet pending when not all ready`() {
        val ds = DaemonSetBuilder()
            .withNewMetadata().withName("ds").endMetadata()
            .withNewStatus()
            .withDesiredNumberScheduled(5)
            .withNumberReady(3)
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(ds, ResourceType.DaemonSet)
        assertEquals(ResourceStatus.Pending, summary.status)
    }

    // --- Namespace ---

    @Test
    fun `inferStatus - Namespace Active`() {
        val ns = NamespaceBuilder()
            .withNewMetadata().withName("default").endMetadata()
            .withNewStatus().withPhase("Active").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(ns, ResourceType.Namespace)
        assertEquals(ResourceStatus.Running, summary.status)
    }

    @Test
    fun `inferStatus - Namespace Terminating`() {
        val ns = NamespaceBuilder()
            .withNewMetadata().withName("old").endMetadata()
            .withNewStatus().withPhase("Terminating").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(ns, ResourceType.Namespace)
        assertEquals(ResourceStatus.Terminating, summary.status)
    }

    // --- PersistentVolume ---

    @Test
    fun `inferStatus - PV Bound`() {
        val pv = PersistentVolumeBuilder()
            .withNewMetadata().withName("pv").endMetadata()
            .withNewStatus().withPhase("Bound").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pv, ResourceType.PersistentVolume)
        assertEquals(ResourceStatus.Running, summary.status)
    }

    @Test
    fun `inferStatus - PV Available`() {
        val pv = PersistentVolumeBuilder()
            .withNewMetadata().withName("pv").endMetadata()
            .withNewStatus().withPhase("Available").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pv, ResourceType.PersistentVolume)
        assertEquals(ResourceStatus.Running, summary.status)
    }

    @Test
    fun `inferStatus - PV Released`() {
        val pv = PersistentVolumeBuilder()
            .withNewMetadata().withName("pv").endMetadata()
            .withNewStatus().withPhase("Released").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pv, ResourceType.PersistentVolume)
        assertEquals(ResourceStatus.Pending, summary.status)
    }

    @Test
    fun `inferStatus - PV Failed`() {
        val pv = PersistentVolumeBuilder()
            .withNewMetadata().withName("pv").endMetadata()
            .withNewStatus().withPhase("Failed").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pv, ResourceType.PersistentVolume)
        assertEquals(ResourceStatus.Failed, summary.status)
    }

    // --- PersistentVolumeClaim ---

    @Test
    fun `inferStatus - PVC Bound`() {
        val pvc = PersistentVolumeClaimBuilder()
            .withNewMetadata().withName("pvc").withNamespace("default").endMetadata()
            .withNewStatus().withPhase("Bound").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pvc, ResourceType.PersistentVolumeClaim)
        assertEquals(ResourceStatus.Running, summary.status)
    }

    @Test
    fun `inferStatus - PVC Pending`() {
        val pvc = PersistentVolumeClaimBuilder()
            .withNewMetadata().withName("pvc").withNamespace("default").endMetadata()
            .withNewStatus().withPhase("Pending").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pvc, ResourceType.PersistentVolumeClaim)
        assertEquals(ResourceStatus.Pending, summary.status)
    }

    @Test
    fun `inferStatus - PVC Lost`() {
        val pvc = PersistentVolumeClaimBuilder()
            .withNewMetadata().withName("pvc").withNamespace("default").endMetadata()
            .withNewStatus().withPhase("Lost").endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pvc, ResourceType.PersistentVolumeClaim)
        assertEquals(ResourceStatus.Failed, summary.status)
    }

    // --- HorizontalPodAutoscaler ---

    @Test
    fun `inferStatus - HPA active`() {
        val hpa = HorizontalPodAutoscalerBuilder()
            .withNewMetadata().withName("hpa").withNamespace("default").endMetadata()
            .withNewStatus()
            .withConditions(
                HorizontalPodAutoscalerConditionBuilder()
                    .withType("ScalingActive")
                    .withStatus("True")
                    .build()
            )
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(hpa, ResourceType.HorizontalPodAutoscaler)
        assertEquals(ResourceStatus.Running, summary.status)
    }

    @Test
    fun `inferStatus - HPA not active`() {
        val hpa = HorizontalPodAutoscalerBuilder()
            .withNewMetadata().withName("hpa").withNamespace("default").endMetadata()
            .withNewStatus()
            .withConditions(
                HorizontalPodAutoscalerConditionBuilder()
                    .withType("ScalingActive")
                    .withStatus("False")
                    .build()
            )
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(hpa, ResourceType.HorizontalPodAutoscaler)
        assertEquals(ResourceStatus.Pending, summary.status)
    }

    // --- restartCount ---

    @Test
    fun `restartCount - sums across containers`() {
        val pod = PodBuilder()
            .withNewMetadata().withName("p").endMetadata()
            .withNewSpec()
            .addNewContainer().withName("c1").endContainer()
            .addNewContainer().withName("c2").endContainer()
            .endSpec()
            .withNewStatus()
            .withPhase("Running")
            .addNewContainerStatus().withName("c1").withReady(true).withRestartCount(3).endContainerStatus()
            .addNewContainerStatus().withName("c2").withReady(true).withRestartCount(7).endContainerStatus()
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(pod, ResourceType.Pod)
        assertEquals(10, summary.restarts)
    }

    @Test
    fun `restartCount - returns null for non-Pod`() {
        val node = NodeBuilder()
            .withNewMetadata().withName("n").endMetadata()
            .withNewStatus()
            .withConditions(
                NodeConditionBuilder().withType("Ready").withStatus("True").build()
            )
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(node, ResourceType.Node)
        assertNull(summary.restarts)
    }

    // --- Null/edge cases ---

    @Test
    fun `inferStatus - Pod with null status phase and no deletionTimestamp returns Unknown`() {
        val pod = PodBuilder()
            .withNewMetadata().withName("p").endMetadata()
            .build()

        val summary = ResourceMappers.toSummary(pod, ResourceType.Pod)
        assertEquals(ResourceStatus.Unknown, summary.status)
    }

    @Test
    fun `readyCount - StatefulSet returns ready count`() {
        val ss = StatefulSetBuilder()
            .withNewMetadata().withName("ss").endMetadata()
            .withNewSpec().withReplicas(3).endSpec()
            .withNewStatus().withReadyReplicas(2).endStatus()
            .build()

        val summary = ResourceMappers.toSummary(ss, ResourceType.StatefulSet)
        assertEquals("2/3", summary.readyCount)
    }

    @Test
    fun `readyCount - DaemonSet returns ready count`() {
        val ds = DaemonSetBuilder()
            .withNewMetadata().withName("ds").endMetadata()
            .withNewStatus()
            .withDesiredNumberScheduled(4)
            .withNumberReady(3)
            .endStatus()
            .build()

        val summary = ResourceMappers.toSummary(ds, ResourceType.DaemonSet)
        assertEquals("3/4", summary.readyCount)
    }
}
