package me.rerere.rikkahub.data.ai.tools.termux

import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlinx.serialization.decodeFromString
import me.rerere.rikkahub.utils.JsonInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import sun.misc.Unsafe

class TermuxPtySessionManagerTest {
    @Test
    fun `bootstrap script should replace legacy server when token file is missing`() {
        assumeTrue(canRunCommand("bash"))
        assumeTrue(canRunCommand("python3"))

        val tempHome = Files.createTempDirectory("termux-pty-bootstrap").toFile()
        val stateDir = File(tempHome, ".rikkahub").apply { mkdirs() }
        val pidFile = File(stateDir, "pty_session_server.pid")
        val tokenFile = File(stateDir, "pty_session_server.token")
        val scriptFile = File(stateDir, "pty_session_server.py")
        val legacyToken = "legacy-token"
        val newToken = "fresh-token"
        val port = reservePort()
        var legacyPid: Long? = null

        try {
            val sessionManager = allocateSessionManager()
            scriptFile.writeText(sessionManager.termuxPtyServerScriptForTest())

            legacyPid = startDetachedPythonServer(
                homeDir = tempHome,
                scriptFile = scriptFile,
                port = port,
                token = legacyToken,
            )
            pidFile.writeText("$legacyPid\n")

            assertTrue(waitForHealth(port = port, token = legacyToken))
            assertFalse(tokenFile.exists())

            val bootstrap = ProcessBuilder("bash", "-s")
                .directory(tempHome)
                .apply {
                    environment()["HOME"] = tempHome.absolutePath
                }
                .start()
            bootstrap.outputStream.bufferedWriter().use { writer ->
                writer.write(
                    sessionManager.buildBootstrapScriptForTest(
                        port = port,
                        token = newToken,
                    )
                )
            }

            assertTrue("bootstrap script timed out", bootstrap.waitFor(20, TimeUnit.SECONDS))
            val stdout = bootstrap.inputStream.bufferedReader().readText()
            val stderr = bootstrap.errorStream.bufferedReader().readText()
            assertEquals(
                "bootstrap failed\nstdout:\n$stdout\nstderr:\n$stderr",
                0,
                bootstrap.exitValue(),
            )

            assertTrue(waitForPidExit(legacyPid))
            val newPid = pidFile.readText().trim().toLong()
            assertNotEquals(legacyPid, newPid)
            assertEquals(newToken, tokenFile.readText().trim())
            assertTrue(waitForHealth(port = port, token = newToken))
            assertFalse(isHealthy(port = port, token = legacyToken))
        } finally {
            legacyPid?.let(::killPid)
            readPid(pidFile)?.let { killPid(it) }
            tempHome.deleteRecursively()
        }
    }

    @Test
    fun `bootstrap script should replace orphan server when pid file is missing`() {
        assumeTrue(canRunCommand("bash"))
        assumeTrue(canRunCommand("python3"))

        val tempHome = Files.createTempDirectory("termux-pty-bootstrap-orphan").toFile()
        val stateDir = File(tempHome, ".rikkahub").apply { mkdirs() }
        val pidFile = File(stateDir, "pty_session_server.pid")
        val tokenFile = File(stateDir, "pty_session_server.token")
        val scriptFile = File(stateDir, "pty_session_server.py")
        val legacyToken = "legacy-token"
        val newToken = "fresh-token"
        val port = reservePort()
        var legacyPid: Long? = null

        try {
            val sessionManager = allocateSessionManager()
            scriptFile.writeText(sessionManager.termuxPtyServerScriptForTest())

            legacyPid = startDetachedPythonServer(
                homeDir = tempHome,
                scriptFile = scriptFile,
                port = port,
                token = legacyToken,
            )

            assertTrue(waitForHealth(port = port, token = legacyToken))
            assertFalse(pidFile.exists())
            assertFalse(tokenFile.exists())

            val bootstrap = ProcessBuilder("bash", "-s")
                .directory(tempHome)
                .apply {
                    environment()["HOME"] = tempHome.absolutePath
                }
                .start()
            bootstrap.outputStream.bufferedWriter().use { writer ->
                writer.write(
                    sessionManager.buildBootstrapScriptForTest(
                        port = port,
                        token = newToken,
                    )
                )
            }

            assertTrue("bootstrap script timed out", bootstrap.waitFor(20, TimeUnit.SECONDS))
            val stdout = bootstrap.inputStream.bufferedReader().readText()
            val stderr = bootstrap.errorStream.bufferedReader().readText()
            assertEquals(
                "bootstrap failed\nstdout:\n$stdout\nstderr:\n$stderr",
                0,
                bootstrap.exitValue(),
            )

            assertTrue(waitForPidExit(legacyPid))
            val newPid = pidFile.readText().trim().toLong()
            assertNotEquals(legacyPid, newPid)
            assertEquals(newToken, tokenFile.readText().trim())
            assertTrue(waitForHealth(port = port, token = newToken))
            assertFalse(isHealthy(port = port, token = legacyToken))
        } finally {
            legacyPid?.let(::killPid)
            readPid(pidFile)?.let { killPid(it) }
            tempHome.deleteRecursively()
        }
    }

