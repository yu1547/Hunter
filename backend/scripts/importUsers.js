const { ObjectId } = require('mongodb');
const fs = require('fs');
const path = require('path');
const { getMongoClient } = require('../config/db');

// 資料庫名稱
const dbName = 'Hunter';
// 集合名稱
const collectionName = 'users';
// JSON 資料檔案路徑
const dataFilePath = path.join(__dirname, '../data/sampleUsers.json');

async function importData() {
  let client;

  try {
    // 使用 db.js 中的函數取得 MongoDB 客戶端
    client = getMongoClient();
    await client.connect();
    console.log('成功連接到 MongoDB');

    // 選擇資料庫和集合
    const db = client.db(dbName);
    const collection = db.collection(collectionName);
    
    // 讀取 JSON 檔案
    const rawData = fs.readFileSync(dataFilePath);
    let users = JSON.parse(rawData);
    
    // 轉換 MongoDB 的 ObjectId 字串為實際的 ObjectId 物件 (如果需要)
    users = users.map(user => {
      // 如果 user 有包含 ObjectId 格式的字段，在這裡處理
      if (user._id && user._id.$oid) {
        user._id = new ObjectId(user._id.$oid);
      }
      return user;
    });

    // 清空集合
    await collection.deleteMany({});
    console.log('已刪除所有現有用戶數據');

    // 插入資料
    const result = await collection.insertMany(users);
    console.log(`成功導入 ${result.insertedCount} 筆用戶數據`);
  } catch (error) {
    console.error('導入用戶數據時出錯:', error);
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
importData();
