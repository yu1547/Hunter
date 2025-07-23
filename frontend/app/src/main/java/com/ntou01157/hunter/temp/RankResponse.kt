package com.ntou01157.hunter.temp

import com.ntou01157.hunter.model.RankResponse
import com.ntou01157.hunter.model.UserRankEntry
import org.json.JSONObject

object RankingRepository {

    fun parseRankingJson(jsonString: String): RankResponse {
        val json = JSONObject(jsonString)

        val rankListJson = json.getJSONArray("rankList")
        val rankList = mutableListOf<UserRankEntry>()
        for (i in 0 until rankListJson.length()) {
            val item = rankListJson.getJSONObject(i)
            rankList.add(
                UserRankEntry(
                    rank = item.getInt("rank"),
                    userId = item.getString("userId"),
                    username = item.getString("username"),
                    score = item.getInt("score")
                )
            )
        }

        val userRankJson = json.getJSONObject("userRank")
        val userRank = UserRankEntry(
            rank = userRankJson.getInt("rank"),
            userId = userRankJson.getString("userId"),
            username = userRankJson.getString("username"),
            score = userRankJson.getInt("score")
        )

        return RankResponse(rankList = rankList, userRank = userRank)
    }
}
