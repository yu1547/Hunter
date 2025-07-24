/*
    根據道具名稱和任務名稱，更新特定任務的獎勵道具。
*/
const { ObjectId } = require('mongodb');
const { getMongoClient } = require('../config/db');

// --- 請在此處設定 ---
// 要更新的任務名稱
const taskNameToUpdate = '新手尋寶教學';
// 新的獎勵道具名稱
const newItemName = '銅鑰匙碎片';
// 新的獎勵道具數量
const newItemQuantity = 6;
// --------------------

const dbName = 'Hunter';
const tasksCollectionName = 'tasks';
const itemsCollectionName = 'items';

async function updateTaskReward() {
  let client;

  try {
    // 取得 MongoDB 客戶端並連接
    client = getMongoClient();
    await client.connect();
    console.log('成功連接到 MongoDB');

    const db = client.db(dbName);
    const tasksCollection = db.collection(tasksCollectionName);
    const itemsCollection = db.collection(itemsCollectionName);

    // 1. 根據道具名稱查找道具 ID
    const rewardItem = await itemsCollection.findOne({ itemName: newItemName });

    if (!rewardItem) {
      console.error(`錯誤：在 '${itemsCollectionName}' 集合中找不到名為 "${newItemName}" 的道具。`);
      process.exit(1);
    }
    const rewardItemId = rewardItem._id;
    console.log(`找到道具 "${newItemName}"，ID 為: ${rewardItemId}`);

    // 2. 更新指定任務的獎勵列表
    const newReward = {
      itemId: rewardItemId,
      quantity: newItemQuantity
    };

    const result = await tasksCollection.updateOne(
      { taskName: taskNameToUpdate },
      { $set: { rewardItems: [newReward] } }
    );

    if (result.matchedCount === 0) {
      console.log(`找不到名為 "${taskNameToUpdate}" 的任務`);
    } else if (result.modifiedCount === 0) {
      console.log(`任務 "${taskNameToUpdate}" 的獎勵未被修改(可能內容相同)`);
    } else {
      console.log(`成功更新任務 "${taskNameToUpdate}" 的獎勵`);
      console.log('新獎勵:', newReward);
    }

  } catch (error) {
    console.error('更新任務獎勵時出錯:', error);
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
updateTaskReward();
