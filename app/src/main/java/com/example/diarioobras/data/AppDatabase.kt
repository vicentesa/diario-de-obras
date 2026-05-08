package com.example.diarioobras.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@TypeConverters(StatusEtapaConverter::class)
@Database(
    entities = [
        ObraEntity::class,
        DiarioEntity::class,
        DeslocamentoItemEntity::class,
        CarregamentoItemEntity::class,
        DesvioItemEntity::class,
        ServicoEntity::class,
        ServicoAreaEntity::class,
        SubservicoEntity::class
    ],
    version = 18,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun obrasDao(): ObrasDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Versões 17 e 18 foram bumps sem alteração de schema.
        private val MIGRATION_16_18 = object : Migration(16, 18) {
            override fun migrate(db: SupportSQLiteDatabase) { /* sem mudança de schema */ }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diario_obras.db"
                )
                    .addMigrations(MIGRATION_16_18)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}