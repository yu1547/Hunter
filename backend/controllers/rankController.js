// backend/controllers/rankController.js

const Rank = require('../models/rankModel');
const mongoose = require('mongoose');

// 輔助函數：將資料庫物件轉換為前端所需的顯示格式
const convertRankItemToDisplay = (rankItem) => {
  if (!rankItem) return null;
  return {
    // 修正：將 uid 改為 userId，與 rankModel.js 一致
    userId: rankItem.userId,
    username: rankItem.username || "未知用戶",
    userImg: rankItem.userImg || "",
    score: rankItem.score || 0,
  };
};

const getRank = async (req, res) => {
  try {
    // 1. 獲取前 100 名的排行榜資料
    const topRankItems = await Rank.find({})
      // 修正：select 中使用 userId
      .select('userId username userImg score')
      .sort({ score: -1 }) // 降序排序
      .limit(100) // 限制前 100 名
      .lean(); // 使用 lean() 提高查詢效率，返回 POJO

    // 將查詢到的資料轉換為前端顯示格式
    const rankList = topRankItems.map(convertRankItemToDisplay);

    let userRank = null;
    // 檢查是否有登入使用者以及其 ID
    // 假設 req.user.userId 存放當前登入使用者的 ID
    if (req.user && req.user.userId) {
      const currentUserId = req.user.userId;

      // 嘗試從已經獲取的前 100 名中找到當前使用者，避免重複查詢
      const foundInTopRank = rankList.find(item => item.userId === currentUserId);

      if (foundInTopRank) {
        // 如果使用者在前 100 名內，直接從 rankList 中獲取資訊並計算排名
        // 注意：這裡的排名是基於 topRankItems 陣列中的索引
        const userIndexInTop = topRankItems.findIndex(item => item.userId === currentUserId);
        userRank = {
          rank: userIndexInTop !== -1 ? userIndexInTop + 1 : -1, // 如果找到就加1，否則為-1 (理論上會找到)
          ...foundInTopRank
        };
      } else {
        // 如果使用者不在前 100 名，則需要單獨查詢其排名和資料
        // 首先，獲取所有使用者的分數，用來計算精確排名
        const allRankItemsSortedByScore = await Rank.find({})
          // 修正：select 中使用 userId
          .select('userId score')
          .sort({ score: -1 })
          .lean();

        // 找到當前使用者的索引，即為其排名 (索引 + 1)
        // 修正：findIndex 中使用 item.userId
        const userIndex = allRankItemsSortedByScore.findIndex(item => item.userId === currentUserId);

        if (userIndex !== -1) {
          // 查找到當前使用者的完整資料
          const currentUserRankDoc = await Rank.findOne({ userId: currentUserId })
            // 修正：select 中使用 userId
            .select('userId username userImg score')
            .lean();

          if (currentUserRankDoc) {
            userRank = {
              rank: userIndex + 1, // 排名是索引加 1
              ...convertRankItemToDisplay(currentUserRankDoc)
            };
          }
        }
      }
    }

    res.status(200).json({ rankList, userRank });

  } catch (error) {
    console.error('獲取排行榜數據時出錯:', error);
    // 提供更詳細的錯誤訊息，幫助前端調試
    res.status(500).json({ message: 'Server Error', details: error.message, stack: error.stack });
  }
};

module.exports = { getRank };