    @Test
    fun `recover token script should read token from running orphan server env`() {
        assumeTrue(canRunCommand("bash"))
        assumeTrue(canRunCommand("python3"))

        val tempHome = Files.createTempDirectory("termux-pty-recover-token").toFile()
        val stateDir = File(tempHome, ".rikkahub").apply { mkdirs() }
        val pidFile = File(stateDir, "pty_session_server.pid")
        val tokenFile = File(stateDir, "pty_session_server.token")
        val scriptFile = File(stateDir, "pty_session_server.py")
        val token = "recover-token"
        val port = reservePort()
        var serverPid: Long? = null

        try {
            val sessionManager = allocateSessionManager()
            scriptFile.writeText(sessionManager.termuxPtyServerScriptForTest())

            serverPid = startDetachedPythonServer(
                homeDir = tempHome,
                scriptFile = scriptFile,
                port = port,
                token = token,
                exportEnvToken = true,
            )

            assertTrue(waitForHealth(port = port, token = token))
            assertFalse(pidFile.exists())
            assertFalse(tokenFile.exists())

            val recover = ProcessBuilder("bash", "-s")
                .directory(tempHome)
                .apply {
                    environment()["HOME"] = tempHome.absolutePath
                }
                .start()
            recover.outputStream.bufferedWriter().use { writer ->
                writer.write(sessionManager.buildRecoverTokenScriptForTest(port = port))
            }

            assertTrue("recover token script timed out", recover.waitFor(10, TimeUnit.SECONDS))
            val stdout = recover.inputStream.bufferedReader().readText().trim()
            val stderr = recover.errorStream.bufferedReader().readText()
            assertEquals(
                "recover token script failed\nstdout:\n$stdout\nstderr:\n$stderr",
                0,
                recover.exitValue(),
            )
            assertEquals(token, stdout)
        } finally {
            serverPid?.let(::killPid)
            tempHome.deleteRecursively()
        }
    }

