const { ObjectId } = require('mongodb');
const fs = require('fs');
const path = require('path');
const { getMongoClient } = require('../config/db');

// 資料庫名稱
const dbName = 'Hunter';
// 集合名稱
const collectionName = 'items';
// JSON 資料檔案路徑
const dataFilePath = path.join(__dirname, '../data/sampleItems.json');

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
    let items = JSON.parse(rawData);
    
    // 轉換 MongoDB 的 ObjectId 字串為實際的 ObjectId 物件
    items = items.map(item => {
      // 轉換 itemId
      if (item.itemId && item.itemId.$oid) {
        item.itemId = new ObjectId(item.itemId.$oid);
      }
      
      // 轉換 resultId (如果存在且不為 null)
      if (item.resultId && item.resultId.$oid) {
        item.resultId = new ObjectId(item.resultId.$oid);
      }
      
      return item;
    });

    // 清空集合前先檢查現有文件數量
    const existingCount = await collection.countDocuments({});
    console.log(`集合中現有 ${existingCount} 筆資料`);

    // 清空集合
    const deleteResult = await collection.deleteMany({});
    console.log(`已刪除 ${deleteResult.deletedCount} 筆舊資料`);

    // 插入資料
    const result = await collection.insertMany(items);
    console.log(`成功導入 ${result.insertedCount} 筆資料到 items 集合`);
  } catch (error) {
    console.error('導入資料過程中發生錯誤:', error);
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
