/*
    更新用戶道具列表的腳本
*/

const { ObjectId } = require('mongodb');
const { getMongoClient } = require('../config/db');

// --- 請在此處設定 ---
// 要更新的用戶 ID (請替換為實際的用戶 _id 字串)
const userIdToUpdate = '68846d797609912e5e6ba9af'; 

// 新的道具列表 (請根據您的 item schema 修改)
// 這裡假設 item 的 ID 也是 ObjectId
const newItems = [
  // { "itemId": new ObjectId("6880f3f7d80b975b33f23e2e"), "quantity": 10 },
  // { "itemId": new ObjectId("6880f3f7d80b975b33f23e2f"), "quantity": 5 },
];
// --------------------

// 資料庫名稱
const dbName = 'Hunter';
// 集合名稱
const collectionName = 'users';

async function updateUserItems() {
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

    // 更新指定用戶的道具列表
    const result = await collection.updateOne(
      { _id: userObjectId },
      { $set: { backpackItems: newItems } }
    );

    if (result.matchedCount === 0) {
      console.log(`找不到 ID 為 ${userIdToUpdate} 的用戶`);
    } else if (result.modifiedCount === 0) {
      console.log(`用戶 ${userIdToUpdate} 的道具列表未被修改(可能內容相同)`);
    } else {
      console.log(`成功更新用戶 ${userIdToUpdate} 的道具列表`);
    }

  } catch (error) {
    console.error('更新用戶道具時出錯:', error);
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
updateUserItems();
