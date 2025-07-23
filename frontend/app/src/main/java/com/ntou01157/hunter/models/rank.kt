package com.ntou01157.hunter.models

data class RankResponse(
    val rankList: List<UserRankEntry>,
    val userRank: UserRankEntry
)

data class UserRankEntry(
    val rank: Int = 0,
    val userId: String = "",
    val username: String = "",
    val score: Int = 0
)