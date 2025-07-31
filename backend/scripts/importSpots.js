const fs = require('fs');
const path = require('path');
const { getMongoClient } = require('../config/db');

// 資料庫名稱
const dbName = 'Hunter';
// 集合名稱
const collectionName = 'spots';
// JSON 資料檔案路徑
const dataFilePath = path.join(__dirname, '../data/sampleSpots.json');

async function importSpots() {
  let client;

  try {
    client = getMongoClient();
    await client.connect();
    console.log('成功連接到 MongoDB');

    const db = client.db(dbName);
    const collection = db.collection(collectionName);

    // 讀取 JSON 檔案
    const rawData = fs.readFileSync(dataFilePath);
    const spots = JSON.parse(rawData);

    // 清空集合
    await collection.deleteMany({});
    console.log('已刪除所有現有打卡點資料');

    // 插入資料
    const result = await collection.insertMany(spots);
    console.log(`成功導入 ${result.insertedCount} 筆打卡點資料`);
  } catch (error) {
    console.error('導入打卡點資料時出錯:', error);
    process.exit(1);
  } finally {
    if (client) {
      await client.close();
      console.log('已關閉 MongoDB 連接');
    }
  }
}

// 執行匯入功能
importSpots();
