package com.ntou01157.hunter.models

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