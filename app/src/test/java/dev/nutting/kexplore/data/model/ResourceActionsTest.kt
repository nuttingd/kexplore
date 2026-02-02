package dev.nutting.kexplore.data.model

import dev.nutting.kexplore.ui.screen.detail.ActionResult
import dev.nutting.kexplore.ui.screen.detail.ResourceDetailViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceActionsTest {

    @Test
    fun `canScale is true for Deployment, StatefulSet, ReplicaSet`() {
        assertTrue(ResourceType.Deployment.canScale)
        assertTrue(ResourceType.StatefulSet.canScale)
        assertTrue(ResourceType.ReplicaSet.canScale)
    }

    @Test
    fun `canScale is false for non-scalable types`() {
        assertFalse(ResourceType.Pod.canScale)
        assertFalse(ResourceType.DaemonSet.canScale)
        assertFalse(ResourceType.Job.canScale)
        assertFalse(ResourceType.CronJob.canScale)
        assertFalse(ResourceType.Service.canScale)
        assertFalse(ResourceType.Node.canScale)
    }

    @Test
    fun `canRestart is true for Deployment, StatefulSet, DaemonSet`() {
        assertTrue(ResourceType.Deployment.canRestart)
        assertTrue(ResourceType.StatefulSet.canRestart)
        assertTrue(ResourceType.DaemonSet.canRestart)
    }

    @Test
    fun `canRestart is false for non-restartable types`() {
        assertFalse(ResourceType.Pod.canRestart)
        assertFalse(ResourceType.ReplicaSet.canRestart)
        assertFalse(ResourceType.Job.canRestart)
        assertFalse(ResourceType.CronJob.canRestart)
        assertFalse(ResourceType.Service.canRestart)
    }

    @Test
    fun `canTrigger is true only for CronJob`() {
        assertTrue(ResourceType.CronJob.canTrigger)
        assertFalse(ResourceType.Job.canTrigger)
        assertFalse(ResourceType.Deployment.canTrigger)
    }

    @Test
    fun `isNode is true only for Node`() {
        assertTrue(ResourceType.Node.isNode)
        assertFalse(ResourceType.Pod.isNode)
        assertFalse(ResourceType.Namespace.isNode)
    }

    @Test
    fun `parseDesiredReplicas extracts desired count`() {
        assertEquals(3, ResourceDetailViewModel.parseDesiredReplicas("2 ready / 3 desired"))
        assertEquals(0, ResourceDetailViewModel.parseDesiredReplicas("0 ready / 0 desired"))
        assertEquals(10, ResourceDetailViewModel.parseDesiredReplicas("10 ready / 10 desired"))
    }

    @Test
    fun `parseDesiredReplicas returns null for null input`() {
        assertNull(ResourceDetailViewModel.parseDesiredReplicas(null))
    }

    @Test
    fun `parseDesiredReplicas returns null for non-matching string`() {
        assertNull(ResourceDetailViewModel.parseDesiredReplicas("not a replica string"))
        assertNull(ResourceDetailViewModel.parseDesiredReplicas(""))
    }

    @Test
    fun `ActionResult Success carries message`() {
        val result: ActionResult = ActionResult.Success("Scaled to 3 replicas")
        assertTrue(result is ActionResult.Success)
        assertEquals("Scaled to 3 replicas", (result as ActionResult.Success).message)
    }

    @Test
    fun `ActionResult Error carries message`() {
        val result: ActionResult = ActionResult.Error("Access denied")
        assertTrue(result is ActionResult.Error)
        assertEquals("Access denied", (result as ActionResult.Error).message)
    }
}
