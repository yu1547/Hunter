package com.ntou01157.hunter.models

data class Spot(
    val spotId: String,        // 從 MongoDB 的 "_id" 欄位轉過來
    val spotName: String,      // 英文代碼（用來對應圖片、比對解鎖紀錄）
    val ChName: String,        // ✅ 中文名稱（顯示在 UI）
    val latitude: Double,
    val longitude: Double
)