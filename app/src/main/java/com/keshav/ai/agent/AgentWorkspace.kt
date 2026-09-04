package com.keshav.ai.agent

import android.content.Context
import java.io.File

/** Local workspace kept strictly inside the app sandbox. */
class AgentWorkspace(context: Context) {
    private val root = File(context.filesDir, "agent-workspace").apply { mkdirs() }
    fun list(relative: String = "."): String = safe(relative).walkTopDown().take(200).joinToString("\n") { it.relativeTo(root).path.ifBlank { "." } }
    fun read(relative: String, maxChars: Int = 100_000): String = safe(relative).readText().take(maxChars)
    fun write(relative: String, content: String) { val f = safe(relative); f.parentFile?.mkdirs(); f.writeText(content) }
    fun delete(relative: String) { val f = safe(relative); require(f != root) { "Workspace root cannot be deleted" }; f.deleteRecursively() }
    private fun safe(relative: String): File { val f = File(root, relative).canonicalFile; require(f == root || f.path.startsWith(root.path + File.separator)) { "Path is outside the agent workspace" }; return f }
}
