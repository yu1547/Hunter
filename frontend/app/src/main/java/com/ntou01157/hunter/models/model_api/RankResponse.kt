// frontend/app/src/main/java/com/ntou01157/hunter/models/model_api/RankResponse.kt

package com.ntou01157.hunter.models.model_api

import com.google.gson.annotations.SerializedName

data class RankResponse(
    @SerializedName("rankList")
    val rankList: List<RankItem>,
    @SerializedName("userRank")
    val userRank: UserRank?
)

data class RankItem(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("userImg")
    val userImg: String?, // 可為空
    @SerializedName("score")
    val score: Int
)

data class UserRank(
    @SerializedName("rank")
    val rank: Int,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("userImg")
    val userImg: String?, // 可為空
    @SerializedName("score")
    val score: Int
)