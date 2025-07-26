package com.ntou01157.hunter.models

data class RankItem( // 這個應該是後端 rankList 裡的每個項目
    val userId: String,
    val username: String,
    val userImg: String?, // 注意：userImg 是可空的
    val score: Int
)

data class RankResponse(
    val rankList: List<UserRanking>,
    val userRank: UserRanking? // 允許 userRank 為 null
)

data class UserRanking(
    val rank: Int?=null,
    val userId: String,
    val username: String,
    val userImg: String?,
    val score: Int
)
