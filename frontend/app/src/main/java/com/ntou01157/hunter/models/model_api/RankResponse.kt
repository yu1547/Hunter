package com.ntou01157.hunter.models.model_api

import com.google.gson.annotations.SerializedName

data class RankCreateRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("userImg") val userImg: String = "",
    @SerializedName("score") val score: Int = 0 // 後端會預設 0，但保留給前端可控
)

data class RankItem(
    @SerializedName("userId") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("userImg") val userImg: String?,
    @SerializedName("score") val score: Int
)

data class UserRank(
    @SerializedName("rank") val rank: Int,
    @SerializedName("userId") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("userImg") val userImg: String?,
    @SerializedName("score") val score: Int
)

data class RankResponse(
    @SerializedName("rankList") val rankList: List<RankItem> = emptyList(),
    @SerializedName("userRank") val userRank: UserRank? = null
)
