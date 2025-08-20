// backend/controllers/rankController.js
const Rank = require('../models/rankModel');
const User = require('../models/userModel'); // 從 users 撈 username / photoURL

// 將 Rank Doc 轉成前端要的格式
const toDisplay = (doc) => ({
  userId: doc.userId,
  username: doc.username || '未知用戶',
  userImg: doc.userImg || '',
  score: typeof doc.score === 'number' ? doc.score : 0,
});

// 若沒有 rank 就用 users 的資料自動建一筆（score=0）
async function ensureRankDoc(userId) {
  if (!userId) return null;

  // 1) 先找 ranks
  let rank = await Rank.findOne({ userId }).lean();
  if (rank) return rank;

  // 2) 沒有就到 users 撈資料
  const user = await User.findById(userId).lean();

  const username =
    (user && (user.username || user.displayName)) || '玩家';
  const userImg = (user && user.photoURL) || '';

  // 3) 建立一筆 score=0
  const created = await Rank.create({
    userId,
    username,
    userImg,
    score: 0,
  });

  // 回傳 POJO（與上面 lean() 一致）
  return {
    userId: created.userId,
    username: created.username,
    userImg: created.userImg,
    score: created.score,
  };
}

const getRank = async (req, res) => {
  try {
    const requestedUserId = req.params.userId;

    // 只取分數 > 0 的前 100 名
    const topRankItems = await Rank.find({ score: { $gt: 0 } })
      .select('userId username userImg score')
      .sort({ score: -1 })
      .limit(100)
      .lean();

    const rankList = topRankItems.map(toDisplay);

    let userRank = null;

    if (requestedUserId) {
      // 確保該使用者在 ranks 有一筆（沒有就新建 score=0）
      const ensured = await ensureRankDoc(requestedUserId);

      // 再拿到目前使用者的 rank 資料
      const curr = await Rank.findOne({ userId: requestedUserId })
        .select('userId username userImg score')
        .lean();

      if (curr) {
        // 分數為 0 → 不上榜，rank = null
        if ((curr.score ?? 0) === 0) {
          userRank = {
            rank: null,
            ...toDisplay(curr),
          };
        } else {
          // 分數 > 0 → 計算實際排名（只在 score>0 的集合中）
          const allSorted = await Rank.find({ score: { $gt: 0 } })
            .select('userId score')
            .sort({ score: -1 })
            .lean();
          const idx = allSorted.findIndex((x) => x.userId === requestedUserId);

          userRank = {
            rank: idx !== -1 ? idx + 1 : null,
            ...toDisplay(curr),
          };
        }
      } else if (ensured) {
        // 理論上不會進來；保底處理
        userRank = { rank: null, ...toDisplay(ensured) };
      }
    }

    res.status(200).json({ rankList, userRank });
  } catch (error) {
    console.error('獲取排行榜數據時出錯:', error);
    res.status(500).json({ message: 'Server Error', details: error.message });
  }
};

module.exports = { getRank };
