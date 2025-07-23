/*
    只更新使用者目前接取任務列表的腳本
*/

const { ObjectId } = require('mongodb');
const { getMongoClient } = require('../config/db');

// --- 請在此處設定 ---
// 要更新的用戶 ID (請替換為實際的用戶 _id 字串)
const userIdToUpdate = '6879fdbc125a5443a1d4bade'; 

// 新的任務 ID 列表 (請根據您的 quest schema 修改)
// 這裡假設 quest 的 ID 也是 ObjectId
const newQuestIds = [
  new ObjectId("688085695aa0d66ff4d2077b"), // 請替換為實際的任務 ID
  new ObjectId("688085695aa0d66ff4d2077c"), // 請替換為實際的任務 ID
];
// --------------------

// 資料庫名稱
const dbName = 'Hunter';
// 集合名稱
const collectionName = 'users';

async function updateUserQuests() {
  let client;

  try {
    // 取得 MongoDB 客戶端
    client = getMongoClient();
    await client.connect();
    console.log('成功連接到 MongoDB');

    // 選擇資料庫和集合
    const db = client.db(dbName);
    const collection = db.collection(collectionName);

    // 將用戶 ID 字串轉換為 ObjectId
    const userObjectId = new ObjectId(userIdToUpdate);

    // 更新指定用戶的當前任務列表
    const result = await collection.updateOne(
      { _id: userObjectId },
      { $set: { currentQuests: newQuestIds } }
    );

    if (result.matchedCount === 0) {
      console.log(`找不到 ID 為 ${userIdToUpdate} 的用戶`);
    } else if (result.modifiedCount === 0) {
      console.log(`用戶 ${userIdToUpdate} 的任務列表未被修改(可能內容相同)`);
    } else {
      console.log(`成功更新用戶 ${userIdToUpdate} 的任務列表`);
    }

  } catch (error) {
    console.error('更新用戶任務時出錯:', error);
    process.exit(1);
  } finally {
    // 關閉連接
    if (client) {
      await client.close();
      console.log('已關閉 MongoDB 連接');
    }
  }
}

// 執行更新功能
updateUserQuests();
