package com.ntou01157.hunter.models

data class Spot(
    val spotId: String,        // 從 MongoDB 的 "_id" 欄位轉過來
    val spotName: String,      // 英文代碼（用來對應圖片、比對解鎖紀錄）
    val ChName: String,        // ✅ 中文名稱（顯示在 UI）
    val latitude: Double,
    val longitude: Double
)


object MockSpotData {
    val pages = listOf(
        listOf(
            Spot("1", "地標A", "", 0.0, 0.0),
            Spot("2", "寰宇之", "",0.0, 0.0),
            Spot("3", "地標C", "", 0.0, 0.0),
            Spot("4", "地標D", "", 0.0, 0.0)
        ),
        listOf(
            Spot("5", "地標E", "", 0.0, 0.0),
            Spot("6", "地標F", "", 0.0, 0.0),
            Spot("7", "地標G", "", 0.0, 0.0),
            Spot("8", "地標H", "", 0.0, 0.0)
        )
    )
}