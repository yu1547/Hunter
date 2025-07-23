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
                    rank = userObj.getInt("rank"),
                    userId = userObj.getString("userId"),
                    username = userObj.getString("username"),
                    score = userObj.getInt("score")
                )
            )
        }

        val userRankObj = jsonObject.getJSONObject("userRank")
        val userRank = UserRanking(
            rank = userRankObj.getInt("rank"),
            userId = userRankObj.getString("userId"),
            username = userRankObj.getString("username"),
            score = userRankObj.getInt("score")
        )

        return RankResponse(rankList = rankList, userRank = userRank)
    }

}
