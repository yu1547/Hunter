/*
    更新用戶道具列表的腳本
*/

const { ObjectId } = require('mongodb');
const { getMongoClient } = require('../config/db');

// --- 請在此處設定 ---
// 要更新的用戶 ID (請替換為實際的用戶 _id 字串)
const userIdToUpdate = '68846d797609912e5e6ba9af'; 

// 新的道具列表將從資料庫動態獲取
// --------------------

// 資料庫名稱
const dbName = 'Hunter';
// 集合名稱
const usersCollectionName = 'users';
const itemsCollectionName = 'items';

async function updateUserItems() {
  let client;

  try {
    // 取得 MongoDB 客戶端
    client = getMongoClient();
    await client.connect();
    console.log('成功連接到 MongoDB');

    // 選擇資料庫和集合
    const db = client.db(dbName);
    const usersCollection = db.collection(usersCollectionName);
    const itemsCollection = db.collection(itemsCollectionName);

    // 1. 從 'items' 集合中獲取所有道具
    console.log('正在從 items 集合中獲取所有道具...');
    const allItems = await itemsCollection.find({}).toArray();
    if (!allItems || allItems.length === 0) {
      console.log('在 items 集合中找不到任何道具，無法更新用戶背包。');
      return;
    }
    console.log(`找到了 ${allItems.length} 個道具。`);

    // 2. 為每個道具創建新的背包物品列表，並設定合理的數量
    const newBackpackItems = allItems.map(item => ({
      itemId: item._id,
      quantity: 20 // 為每個道具設定合理的數量，例如 20
    }));

    // 將用戶 ID 字串轉換為 ObjectId
    const userObjectId = new ObjectId(userIdToUpdate);

    // 更新指定用戶的道具列表
    const result = await usersCollection.updateOne(
      { _id: userObjectId },
      { $set: { backpackItems: newBackpackItems } }
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
