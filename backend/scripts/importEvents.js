// 引入 dotenv，讓腳本可以讀取 .env 檔案
require('dotenv').config();
const mongoose = require('mongoose');
const fs = require('fs');
const path = require('path');
const Event = require('../models/eventModel');


// 確保資料庫 URL 存在於 .env 檔案中
const mongoURI = process.env.MONGODB_URI;
if (!mongoURI) {
  console.error('錯誤：請在 .env 檔案中設定 MONGODB_URI');
  process.exit(1);
}

const importData = async () => {
  try {
    // 建立與 MongoDB 的連線
    await mongoose.connect(mongoURI);
    console.log('MongoDB 連線成功！');

    // 讀取 JSON 檔案
    const eventsFilePath = path.resolve(__dirname, '../data/sampleEvents.json');
    const events = JSON.parse(fs.readFileSync(eventsFilePath, 'utf-8'));
    console.log(`已讀取 ${events.length} 個事件資料。`);

    // 清空現有資料
    await Event.deleteMany({});
    console.log('現有事件資料已清空！');

    // 匯入新資料
    await Event.insertMany(events);
    console.log('Events data successfully imported!');

    // 匯入成功後斷開連線
    await mongoose.disconnect();
    console.log('MongoDB 連線已關閉。');

    process.exit();
  } catch (err) {
    // 處理各種可能的錯誤，並提供更詳細的訊息
    console.error('匯入資料時發生錯誤：');
    console.error(err);
    // 確保發生錯誤時也能斷開連線
    if (mongoose.connection.readyState === 1) {
      await mongoose.disconnect();
    }
    process.exit(1);
  }
};

importData();