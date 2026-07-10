package me.rerere.rikkahub.data.sync

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.json.Json
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.db.AppDatabase
import me.rerere.rikkahub.utils.fileSizeToString
import java.io.File

private const val TAG = "BackupCompatibility"

internal const val UPSTREAM_DATABASE_VERSION = 24
internal const val UPSTREAM_IDENTITY_HASH = "0ea1aaebfa031c7995c45a1e35822e1a"

object BackupCompatibility {
    fun encodeUpstreamSettings(json: Json, settings: Settings): String {
        return UpstreamSettingsCompatibility.encode(json, settings)
    }

    /**
     * Builds a standalone database with the exact upstream v24 Room schema.
     *
     * The source database stays in a Room transaction while rows are copied into an independent
     * SQLite database. This avoids both raw database/WAL races and connection-pool-dependent
     * ATTACH behavior, while excluding fork-only tables, columns, and derived FTS tables.
     */
    fun createUpstreamDatabaseCopy(context: Context, database: AppDatabase): File {
        val target = File.createTempFile("upstream_rikka_hub_", ".db", context.cacheDir)
        val sourceDb = database.openHelper.writableDatabase
        try {
            SQLiteDatabase.openOrCreateDatabase(target, null).use { targetDb ->
                targetDb.setForeignKeyConstraintsEnabled(true)
                sourceDb.beginTransaction()
                try {
                    targetDb.beginTransaction()
                    try {
                        createUpstreamSchema(targetDb)
                        copySharedData(sourceDb, targetDb)
                        writeRoomMetadata(targetDb)
                        targetDb.setTransactionSuccessful()
                    } finally {
                        targetDb.endTransaction()
                    }

                    validateUpstreamDatabase(sourceDb, targetDb)
                    sourceDb.setTransactionSuccessful()
                } finally {
                    sourceDb.endTransaction()
                }
            }
        } catch (error: Throwable) {
            target.delete()
            Log.e(TAG, "Failed to create upstream-compatible database", error)
            throw IllegalStateException(
                "Unable to create upstream-compatible database: ${error.message}",
                error
            )
        }

        Log.i(TAG, "Created ${target.name} (${target.length().fileSizeToString()})")
        return target
    }
}

