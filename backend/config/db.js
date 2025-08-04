const mongoose = require('mongoose');
const { MongoClient, ServerApiVersion } = require('mongodb');
require('dotenv').config();

const uri = process.env.MONGODB_URI;

// MongoDB 連接（使用 mongoose）
const connectDB = async () => {
    try {
    await mongoose.connect(uri, {
        serverApi: {
        version: ServerApiVersion.v1,
        strict: true,
        deprecationErrors: true,
        }
    });
    console.log('MongoDB 連接成功！');
    console.log('目前連線的資料庫名稱：', mongoose.connection.name); 
    } catch (error) {
    console.error('MongoDB 連接失敗:', error.message);
    process.exit(1);
    }
};

// 獲取 MongoDB 客戶端
const getMongoClient = () => {
    return new MongoClient(uri, {
        serverApi: {
            version: ServerApiVersion.v1,
            strict: true,
            deprecationErrors: true,
        }
    });
};

module.exports = { connectDB, getMongoClient };
