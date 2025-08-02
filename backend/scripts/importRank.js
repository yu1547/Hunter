// backend/scripts/importRank.js

const fs = require('fs');
const path = require('path');
const { getMongoClient } = require('../config/db'); // 引入您的 MongoDB 連線客戶端函數

// 資料庫名稱
const dbName = 'Hunter';
// 集合名稱 (User 模型對應的集合通常是 'users')
const collectionName = 'ranks';
// JSON 資料檔案路徑 (假設 sampleRanks.json 放在 data 目錄下)
const dataFilePath = path.join(__dirname, '../data/sampleRanks.json'); // 確保路徑正確

async function importRankData() {
  let client;

  try {
    client = getMongoClient();
    await client.connect();
    console.log('成功連線到 MongoDB');

    const db = client.db(dbName);
    const collection = db.collection(collectionName);

    const rawData = fs.readFileSync(dataFilePath, 'utf-8');
    const fullRankData = JSON.parse(rawData); // 這是從 sampleRanks.json 讀取到的用戶數據

    // 提取 rankList 和 userRank
    const rankListFromData = fullRankData.rankList;
    const userRankFromData = fullRankData.userRank;

    // 將 rankList 和 userRank 合併為一個要處理的用戶列表
    const usersToProcess = [];
    if (Array.isArray(rankListFromData)) {
      usersToProcess.push(...rankListFromData);
    } else {
      console.warn(`警告: ${dataFilePath} 中的 'rankList' 數據不是一個陣列，將跳過。`);
    }

    // 如果 userRank 存在且是一個物件，則添加它
    // 注意: userRank 中的 'rank' 欄位不會被存儲到 User 模型中，因為它不是用戶的基本屬性，
    // 而是與排行榜相關的動態資訊。通常這些資訊在資料庫中獨立管理或在查詢時生成。
    if (userRankFromData && typeof userRankFromData === 'object') {
      usersToProcess.push(userRankFromData);
    }

    console.log(`開始處理來自 ${dataFilePath} 的 ${usersToProcess.length} 筆排行榜數據...`);

    let updatedCount = 0;
    let insertedCount = 0; // 重新啟用插入計數
    let skippedCount = 0;
    // notFoundCount 在 upsert: true 的情況下不再適用，因為未找到會導致插入

    for (const rankUser of usersToProcess) {
      const identifier = rankUser.username || rankUser.userId;

      if (!identifier || rankUser.score === undefined) {
        console.warn(`跳過無效的排行榜條目 (缺少 username/userId 或 score):`, rankUser);
        skippedCount++;
        continue;
      }

      const userDataToSet = {
        userId:rankUser.userId,
        username: rankUser.username || rankUser.userId || `玩家_${identifier}`,
        userImg: rankUser.userImg || null,
        score: rankUser.score,
      };

      const filter = { username: identifier };
      const update = { $set: userDataToSet };
      const options = { upsert: true }; // 保持 upsert: true

      const result = await collection.updateOne(filter, update, options);

      if (result.upsertedCount > 0) {
        insertedCount++;
        console.log(`新增用戶 (由排行榜數據引入): ${identifier} (分數: ${rankUser.score})`);
      } else if (result.modifiedCount > 0) {
        updatedCount++;
        console.log(`更新用戶分數: ${identifier} (分數: ${rankUser.score})`);
      } else {
        // 如果 matchedCount > 0 且 modifiedCount == 0，表示數據沒有變化
        console.log(`用戶分數未更改 (已存在且數據相同): ${identifier} (分數: ${rankUser.score})`);
      }
    }

    console.log(`\n數據處理完成。`);
    console.log(`成功更新 ${updatedCount} 筆現有用戶數據。`);
    console.log(`成功插入 ${insertedCount} 筆新用戶數據 (由排行榜數據引入)。`); // 修正日誌訊息
    if (skippedCount > 0) {
        console.warn(`跳過 ${skippedCount} 筆無效或缺少識別符的數據。`);
    }

  } catch (error) {
    console.error('導入排行榜數據時出錯:', error);
    console.error('錯誤詳情:', error.message);
    console.error('堆棧追蹤:', error.stack);
    process.exit(1);
  } finally {
    if (client) {
      await client.close();
      console.log('已關閉 MongoDB 連線');
    }
  }
}

// 執行匯入功能
importRankData();