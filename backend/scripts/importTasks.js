const { ObjectId } = require('mongodb');
const fs = require('fs');
const path = require('path');
const { getMongoClient } = require('../config/db');

// 資料庫名稱
const dbName = 'Hunter';
// 集合名稱
const collectionName = 'tasks';
// JSON 資料檔案路徑
const dataFilePath = path.join(__dirname, '../data/sampleTasks.json');

async function importTasks() {
  let client;

  try {
    // 取得 MongoDB 客戶端
    client = getMongoClient();
    await client.connect();
    console.log('成功連接到 MongoDB');

    // 選擇資料庫和集合
    const db = client.db(dbName);
    const collection = db.collection(collectionName);
    
    // 讀取 JSON 檔案
    const rawData = fs.readFileSync(dataFilePath);
    let tasks = JSON.parse(rawData);
    
    // 將字串 ID 轉換為 ObjectId
    tasks = tasks.map(task => {
      if (task.rewardItems) {
        task.rewardItems = task.rewardItems.map(item => {
          if (item.itemId && typeof item.itemId === 'string') {
            item.itemId = new ObjectId(item.itemId);
          }
          return item;
        });
      }
      if (task.checkPlace) {
        task.checkPlace = task.checkPlace.map(place => {
          if (place.spotId && typeof place.spotId === 'string') {
            place.spotId = new ObjectId(place.spotId);
          }
          return place;
        });
      }
      return task;
    });

    // 清空集合
    await collection.deleteMany({});
    console.log('已刪除所有現有任務數據');

    // 插入資料
    const result = await collection.insertMany(tasks);
    console.log(`成功導入 ${result.insertedCount} 筆任務數據`);
  } catch (error) {
    console.error('導入任務數據時出錯:', error);
    process.exit(1);
  } finally {
    // 關閉連接
    if (client) {
      await client.close();
      console.log('已關閉 MongoDB 連接');
    }
  }
}

// 執行匯入功能
importTasks();
