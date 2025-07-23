package com.ntou01157.hunter.models

data class Supply(
    val supplyId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

object SupplyRepository {
    val supplyStations = listOf(
        Supply(
            supplyId = "station1",
            name = "補給站 1",
            latitude = 25.149034,
            longitude = 121.779087
        ),
        Supply(
            supplyId = "station2",
            name = "補給站 2",
            latitude = 25.149836,
            longitude = 121.779452
        )
    )
}