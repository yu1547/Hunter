/*
  這程式碼是用來更新物品的 resultId，將原料物品的 resultId 設置為產物物品的 _id，
  要加任何配方可以在craftingRecipes 物件中添加新的鍵值對。
*/

const { getMongoClient } = require('../config/db');
const dbName = 'Hunter';
const collectionName = 'items';

// 定義合成配方：{ "原料物品名稱": "產物物品名稱" }
// 腳本會查找原料物品，並將其 resultId 設置為產物物品的 _id
const craftingRecipes = {
  "銅鑰匙碎片": "銅鑰匙",
  "銀鑰匙碎片": "銀鑰匙",
  "金鑰匙碎片": "金鑰匙",
  "普通的史萊姆黏液": "小史萊姆",
  "黏稠的史萊姆黏液": "大史萊姆",
  "散落的地圖殘片": "寶藏圖",
  // 加更多配方
};

async function updateResultIds() {
  let client;

  try {
    // 取得 MongoDB 客戶端並連接
    client = getMongoClient();
    await client.connect();
    console.log('成功連接到 MongoDB');

    const db = client.db(dbName);
    const collection = db.collection(collectionName);

    // 遍歷所有合成配方
    for (const sourceName in craftingRecipes) {
      const resultName = craftingRecipes[sourceName];

      // 根據名稱查找原料物品和產物物品
      const sourceItem = await collection.findOne({ itemName: sourceName });
      const resultItem = await collection.findOne({ itemName: resultName });

      if (sourceItem && resultItem) {
        // 如果兩個物品都存在，則更新原料物品的 resultId
        const updateResult = await collection.updateOne(
          { _id: sourceItem._id },
          { $set: { resultId: resultItem._id } }
        );

        if (updateResult.modifiedCount > 0) {
          console.log(`成功更新 '${sourceName}' 的 resultId -> '${resultName}'`);
        } else if (updateResult.matchedCount > 0) {
          console.log(`'${sourceName}' 的 resultId 已是最新，無需更新。`);
        } else {
          // This case should ideally not be hit if sourceItem was found
          console.log(`找不到符合 '${sourceName}' 的項目進行更新。`);
        }
      } else {
        // 如果找不到任何一個物品，則顯示警告
        if (!sourceItem) {
          console.warn(`警告：在資料庫中找不到原料物品 '${sourceName}'`);
        }
        if (!resultItem) {
          console.warn(`警告：在資料庫中找不到產物物品 '${resultName}'`);
        }
      }
    }
  } catch (error) {
    console.error('更新 resultId 過程中發生錯誤:', error);
  } finally {
    // 確保關閉連接
    if (client) {
      await client.close();
      console.log('已關閉 MongoDB 連接');
    }
  }
}

// 執行更新腳本
updateResultIds();
