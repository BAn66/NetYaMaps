package ru.netology.netologyyamaps.dto

data class Place (
    val id: Long = 0,
    val lat: Double, //ширина
    val long: Double, //долгота
    val name: String, //имя метки
)