package com.keshav.ai

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.keshav.ai.data.local.KeshavDatabase

class KeshavApplication : Application() {
    val database: KeshavDatabase by lazy { Room.databaseBuilder(this, KeshavDatabase::class.java, "keshav.db").addMigrations(MIGRATION_1_2).build() }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentNames TEXT NOT NULL DEFAULT ''") }
}