    @Test
    fun `sync server state script should rewrite stale pid and token files to running server`() {
        assumeTrue(canRunCommand("bash"))
        assumeTrue(canRunCommand("python3"))

        val tempHome = Files.createTempDirectory("termux-pty-sync-state").toFile()
        val stateDir = File(tempHome, ".rikkahub").apply { mkdirs() }
        val pidFile = File(stateDir, "pty_session_server.pid")
        val tokenFile = File(stateDir, "pty_session_server.token")
        val scriptFile = File(stateDir, "pty_session_server.py")
        val token = "sync-token"
        val port = reservePort()
        var serverPid: Long? = null

        try {
            val sessionManager = allocateSessionManager()
            scriptFile.writeText(sessionManager.termuxPtyServerScriptForTest())

            serverPid = startDetachedPythonServer(
                homeDir = tempHome,
                scriptFile = scriptFile,
                port = port,
                token = token,
            )

            assertTrue(waitForHealth(port = port, token = token))
            pidFile.writeText("999999\n")
            tokenFile.writeText("stale-token\n")

            val sync = ProcessBuilder("bash", "-s")
                .directory(tempHome)
                .apply {
                    environment()["HOME"] = tempHome.absolutePath
                }
                .start()
            sync.outputStream.bufferedWriter().use { writer ->
                writer.write(
                    sessionManager.buildSyncServerStateScriptForTest(
                        port = port,
                        token = token,
                    )
                )
            }

            assertTrue("sync server state script timed out", sync.waitFor(10, TimeUnit.SECONDS))
            val stdout = sync.inputStream.bufferedReader().readText()
            val stderr = sync.errorStream.bufferedReader().readText()
            assertEquals(
                "sync server state script failed\nstdout:\n$stdout\nstderr:\n$stderr",
                0,
                sync.exitValue(),
            )
            assertEquals(serverPid, readPid(pidFile))
            assertEquals(token, tokenFile.readText().trim())
        } finally {
            serverPid?.let(::killPid)
            tempHome.deleteRecursively()
        }
    }

    @Test
    fun `stop server script should ignore unrelated pid file`() {
        assumeTrue(canRunCommand("bash"))

        val tempHome = Files.createTempDirectory("termux-pty-stop-script").toFile()
        val stateDir = File(tempHome, ".rikkahub").apply { mkdirs() }
        val pidFile = File(stateDir, "pty_session_server.pid")
        val tokenFile = File(stateDir, "pty_session_server.token")
        val port = reservePort()
        var unrelatedPid: Long? = null

        try {
            val sessionManager = allocateSessionManager()
            unrelatedPid = startDetachedProcess(
                homeDir = tempHome,
                command = "sleep 30",
            )
            pidFile.writeText("$unrelatedPid\n")
            tokenFile.writeText("stale-token\n")

            val stop = ProcessBuilder("bash", "-s")
                .directory(tempHome)
                .apply {
                    environment()["HOME"] = tempHome.absolutePath
                }
                .start()
            stop.outputStream.bufferedWriter().use { writer ->
                writer.write(sessionManager.buildStopServerScriptForTest(port))
            }

            assertTrue("stop server script timed out", stop.waitFor(10, TimeUnit.SECONDS))
            val stdout = stop.inputStream.bufferedReader().readText()
            val stderr = stop.errorStream.bufferedReader().readText()
            assertEquals(
                "stop server script failed\nstdout:\n$stdout\nstderr:\n$stderr",
                0,
                stop.exitValue(),
            )

            assertTrue("unrelated pid should still be alive", isPidAlive(requireNotNull(unrelatedPid)))
            assertFalse(pidFile.exists())
            assertFalse(tokenFile.exists())
        } finally {
            unrelatedPid?.let(::killPid)
            tempHome.deleteRecursively()
        }
    }

    @Test
    fun `finished session should remain visible in session list until cleanup`() {
        assumeTrue(canRunCommand("bash"))
        assumeTrue(canRunCommand("python3"))

        val tempHome = Files.createTempDirectory("termux-pty-finished-session").toFile()
        val scriptFile = File(tempHome, "pty_session_server.py")
        val token = "session-token"
        val port = reservePort()
        var serverPid: Long? = null

        try {
            val sessionManager = allocateSessionManager()
            scriptFile.writeText(sessionManager.termuxPtyServerScriptForLocalBash())

            serverPid = startDetachedPythonServer(
                homeDir = tempHome,
                scriptFile = scriptFile,
                port = port,
                token = token,
            )

            assertTrue(waitForHealth(port = port, token = token))

            val command = "printf 'hello-from-pty\\n'"
            val createResponse = postJson(
                port = port,
                token = token,
                path = "/sessions",
                body = """
                    {
                      "command": "$command",
                      "workdir": "${tempHome.absolutePath}",
                      "yield_time_ms": 400,
                      "max_output_chars": 12000,
                      "cols": 120,
                      "rows": 40
                    }
                """.trimIndent()
            )
            val toolResponse = JsonInstant.decodeFromString<TermuxPtyServerResponse>(createResponse)
            assertTrue(toolResponse.error.isNullOrBlank())

            val listResponse = waitForSessionList(port = port, token = token) { state ->
                state.sessions.any { session ->
                    session.command.contains("hello-from-pty") && !session.running && session.exitCode == 0
                }
            }

            assertNotNull("finished session was not retained in /sessions", listResponse)
            val retainedList = requireNotNull(listResponse)
            val session = retainedList.sessions.first { it.command.contains("hello-from-pty") }
            assertFalse(session.running)
            assertEquals(0, session.exitCode)
            assertTrue(session.pendingOutputChars == 0)
        } finally {
            serverPid?.let(::killPid)
            tempHome.deleteRecursively()
        }
    }

