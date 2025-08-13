const { ObjectId } = require('mongodb');
const fs = require('fs');
const path = require('path');
const { getMongoClient } = require('../config/db');

// 資料庫名稱與集合
const dbName = 'Hunter';
const collectionName = 'users';
// JSON 檔案路徑
const settingsFilePath = path.join(__dirname, '../data/sampleSettings.json');

async function importSettings() {
  let client;

  try {
    client = getMongoClient();
    await client.connect();
    console.log('成功連接到 MongoDB');

    const db = client.db(dbName);
    const collection = db.collection(collectionName);

    // 讀取設定檔資料
    const rawData = fs.readFileSync(settingsFilePath);
    const settingsData = JSON.parse(rawData);

    let updatedCount = 0;

    for (const setting of settingsData) {
      const { userId, settings } = setting;

      // 確保 userId 是 ObjectId 型別
      const result = await collection.updateOne(
        { _id: new ObjectId(userId) },
        { $set: { settings: settings } }
      );

      if (result.modifiedCount > 0) {
        updatedCount++;
        console.log(`已更新使用者 ${userId} 的 settings`);
      }
    }

    console.log(`共更新 ${updatedCount} 位使用者的 settings`);

  } catch (err) {
    console.error('匯入 settings 發生錯誤：', err);
    process.exit(1);
  } finally {
    if (client) {
      await client.close();
      console.log('已關閉 MongoDB 連線');
    }
  }
}

importSettings();
