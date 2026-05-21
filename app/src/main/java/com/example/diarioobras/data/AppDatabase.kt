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
        SubservicoEntity::class,
        AbastecimentoItemEntity::class
    ],
    version = 24,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun obrasDao(): ObrasDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_16_18 = object : Migration(16, 18) {
            override fun migrate(db: SupportSQLiteDatabase) { /* sem mudança de schema */ }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE obras ADD COLUMN espessuraContratoCm REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE carregamentos ADD COLUMN latitude REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE carregamentos ADD COLUMN longitude REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diarios ADD COLUMN horarioPontoCidade TEXT")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `abastecimentos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `diarioId` INTEGER NOT NULL,
                        `veiculo` TEXT NOT NULL DEFAULT '',
                        `litros` REAL NOT NULL DEFAULT 0.0,
                        `fotoTicketUri` TEXT NOT NULL DEFAULT '',
                        `horario` TEXT,
                        FOREIGN KEY(`diarioId`) REFERENCES `diarios`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_abastecimentos_diarioId` ON `abastecimentos` (`diarioId`)")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE desvios ADD COLUMN litros REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE desvios ADD COLUMN fotoTicketUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE desvios ADD COLUMN latitude REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE desvios ADD COLUMN longitude REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE desvios ADD COLUMN endereco TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE servicos ADD COLUMN observacoes TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diario_obras.db"
                )
                    .addMigrations(
                        MIGRATION_16_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23,
                        MIGRATION_23_24
                    )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}