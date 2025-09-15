/*
    只更新使用者目前接取任務列表的腳本
*/

const { ObjectId } = require('mongodb');
const { getMongoClient } = require('../config/db');

// --- 請在此處設定 ---
// 要更新的用戶 ID
const userIdToUpdate = '68846d797609912e5e6ba9af'; 
// --------------------

// 資料庫名稱
const dbName = 'Hunter';
// 集合名稱
const userCollectionName = 'users';
const eventCollectionName = 'events';

async function updateUserQuests() {
  let client;

  try {
    // 取得 MongoDB 客戶端
    client = getMongoClient();
    await client.connect();
    console.log('成功連接到 MongoDB');

    // 選擇資料庫和集合
    const db = client.db(dbName);
    const userCollection = db.collection(userCollectionName);
    const eventCollection = db.collection(eventCollectionName);

    // 取得所有 events
    const events = await eventCollection.find({}, { projection: { _id: 1, spotId: 1 } }).toArray();

    // 依 userModel.js 的 missions schema 格式建立 mission 物件
    const newMissions = events.map(event => ({
      taskId: event._id,
      state: 'available',
      acceptedAt: null,
      expiresAt: null,
      refreshedAt: null,
      haveCheckPlaces: event.spotId ? [{
        spotId: event.spotId,
        isCheck: false
      }] : [],
      isLLM: false
    }));

    // 將用戶 ID 字串轉換為 ObjectId
    const userObjectId = new ObjectId(userIdToUpdate);

    // 更新指定用戶的當前任務列表
    const result = await userCollection.updateOne(
      { _id: userObjectId },
      { $set: { missions: newMissions } }
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
