package me.rerere.rikkahub.data.sync

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.rerere.rikkahub.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupCompatibilityDatabaseTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun exportedDatabaseMatchesUpstreamV24Contract() {
        val sourceName = "backup-compatibility-source"
        context.deleteDatabase(sourceName)
        val source = Room.databaseBuilder(context, AppDatabase::class.java, sourceName)
            .allowMainThreadQueries()
            .build()

        val conversationId = "00000000-0000-0000-0000-000000000001"
        val nodeId = "00000000-0000-0000-0000-000000000002"
        source.openHelper.writableDatabase.apply {
            execSQL(
                """
                INSERT INTO ConversationEntity (
                    id, assistant_id, title, nodes, st_local_variables, create_at, update_at,
                    replacement_history, compression_revisions, suggestions, is_pinned,
                    custom_system_prompt, mode_injection_ids, lorebook_ids
                ) VALUES (?, ?, ?, '[]', '{}', 1, 2, '[]', '[]', '[]', 0, '', '[]', '[]')
                """.trimIndent(),
                arrayOf(
                    conversationId,
                    "0950e2dc-9bd5-4801-afa3-aa887aa36b4e",
                    "Compatibility test",
                )
            )
            execSQL(
                """
                INSERT INTO message_node (id, conversation_id, node_index, messages, select_index)
                VALUES (?, ?, 0, '[]', 0)
                """.trimIndent(),
                arrayOf(nodeId, conversationId)
            )
        }

        val exported = try {
            BackupCompatibility.createUpstreamDatabaseCopy(context, source)
        } finally {
            source.close()
            context.deleteDatabase(sourceName)
        }

        try {
            SQLiteDatabase.openDatabase(exported.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                assertEquals(UPSTREAM_DATABASE_VERSION, db.version)
                assertEquals(
                    UPSTREAM_IDENTITY_HASH,
                    db.rawQuery(
                        "SELECT identity_hash FROM room_master_table WHERE id = 42",
                        null
                    ).singleString()
                )

                val tables = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
                    null
                ).stringSet()
                assertEquals(
                    UPSTREAM_TABLE_COLUMNS.keys + "room_master_table",
                    tables - "android_metadata"
                )
                assertFalse("scheduled_task_run" in tables)
                assertFalse("message_fts" in tables)

                UPSTREAM_TABLE_COLUMNS.forEach { (table, expectedColumns) ->
                    val columns = db.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
                        val nameIndex = cursor.getColumnIndexOrThrow("name")
                        buildList {
                            while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                        }
                    }
                    assertEquals("Unexpected columns for $table", expectedColumns, columns)
                }

                assertEquals(1L, db.rawQuery("SELECT COUNT(*) FROM ConversationEntity", null).singleLong())
                assertEquals(1L, db.rawQuery("SELECT COUNT(*) FROM message_node", null).singleLong())
                assertEquals(
                    "Compatibility test",
                    db.rawQuery(
                        "SELECT title FROM ConversationEntity WHERE id = ?",
                        arrayOf(conversationId)
                    ).singleString()
                )

                val indices = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type = 'index' AND name NOT LIKE 'sqlite_%'",
                    null
                ).stringSet()
                assertEquals(EXPECTED_UPSTREAM_INDICES, indices)
                assertEquals("ok", db.rawQuery("PRAGMA integrity_check", null).singleString())
                db.rawQuery("PRAGMA foreign_key_check", null).use { cursor ->
                    assertFalse(cursor.moveToFirst())
                }
            }
        } finally {
            assertTrue(exported.delete())
        }
    }
}

private val EXPECTED_UPSTREAM_INDICES = setOf(
    "index_message_node_conversation_id",
    "index_managed_files_relative_path",
    "index_managed_files_folder",
    "index_favorites_ref_key",
    "index_favorites_type",
    "index_favorites_created_at",
    "index_workspaces_root",
    "index_workspaces_updated_at",
    "index_conversation_folder_assistant_id",
)

private fun android.database.Cursor.singleString(): String = use { cursor ->
    check(cursor.moveToFirst())
    cursor.getString(0)
}

private fun android.database.Cursor.singleLong(): Long = use { cursor ->
    check(cursor.moveToFirst())
    cursor.getLong(0)
}

private fun android.database.Cursor.stringSet(): Set<String> = use { cursor ->
    buildSet {
        while (cursor.moveToNext()) add(cursor.getString(0))
    }
}
