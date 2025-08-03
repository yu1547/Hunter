package com.ntou01157.hunter.models

data class Spot(
    val spotId: String,
    val spotName: String,
    val spotPhoto: String,
    val latitude: Double,
    val longitude: Double
)

object MockSpotData {
    val pages = listOf(
        listOf(
            Spot("1", "地標A", "", 25.15102, 121.78015),
            Spot("2", "海大甜甜圈", "",0.0, 0.0),
            Spot("3", "地標C", "", 0.0, 0.0),
            Spot("4", "地標D", "", 0.0, 0.0)
        ),
        listOf(
            Spot("5", "地標E", "", 0.0, 0.0),
            Spot("6", "地標F", "", 0.0, 0.0),
            Spot("7", "地標G", "", 0.0, 0.0),
            Spot("8", "地標H", "", 0.0, 0.0)
        ),
        listOf(
            Spot("9", "地標I", "", 0.0, 0.0),
            Spot("10", "地標J", "", 0.0, 0.0)
        )
    )
    // 取得所有打卡點
    val allSpots: List<Spot> = pages.flatten()
}