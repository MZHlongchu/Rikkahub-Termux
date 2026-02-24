package me.rerere.rikkahub.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.rerere.rikkahub.data.db.DatabaseMigrationTracker

val Migration_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        DatabaseMigrationTracker.onMigrationStart(16, 17)
        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS scheduled_task_run (
                    id TEXT NOT NULL PRIMARY KEY,
                    task_id TEXT NOT NULL,
                    task_title_snapshot TEXT NOT NULL,
                    assistant_id_snapshot TEXT NOT NULL,
                    status TEXT NOT NULL,
                    started_at INTEGER NOT NULL,
                    finished_at INTEGER NOT NULL,
                    duration_ms INTEGER NOT NULL,
                    prompt_snapshot TEXT NOT NULL,
                    result_text TEXT NOT NULL,
                    error_text TEXT NOT NULL,
                    model_id_snapshot TEXT,
                    provider_name_snapshot TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_task_run_task_id ON scheduled_task_run(task_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_task_run_started_at ON scheduled_task_run(started_at)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_task_run_status ON scheduled_task_run(status)")
        } finally {
            DatabaseMigrationTracker.onMigrationEnd()
        }
    }
}
