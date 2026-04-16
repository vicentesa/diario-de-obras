package com.example.diarioobras.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ObraEntity::class,
        DiarioEntity::class,
        DeslocamentoItemEntity::class,
        CarregamentoItemEntity::class,
        DesvioItemEntity::class,
        ServicoEntity::class,
        SubservicoEntity::class
    ],
    version = 7,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun obrasDao(): ObrasDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diario_obras.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}