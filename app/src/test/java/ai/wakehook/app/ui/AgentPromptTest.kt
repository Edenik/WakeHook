package ai.wakehook.app.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AgentPromptTest {
    @Test fun build_containsFileNameSchemaAndGuidance() {
        val prompt = AgentPrompt.build("http://x/wakehook.json")

        assertThat(prompt).contains("wakehook.json")
        assertThat(prompt).contains("daysOfWeek")
        assertThat(prompt).contains("dates")
        assertThat(prompt).contains("enabled")

        val lower = prompt.lowercase()
        assertThat(lower).contains("add")
        assertThat(lower).contains("edit")
        assertThat(lower).contains("cancel")
    }

    @Test fun build_includesFileLinkWhenProvided() {
        val prompt = AgentPrompt.build("http://x/wakehook.json")
        assertThat(prompt).contains("http://x/wakehook.json")
    }

    @Test fun build_omitsFileLinkWhenNull() {
        val prompt = AgentPrompt.build(null)
        assertThat(prompt).doesNotContain("File link:")
    }
}