    private fun allocateSessionManager(): TermuxPtySessionManager {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null) as Unsafe
        return unsafe.allocateInstance(TermuxPtySessionManager::class.java) as TermuxPtySessionManager
    }

    private fun TermuxPtySessionManager.buildStopServerScriptForTest(port: Int): String {
        val method = javaClass.getDeclaredMethod(
            "buildStopServerScript",
            Int::class.javaPrimitiveType,
        )
        method.isAccessible = true
        return method.invoke(this, port) as String
    }

    private fun TermuxPtySessionManager.buildBootstrapScriptForTest(port: Int, token: String): String {
        val method = javaClass.getDeclaredMethod(
            "buildBootstrapScript",
            Int::class.javaPrimitiveType,
            String::class.java,
        )
        method.isAccessible = true
        return method.invoke(this, port, token) as String
    }

    private fun TermuxPtySessionManager.buildRecoverTokenScriptForTest(port: Int): String {
        val method = javaClass.getDeclaredMethod(
            "buildRecoverTokenScript",
            Int::class.javaPrimitiveType,
        )
        method.isAccessible = true
        return method.invoke(this, port) as String
    }

    private fun TermuxPtySessionManager.buildSyncServerStateScriptForTest(port: Int, token: String): String {
        val method = javaClass.getDeclaredMethod(
            "buildSyncServerStateScript",
            Int::class.javaPrimitiveType,
            String::class.java,
        )
        method.isAccessible = true
        return method.invoke(this, port, token) as String
    }

    private fun TermuxPtySessionManager.termuxPtyServerScriptForTest(): String {
        val method = javaClass.getDeclaredMethod("termuxPtyServerScript")
        method.isAccessible = true
        return method.invoke(this) as String
    }

    private fun TermuxPtySessionManager.termuxPtyServerScriptForLocalBash(): String {
        return termuxPtyServerScriptForTest().replace(
            "\"/data/data/com.termux/files/usr/bin/bash\"",
            "\"${resolveBashPath()}\"",
        )
    }

    private fun reservePort(): Int {
        ServerSocket(0).use { socket ->
            return socket.localPort
        }
    }

    private fun startDetachedPythonServer(
        homeDir: File,
        scriptFile: File,
        port: Int,
        token: String,
        exportEnvToken: Boolean = false,
    ): Long {
        val envPrefix = if (exportEnvToken) {
            "RIKKAHUB_PTY_SERVER_TOKEN='$token' "
        } else {
            ""
        }
        val process = ProcessBuilder(
            "bash",
            "-lc",
            "${envPrefix}python3 -u '${scriptFile.absolutePath}' --port '$port' --token '$token' >/dev/null 2>&1 < /dev/null & echo \$!",
        )
            .directory(homeDir)
            .redirectErrorStream(true)
            .apply {
                environment()["HOME"] = homeDir.absolutePath
            }
            .start()
        assertTrue("failed to start legacy server shell", process.waitFor(5, TimeUnit.SECONDS))
        return process.inputStream.bufferedReader().readText().trim().toLong()
    }

    private fun startDetachedProcess(
        homeDir: File,
        command: String,
    ): Long {
        val process = ProcessBuilder(
            "bash",
            "-lc",
            "nohup $command >/dev/null 2>&1 < /dev/null & echo \$!",
        )
            .directory(homeDir)
            .redirectErrorStream(true)
            .apply {
                environment()["HOME"] = homeDir.absolutePath
            }
            .start()
        assertTrue("failed to start detached process shell", process.waitFor(5, TimeUnit.SECONDS))
        return process.inputStream.bufferedReader().readText().trim().toLong()
    }

    private fun canRunCommand(command: String): Boolean {
        return runCatching {
            ProcessBuilder(command, "--version")
                .redirectErrorStream(true)
                .start()
                .waitFor(5, TimeUnit.SECONDS)
        }.getOrDefault(false)
    }

    private fun waitForHealth(
        port: Int,
        token: String,
        timeoutMs: Long = 10_000L,
    ): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            if (isHealthy(port = port, token = token)) return true
            Thread.sleep(100)
        }
        return false
    }

    private fun isHealthy(port: Int, token: String): Boolean {
        return runCatching {
            val connection = URL("http://127.0.0.1:$port/health").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 500
            connection.readTimeout = 500
            connection.setRequestProperty("X-RikkaHub-Token", token)
            connection.responseCode == 200
        }.getOrDefault(false)
    }

    private fun postJson(
        port: Int,
        token: String,
        path: String,
        body: String,
    ): String {
        val connection = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 1_000
        connection.readTimeout = 1_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("X-RikkaHub-Token", token)
        connection.outputStream.bufferedWriter().use { it.write(body) }
        assertEquals(200, connection.responseCode)
        return connection.inputStream.bufferedReader().readText()
    }

    private fun getSessionList(
        port: Int,
        token: String,
    ): TermuxPtySessionListResponse {
        val connection = URL("http://127.0.0.1:$port/sessions").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 1_000
        connection.readTimeout = 1_000
        connection.setRequestProperty("X-RikkaHub-Token", token)
        assertEquals(200, connection.responseCode)
        return JsonInstant.decodeFromString(connection.inputStream.bufferedReader().readText())
    }

    private fun waitForSessionList(
        port: Int,
        token: String,
        timeoutMs: Long = 5_000L,
        predicate: (TermuxPtySessionListResponse) -> Boolean,
    ): TermuxPtySessionListResponse? {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        var lastResponse: TermuxPtySessionListResponse? = null
        while (System.nanoTime() < deadline) {
            val response = runCatching { getSessionList(port = port, token = token) }.getOrNull()
            if (response != null) {
                lastResponse = response
            }
            if (response != null && predicate(response)) {
                return response
            }
            Thread.sleep(100)
        }
        return lastResponse?.takeIf(predicate)
    }

    private fun waitForPidExit(
        pid: Long,
        timeoutMs: Long = 5_000L,
    ): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            if (!isPidAlive(pid)) return true
            Thread.sleep(100)
        }
        return !isPidAlive(pid)
    }

    private fun readPid(pidFile: File): Long? {
        return pidFile.takeIf(File::exists)?.readText()?.trim()?.toLongOrNull()
    }

    private fun killPid(pid: Long) {
        runCatching {
            ProcessBuilder("kill", "-9", pid.toString())
                .start()
                .waitFor(5, TimeUnit.SECONDS)
        }
    }

    private fun isPidAlive(pid: Long): Boolean {
        return runCatching {
            val process = ProcessBuilder("kill", "-0", pid.toString()).start()
            process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0
        }.getOrDefault(false)
    }

    private fun resolveBashPath(): String {
        val process = ProcessBuilder("bash", "-lc", "command -v bash")
            .redirectErrorStream(true)
            .start()
        assertTrue("failed to resolve bash path", process.waitFor(5, TimeUnit.SECONDS))
        return process.inputStream.bufferedReader().readText().trim().ifBlank { "/bin/bash" }
    }
}
