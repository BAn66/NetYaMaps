package ru.netology.netologyyamaps.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.netologyyamaps.dto.Place

@Entity
data class PlaceEntity constructor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double, //ширина
    val longtitude: Double, //долгота
    val name: String, //имя метки
) {
    companion object{
        fun fromDto(dto: Place): PlaceEntity = with(dto){
            PlaceEntity(id = id, latitude = lat, longtitude = long, name = name)
        }
    }

    fun toDto(): Place = Place(id= id, lat = latitude, long = longtitude, name)
}