const { connectDB } = require('../config/db');
const User = require('../models/userModel');
const users = require('../data/sampleUsers.json');
const mongoose = require('mongoose');

// 連接到資料庫
connectDB();

// 導入用戶數據
const importData = async () => {
  try {
    // 清空現有用戶數據
    await User.deleteMany({});
    console.log('已刪除所有現有用戶數據');

    // 導入示例用戶數據
    await User.insertMany(users);
    console.log('成功導入用戶數據');

    // 關閉數據庫連接
    mongoose.connection.close();
    process.exit(0);
  } catch (error) {
    console.error('導入用戶數據時出錯:', error);
    process.exit(1);
  }
};

importData();
