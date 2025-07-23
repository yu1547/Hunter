package com.ntou01157.hunter.models

data class RankResponse(
    val rankList: List<UserRanking>,
    val userRank: UserRanking
)

data class UserRanking(
    val rank: Int,
    val userId: String,
    val username: String,
    val userImg: String,
    val score: Int
)