private fun createUpstreamSchema(db: SQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE `ConversationEntity` (
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
        CREATE TABLE `MemoryEntity` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `assistant_id` TEXT NOT NULL,
            `content` TEXT NOT NULL
        )
        """.trimIndent()
    )
    db.execSQL(
        """
        CREATE TABLE `GenMediaEntity` (
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
        CREATE TABLE `message_node` (
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
        "CREATE INDEX `index_message_node_conversation_id` " +
            "ON `message_node` (`conversation_id`)"
    )
    db.execSQL(
        """
        CREATE TABLE `managed_files` (
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
        "CREATE UNIQUE INDEX `index_managed_files_relative_path` " +
            "ON `managed_files` (`relative_path`)"
    )
    db.execSQL(
        "CREATE INDEX `index_managed_files_folder` ON `managed_files` (`folder`)"
    )
    db.execSQL(
        """
        CREATE TABLE `favorites` (
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
        "CREATE UNIQUE INDEX `index_favorites_ref_key` ON `favorites` (`ref_key`)"
    )
    db.execSQL("CREATE INDEX `index_favorites_type` ON `favorites` (`type`)")
    db.execSQL(
        "CREATE INDEX `index_favorites_created_at` ON `favorites` (`created_at`)"
    )
    db.execSQL(
        """
        CREATE TABLE `workspaces` (
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
        "CREATE UNIQUE INDEX `index_workspaces_root` ON `workspaces` (`root`)"
    )
    db.execSQL(
        "CREATE INDEX `index_workspaces_updated_at` ON `workspaces` (`updated_at`)"
    )
    db.execSQL(
        """
        CREATE TABLE `conversation_folder` (
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
        "CREATE INDEX `index_conversation_folder_assistant_id` " +
            "ON `conversation_folder` (`assistant_id`)"
    )
}

private fun copySharedData(source: SupportSQLiteDatabase, target: SQLiteDatabase) {
    copyQueryToTable(
        source = source,
        target = target,
        table = "ConversationEntity",
        columns = UPSTREAM_TABLE_COLUMNS.getValue("ConversationEntity"),
        query = """
            SELECT
                `id`, `assistant_id`, `title`, `nodes`, `create_at`, `update_at`, `suggestions`,
                `is_pinned`, `custom_system_prompt`, `mode_injection_ids`, `lorebook_ids`, '', ''
            FROM `ConversationEntity`
        """.trimIndent()
    )
    copyTable(source, target, "MemoryEntity")
    copyTable(source, target, "GenMediaEntity")
    copyQueryToTable(
        source = source,
        target = target,
        table = "message_node",
        columns = UPSTREAM_TABLE_COLUMNS.getValue("message_node"),
        query = """
            SELECT m.`id`, m.`conversation_id`, m.`node_index`, m.`messages`, m.`select_index`
            FROM `message_node` AS m
            INNER JOIN `ConversationEntity` AS c ON c.`id` = m.`conversation_id`
        """.trimIndent()
    )
    copyTable(source, target, "managed_files")
    copyTable(source, target, "favorites")
}

private fun copyTable(source: SupportSQLiteDatabase, target: SQLiteDatabase, table: String) {
    val columns = UPSTREAM_TABLE_COLUMNS.getValue(table)
    val projection = columns.joinToString { "`$it`" }
    copyQueryToTable(
        source = source,
        target = target,
        table = table,
        columns = columns,
        query = "SELECT $projection FROM `$table`"
    )
}

private fun copyQueryToTable(
    source: SupportSQLiteDatabase,
    target: SQLiteDatabase,
    table: String,
    columns: List<String>,
    query: String,
) {
    val columnSql = columns.joinToString { "`$it`" }
    val placeholders = List(columns.size) { "?" }.joinToString()
    target.compileStatement("INSERT INTO `$table` ($columnSql) VALUES ($placeholders)").use { statement ->
        source.query(query).use { cursor ->
            check(cursor.columnCount == columns.size) {
                "Unexpected query column count for $table: ${cursor.columnCount}"
            }
            while (cursor.moveToNext()) {
                statement.clearBindings()
                for (index in columns.indices) {
                    statement.bindCursorValue(index + 1, cursor, index)
                }
                statement.executeInsert()
            }
        }
    }
}

private fun SQLiteStatement.bindCursorValue(bindIndex: Int, cursor: Cursor, columnIndex: Int) {
    when (cursor.getType(columnIndex)) {
        Cursor.FIELD_TYPE_NULL -> bindNull(bindIndex)
        Cursor.FIELD_TYPE_INTEGER -> bindLong(bindIndex, cursor.getLong(columnIndex))
        Cursor.FIELD_TYPE_FLOAT -> bindDouble(bindIndex, cursor.getDouble(columnIndex))
        Cursor.FIELD_TYPE_BLOB -> bindBlob(bindIndex, cursor.getBlob(columnIndex))
        Cursor.FIELD_TYPE_STRING -> bindString(bindIndex, cursor.getString(columnIndex))
        else -> error("Unsupported SQLite value type at column $columnIndex")
    }
}

private fun writeRoomMetadata(db: SQLiteDatabase) {
    db.execSQL(
        "CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)"
    )
    db.execSQL(
        "INSERT INTO room_master_table (id, identity_hash) VALUES(42, ?)",
        arrayOf(UPSTREAM_IDENTITY_HASH)
    )
    db.version = UPSTREAM_DATABASE_VERSION
}

private fun validateUpstreamDatabase(source: SupportSQLiteDatabase, target: SQLiteDatabase) {
    val expectedTables = UPSTREAM_TABLE_COLUMNS.keys + "room_master_table"
    val actualTables = target.rawQuery(
        "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
        null
    ).use { cursor ->
        buildSet {
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
    }
    check(actualTables - ANDROID_INTERNAL_TABLES == expectedTables) {
        "Unexpected upstream database tables: expected=$expectedTables, actual=$actualTables"
    }

    UPSTREAM_TABLE_COLUMNS.forEach { (table, expectedColumns) ->
        val actualColumns = target.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
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
        val sourceCount = source.longForQuery(sourceCountQuery(table))
        val targetCount = target.longForQuery("SELECT COUNT(*) FROM `$table`")
        check(sourceCount == targetCount) {
            "Row count mismatch for $table: source=$sourceCount, target=$targetCount"
        }
    }

    check(target.stringForQuery("PRAGMA integrity_check") == "ok") {
        "Upstream database integrity check failed"
    }
    target.rawQuery("PRAGMA foreign_key_check", null).use { cursor ->
        check(!cursor.moveToFirst()) { "Upstream database foreign key check failed" }
    }
    check(target.version == UPSTREAM_DATABASE_VERSION)
    check(
        target.stringForQuery(
            "SELECT identity_hash FROM room_master_table WHERE id = 42"
        ) == UPSTREAM_IDENTITY_HASH
    )
}

private fun sourceCountQuery(table: String): String = when (table) {
    "message_node" -> {
        """
        SELECT COUNT(*)
        FROM `message_node` AS m
        INNER JOIN `ConversationEntity` AS c ON c.`id` = m.`conversation_id`
        """.trimIndent()
    }

    else -> "SELECT COUNT(*) FROM `$table`"
}

private fun SupportSQLiteDatabase.longForQuery(sql: String): Long = query(sql).use { cursor ->
    check(cursor.moveToFirst()) { "Query returned no rows: $sql" }
    cursor.getLong(0)
}

private fun SQLiteDatabase.longForQuery(sql: String): Long = rawQuery(sql, null).use { cursor ->
    check(cursor.moveToFirst()) { "Query returned no rows: $sql" }
    cursor.getLong(0)
}

private fun SQLiteDatabase.stringForQuery(sql: String): String = rawQuery(sql, null).use { cursor ->
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

private val ANDROID_INTERNAL_TABLES = setOf("android_metadata")
