const mongoose = require('mongoose');
const { MongoClient, ServerApiVersion } = require('mongodb');

// 更新連接字串，指定使用 'items' 數據庫
const uri = "mongodb+srv://root:0000@cluster0.ttaaau1.mongodb.net/items?retryWrites=true&w=majority&appName=Cluster0";

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

// 舊的 MongoDB 客戶端連接（保留以向後兼容）
const client = new MongoClient(uri, {
  serverApi: {
    version: ServerApiVersion.v1,
    strict: true,
    deprecationErrors: true,
  }
});

async function run() {
  try {
    await client.connect();
    await client.db("admin").command({ ping: 1 });
    console.log("Pinged your deployment. You successfully connected to MongoDB!");
  } finally {
    await client.close();
  }
}

module.exports = { connectDB, client, run };
