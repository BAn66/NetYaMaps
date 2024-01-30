package ru.netology.netologyyamaps.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.netology.netologyyamaps.dao.PlaceDao
import ru.netology.netologyyamaps.entity.PlaceEntity


@Database(entities = [PlaceEntity::class], version = 1)
abstract class PlaceDatabase : RoomDatabase() {
    abstract val placeDao: PlaceDao

    companion object {
        @Volatile
        private var INSTANCE: PlaceDatabase? = null

        fun getInstance(context: Context): PlaceDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context, PlaceDatabase::class.java, "place_db")
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
    }
}