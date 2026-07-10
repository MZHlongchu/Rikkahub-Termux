package me.rerere.rikkahub.data.sync

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.json.Json
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.db.AppDatabase
import me.rerere.rikkahub.utils.fileSizeToString
import java.io.File

private const val TAG = "BackupCompatibility"
private const val BACKUP_SCHEMA = "upstream_backup"

internal const val UPSTREAM_DATABASE_VERSION = 24
internal const val UPSTREAM_IDENTITY_HASH = "0ea1aaebfa031c7995c45a1e35822e1a"

object BackupCompatibility {
    fun encodeUpstreamSettings(json: Json, settings: Settings): String {
        return UpstreamSettingsCompatibility.encode(json, settings)
    }

    /**
     * Builds a standalone database with the exact upstream v24 Room schema.
     *
     * The source database stays attached to Room while all shared rows are copied in one
     * transaction. This avoids racing a raw database/WAL file copy and intentionally excludes
     * fork-only tables, columns, and derived FTS tables.
     */
    fun createUpstreamDatabaseCopy(context: Context, database: AppDatabase): File {
        val target = File.createTempFile("upstream_rikka_hub_", ".db", context.cacheDir)
        check(target.delete()) { "Unable to prepare temporary upstream database" }

        val db = database.openHelper.writableDatabase
        var attached = false
        try {
            db.execSQL("ATTACH DATABASE ? AS $BACKUP_SCHEMA", arrayOf(target.absolutePath))
            attached = true

            db.beginTransaction()
            try {
                createUpstreamSchema(db)
                copySharedData(db)
                writeRoomMetadata(db)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            validateUpstreamDatabase(db)
            db.execSQL("DETACH DATABASE $BACKUP_SCHEMA")
            attached = false
        } catch (error: Throwable) {
            if (attached) {
                runCatching { db.execSQL("DETACH DATABASE $BACKUP_SCHEMA") }
            }
            target.delete()
            Log.e(TAG, "Failed to create upstream-compatible database", error)
            throw IllegalStateException("Unable to create upstream-compatible database", error)
        }

        Log.i(TAG, "Created ${target.name} (${target.length().fileSizeToString()})")
        return target
    }
}

private fun createUpstreamSchema(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE $BACKUP_SCHEMA.`ConversationEntity` (
            `id` TEXT NOT NULL,
            `assistant_id` TEXT NOT NULL DEFAULT '0950e2dc-9bd5-4801-afa3-aa887aa36b4e',
            `title` TEXT NOT NULL,
            `nodes` TEXT NOT NULL,
            `create_at` INTEGER NOT NULL,
            `update_at` INTEGER NOT NULL,
            `suggestions` TEXT NOT NULL DEFAULT '[]',
            `is_pinned` INTEGER NOT NULL DEFAULT 0,
            `custom_system_prompt` TEXT NOT NULL DEFAULT '',
            `mode_injection_ids` TEXT NOT NULL DEFAULT '[]',
            `lorebook_ids` TEXT NOT NULL DEFAULT '[]',
            `workspace_cwd` TEXT NOT NULL DEFAULT '',
            `folder_id` TEXT NOT NULL DEFAULT '',
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )
    db.execSQL(
        """
        CREATE TABLE $BACKUP_SCHEMA.`MemoryEntity` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `assistant_id` TEXT NOT NULL,
            `content` TEXT NOT NULL
        )
        """.trimIndent()
    )
    db.execSQL(
        """
        CREATE TABLE $BACKUP_SCHEMA.`GenMediaEntity` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `path` TEXT NOT NULL,
            `model_id` TEXT NOT NULL,
            `prompt` TEXT NOT NULL,
            `create_at` INTEGER NOT NULL,
            `type` TEXT NOT NULL DEFAULT 'image_generation',
            `source_paths` TEXT
        )
        """.trimIndent()
    )
    db.execSQL(
        """
        CREATE TABLE $BACKUP_SCHEMA.`message_node` (
            `id` TEXT NOT NULL,
            `conversation_id` TEXT NOT NULL,
            `node_index` INTEGER NOT NULL,
            `messages` TEXT NOT NULL,
            `select_index` INTEGER NOT NULL,
            PRIMARY KEY(`id`),
            FOREIGN KEY(`conversation_id`) REFERENCES `ConversationEntity`(`id`)
                ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent()
    )
    db.execSQL(
        "CREATE INDEX $BACKUP_SCHEMA.`index_message_node_conversation_id` " +
            "ON `message_node` (`conversation_id`)"
    )
    db.execSQL(
        """
        CREATE TABLE $BACKUP_SCHEMA.`managed_files` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `folder` TEXT NOT NULL,
            `relative_path` TEXT NOT NULL,
            `display_name` TEXT NOT NULL,
            `mime_type` TEXT NOT NULL,
            `size_bytes` INTEGER NOT NULL,
            `created_at` INTEGER NOT NULL,
            `updated_at` INTEGER NOT NULL
        )
        """.trimIndent()
    )
    db.execSQL(
        "CREATE UNIQUE INDEX $BACKUP_SCHEMA.`index_managed_files_relative_path` " +
            "ON `managed_files` (`relative_path`)"
    )
    db.execSQL(
        "CREATE INDEX $BACKUP_SCHEMA.`index_managed_files_folder` ON `managed_files` (`folder`)"
    )
    db.execSQL(
        """
        CREATE TABLE $BACKUP_SCHEMA.`favorites` (
            `id` TEXT NOT NULL,
            `type` TEXT NOT NULL,
            `ref_key` TEXT NOT NULL,
            `ref_json` TEXT NOT NULL,
            `snapshot_json` TEXT NOT NULL,
            `meta_json` TEXT,
            `created_at` INTEGER NOT NULL,
            `updated_at` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )
    db.execSQL(
        "CREATE UNIQUE INDEX $BACKUP_SCHEMA.`index_favorites_ref_key` ON `favorites` (`ref_key`)"
    )
    db.execSQL("CREATE INDEX $BACKUP_SCHEMA.`index_favorites_type` ON `favorites` (`type`)")
    db.execSQL(
        "CREATE INDEX $BACKUP_SCHEMA.`index_favorites_created_at` ON `favorites` (`created_at`)"
    )
    db.execSQL(
        """
        CREATE TABLE $BACKUP_SCHEMA.`workspaces` (
            `id` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `root` TEXT NOT NULL,
            `shell_status` TEXT NOT NULL,
            `created_at` INTEGER NOT NULL,
            `updated_at` INTEGER NOT NULL,
            `last_access_at` INTEGER,
            `tool_approvals` TEXT NOT NULL DEFAULT '{}',
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )
    db.execSQL(
        "CREATE UNIQUE INDEX $BACKUP_SCHEMA.`index_workspaces_root` ON `workspaces` (`root`)"
    )
    db.execSQL(
        "CREATE INDEX $BACKUP_SCHEMA.`index_workspaces_updated_at` ON `workspaces` (`updated_at`)"
    )
    db.execSQL(
        """
        CREATE TABLE $BACKUP_SCHEMA.`conversation_folder` (
            `id` TEXT NOT NULL,
            `assistant_id` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `sort_index` INTEGER NOT NULL DEFAULT 0,
            `create_at` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )
    db.execSQL(
        "CREATE INDEX $BACKUP_SCHEMA.`index_conversation_folder_assistant_id` " +
            "ON `conversation_folder` (`assistant_id`)"
    )
}

private fun copySharedData(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        INSERT INTO $BACKUP_SCHEMA.`ConversationEntity` (
            `id`, `assistant_id`, `title`, `nodes`, `create_at`, `update_at`, `suggestions`,
            `is_pinned`, `custom_system_prompt`, `mode_injection_ids`, `lorebook_ids`,
            `workspace_cwd`, `folder_id`
        )
        SELECT
            `id`, `assistant_id`, `title`, `nodes`, `create_at`, `update_at`, `suggestions`,
            `is_pinned`, `custom_system_prompt`, `mode_injection_ids`, `lorebook_ids`, '', ''
        FROM main.`ConversationEntity`
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO $BACKUP_SCHEMA.`MemoryEntity` (`id`, `assistant_id`, `content`)
        SELECT `id`, `assistant_id`, `content` FROM main.`MemoryEntity`
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO $BACKUP_SCHEMA.`GenMediaEntity` (
            `id`, `path`, `model_id`, `prompt`, `create_at`, `type`, `source_paths`
        )
        SELECT `id`, `path`, `model_id`, `prompt`, `create_at`, `type`, `source_paths`
        FROM main.`GenMediaEntity`
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO $BACKUP_SCHEMA.`message_node` (
            `id`, `conversation_id`, `node_index`, `messages`, `select_index`
        )
        SELECT `id`, `conversation_id`, `node_index`, `messages`, `select_index`
        FROM main.`message_node`
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO $BACKUP_SCHEMA.`managed_files` (
            `id`, `folder`, `relative_path`, `display_name`, `mime_type`, `size_bytes`,
            `created_at`, `updated_at`
        )
        SELECT
            `id`, `folder`, `relative_path`, `display_name`, `mime_type`, `size_bytes`,
            `created_at`, `updated_at`
        FROM main.`managed_files`
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO $BACKUP_SCHEMA.`favorites` (
            `id`, `type`, `ref_key`, `ref_json`, `snapshot_json`, `meta_json`,
            `created_at`, `updated_at`
        )
        SELECT
            `id`, `type`, `ref_key`, `ref_json`, `snapshot_json`, `meta_json`,
            `created_at`, `updated_at`
        FROM main.`favorites`
        """.trimIndent()
    )
}

private fun writeRoomMetadata(db: SupportSQLiteDatabase) {
    db.execSQL(
        "CREATE TABLE $BACKUP_SCHEMA.room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)"
    )
    db.execSQL(
        "INSERT INTO $BACKUP_SCHEMA.room_master_table (id, identity_hash) VALUES(42, ?)",
        arrayOf(UPSTREAM_IDENTITY_HASH)
    )
    db.execSQL("PRAGMA $BACKUP_SCHEMA.user_version = $UPSTREAM_DATABASE_VERSION")
}

private fun validateUpstreamDatabase(db: SupportSQLiteDatabase) {
    val expectedTables = UPSTREAM_TABLE_COLUMNS.keys + "room_master_table"
    val actualTables = db.query(
        "SELECT name FROM $BACKUP_SCHEMA.sqlite_master " +
            "WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"
    ).use { cursor ->
        buildSet {
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
    }
    check(actualTables == expectedTables) {
        "Unexpected upstream database tables: expected=$expectedTables, actual=$actualTables"
    }

    UPSTREAM_TABLE_COLUMNS.forEach { (table, expectedColumns) ->
        val actualColumns = db.query("PRAGMA $BACKUP_SCHEMA.table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }
        check(actualColumns == expectedColumns) {
            "Unexpected columns for $table: expected=$expectedColumns, actual=$actualColumns"
        }
    }

    SHARED_TABLES.forEach { table ->
        val sourceCount = db.longForQuery("SELECT COUNT(*) FROM main.`$table`")
        val targetCount = db.longForQuery("SELECT COUNT(*) FROM $BACKUP_SCHEMA.`$table`")
        check(sourceCount == targetCount) {
            "Row count mismatch for $table: source=$sourceCount, target=$targetCount"
        }
    }

    check(db.stringForQuery("PRAGMA $BACKUP_SCHEMA.integrity_check") == "ok") {
        "Upstream database integrity check failed"
    }
    db.query("PRAGMA $BACKUP_SCHEMA.foreign_key_check").use { cursor ->
        check(!cursor.moveToFirst()) { "Upstream database foreign key check failed" }
    }
    check(db.longForQuery("PRAGMA $BACKUP_SCHEMA.user_version") == UPSTREAM_DATABASE_VERSION.toLong())
    check(
        db.stringForQuery(
            "SELECT identity_hash FROM $BACKUP_SCHEMA.room_master_table WHERE id = 42"
        ) == UPSTREAM_IDENTITY_HASH
    )
}

private fun SupportSQLiteDatabase.longForQuery(sql: String): Long = query(sql).use { cursor ->
    check(cursor.moveToFirst()) { "Query returned no rows: $sql" }
    cursor.getLong(0)
}

private fun SupportSQLiteDatabase.stringForQuery(sql: String): String = query(sql).use { cursor ->
    check(cursor.moveToFirst()) { "Query returned no rows: $sql" }
    cursor.getString(0)
}

internal val UPSTREAM_TABLE_COLUMNS = linkedMapOf(
    "ConversationEntity" to listOf(
        "id", "assistant_id", "title", "nodes", "create_at", "update_at", "suggestions",
        "is_pinned", "custom_system_prompt", "mode_injection_ids", "lorebook_ids",
        "workspace_cwd", "folder_id"
    ),
    "MemoryEntity" to listOf("id", "assistant_id", "content"),
    "GenMediaEntity" to listOf("id", "path", "model_id", "prompt", "create_at", "type", "source_paths"),
    "message_node" to listOf("id", "conversation_id", "node_index", "messages", "select_index"),
    "managed_files" to listOf(
        "id", "folder", "relative_path", "display_name", "mime_type", "size_bytes", "created_at", "updated_at"
    ),
    "favorites" to listOf(
        "id", "type", "ref_key", "ref_json", "snapshot_json", "meta_json", "created_at", "updated_at"
    ),
    "workspaces" to listOf(
        "id", "name", "root", "shell_status", "created_at", "updated_at", "last_access_at", "tool_approvals"
    ),
    "conversation_folder" to listOf("id", "assistant_id", "name", "sort_index", "create_at"),
)

private val SHARED_TABLES = listOf(
    "ConversationEntity",
    "MemoryEntity",
    "GenMediaEntity",
    "message_node",
    "managed_files",
    "favorites",
)
