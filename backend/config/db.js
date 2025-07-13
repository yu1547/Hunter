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
    } catch (error) {
    console.error('MongoDB 連接失敗:', error.message);
    process.exit(1);
    }
};

// // 舊的 MongoDB 客戶端連接（保留以向後兼容）
// const client = new MongoClient(uri, {
//     serverApi: {
//         version: ServerApiVersion.v1,
//         strict: true,
//         deprecationErrors: true,
//     }
// });

// async function run() {
//     try {
//         await client.connect();
//         await client.db("admin").command({ ping: 1 });
//         console.log("Pinged your deployment. You successfully connected to MongoDB!");
//     } finally {
//         await client.close();
//     }
// }

module.exports = { connectDB };
