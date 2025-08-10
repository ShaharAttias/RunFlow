package com.example.runflow.model

data class Run(
    val durationMillis: Long = 0L,
    val distanceKm: Float = 0f,
    val pace: String = "",
    val pathPoints: List<MyLatLng> = emptyList()
)
