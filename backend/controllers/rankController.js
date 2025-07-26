// backend/controllers/rankController.js

const User = require('../models/userModel');
const mongoose = require('mongoose');

const convertUserToRankDisplayItem = (user) => {
  if (!user) return null;
  return {
    // 現在確認使用 user.uid 作為前端的 userId
    userId: user.uid, // <-- 關鍵修正：確保這裡使用 uid
    username: user.userName || "未知用戶", // 數據庫中是 userName
    userImg: user.userImg || "",
    score: user.score || 0,
  };
};

const getRank = async (req, res) => {
  try {
    const usersInRankCursor = await User.find({
        // 確保 userName 存在且非空，score 存在
        userName: { $exists: true, $ne: null, $ne: "" }, // 數據庫中是 userName
        score: { $exists: true }
      })
      .select('uid userName userImg score') // <-- 關鍵修正：確保選取 uid 和 userName
      .sort({ score: -1 })
      .limit(100)
      .lean();

    const rankList = usersInRankCursor.map(user => convertUserToRankDisplayItem(user));

    let userRank = null;
    // 假設 req.user.uid 包含了登錄用戶的 uid
    if (req.user && req.user.uid) { // <-- 關鍵檢查：確認您的身份驗證中間件設置 req.user.uid
      const currentUid = req.user.uid;

      const allUsersSortedByScore = await User.find({ score: { $exists: true } })
        .select('uid score') // <-- 關鍵修正：選取 uid 和 score
        .sort({ score: -1 })
        .lean();

      const userIndex = allUsersSortedByScore.findIndex(user => user.uid === currentUid); // 直接比較字符串 uid

      if (userIndex !== -1) {
        const currentUserDoc = await User.findOne({ uid: currentUid }) // <-- 根據 uid 查找
          .select('uid userName userImg score') // <-- 關鍵修正：選擇正確的字段
          .lean();

        if (currentUserDoc) {
          userRank = {
            rank: userIndex + 1,
            ...convertUserToRankDisplayItem(currentUserDoc)
          };
        }
      }
    }

    res.status(200).json({ rankList, userRank });

  } catch (error) {
    console.error('獲取排行榜數據時出錯:', error);
    res.status(500).json({ message: 'Server Error', details: error.message });
  }
};

module.exports = { getRank };