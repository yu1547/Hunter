package com.ntou01157.hunter.temp

import com.ntou01157.hunter.models.*
import org.json.JSONObject

object RankingRepository {

    fun parseRankingJson(jsonString: String): RankResponse {
        val jsonObject = JSONObject(jsonString)

        val rankListJsonArray = jsonObject.getJSONArray("rankList")
        val rankList = mutableListOf<UserRanking>()

        for (i in 0 until rankListJsonArray.length()) {
            val userObj = rankListJsonArray.getJSONObject(i)
            rankList.add(
                UserRanking(
                    rank = i+1,
                    userId = userObj.getString("userId"),
                    username = userObj.getString("username"),
                    userImg = userObj.getString("userImg"),
                    score = userObj.getInt("score")
                )
            )
        }

        val userRankObj = jsonObject.getJSONObject("userRank")
        val userRank = UserRanking(
            rank = userRankObj.getInt("rank"),
            userId = userRankObj.getString("userId"),
            username = userRankObj.getString("username"),
            userImg = userRankObj.getString("userImg"),
            score = userRankObj.getInt("score")
        )

        return RankResponse(rankList = rankList, userRank = userRank)
    }

}

