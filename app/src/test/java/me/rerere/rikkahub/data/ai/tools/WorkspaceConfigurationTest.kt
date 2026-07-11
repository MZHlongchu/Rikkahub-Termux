package me.rerere.rikkahub.data.ai.tools

import me.rerere.rikkahub.data.ai.transformers.buildDefaultWorkspaceSystemPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceConfigurationTest {
    @Test
    fun builtInToolsAreEnabledByDefault() {
        WorkspaceToolDefaultEnabled.keys.forEach { toolName ->
            assertTrue(resolveWorkspaceToolEnabled(toolName, emptyMap()))
        }
        assertEquals(true, WorkspaceToolDefaultApprovals["workspace_python"])
    }

    @Test
    fun explicitToolSettingOverridesDefault() {
        assertFalse(
            resolveWorkspaceToolEnabled(
                name = "workspace_shell",
                overrides = mapOf("workspace_shell" to false),
            )
        )
        assertFalse(resolveWorkspaceToolEnabled("unknown_tool", emptyMap()))
    }

    @Test
    fun defaultPromptOnlyListsEnabledTools() {
        val prompt = buildDefaultWorkspaceSystemPrompt(
            mapOf(
                "workspace_read_file" to true,
                "workspace_write_file" to true,
                "workspace_edit_file" to true,
                "workspace_shell" to false,
                "workspace_python" to true,
            )
        )

        assertTrue(prompt.contains("`workspace_read_file`"))
        assertFalse(prompt.contains("`workspace_shell`"))
        assertTrue(prompt.contains("`workspace_python`"))
    }
}
