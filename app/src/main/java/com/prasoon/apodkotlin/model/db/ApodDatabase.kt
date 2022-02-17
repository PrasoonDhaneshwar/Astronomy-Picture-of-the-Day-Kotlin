package com.prasoon.apodkotlin.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.prasoon.apodkotlin.model.ApodModel
import com.prasoon.apodkotlin.model.Constants.APOD_DATABASE_NAME

@Database(entities = [ApodModel::class], version = 1)
abstract class ApodDatabase : RoomDatabase() {

    abstract fun apodModelDao(): ApodDao

    companion object {
        @Volatile
        private var instance: ApodDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: buildDatabase(context).also {
                instance = it
            }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                ApodDatabase::class.java,
                APOD_DATABASE_NAME
            ).build()
    }
}