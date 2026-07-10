package me.rerere.rikkahub.data.sync

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupCompatibilityArchiveTest {
    @Test
    fun `database archive includes sidecars that clear destination wal state`() {
        val archive = createArchive(includeSidecars = true)
        try {
            BackupCompatibility.validateBackupArchive(archive, requireDatabase = true)
        } finally {
            archive.delete()
        }
    }

    @Test
    fun `database archive without sidecars is rejected`() {
        val archive = createArchive(includeSidecars = false)
        try {
            val error = runCatching {
                BackupCompatibility.validateBackupArchive(archive, requireDatabase = true)
            }.exceptionOrNull()
            assertTrue(error is IllegalStateException)
        } finally {
            archive.delete()
        }
    }

    private fun createArchive(includeSidecars: Boolean): File {
        val archive = File.createTempFile("backup-compatibility-", ".zip")
        ZipOutputStream(FileOutputStream(archive)).use { zip ->
            zip.addEntry("settings.json", "{}".toByteArray())
            zip.addEntry("rikka_hub.db", byteArrayOf(1))
            if (includeSidecars) {
                UPSTREAM_DATABASE_SIDECAR_ENTRIES.forEach { zip.addEntry(it, byteArrayOf()) }
            }
        }
        return archive
    }
}

private fun ZipOutputStream.addEntry(name: String, content: ByteArray) {
    putNextEntry(ZipEntry(name))
    write(content)
    closeEntry()
}
