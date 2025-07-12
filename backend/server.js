const express = require('express');
const cors = require('cors');
const { connectDB, client } = require('./db');
const itemRoutes = require('./routes/itemRoutes');
const app = express();
const PORT = process.env.PORT || 3000;
const mongoose = require('mongoose');

// 連接到資料庫
connectDB();

// 添加數據庫診斷代碼
mongoose.connection.once('open', async () => {
  console.log('數據庫連接已打開');
  try {
    // 獲取所有集合
    const collections = await mongoose.connection.db.listCollections().toArray();
    console.log('可用集合:', collections.map(c => c.name));
    
    // 嘗試獲取一個道具來測試
    const db = mongoose.connection.db;
    const itemsCollection = db.collection('items');
    const sampleItem = await itemsCollection.findOne({});
    console.log('樣本道具:', sampleItem);
  } catch (err) {
    console.error('數據庫診斷失敗:', err);
  }
});

app.use(cors());
app.use(express.json());

// 基本路由
app.get('/', (req, res) => {
    res.send('Hunter 遊戲後端 API');
});

// 項目 API 路由
app.use('/api/items', itemRoutes);

app.listen(PORT, () => {
    console.log(`伺服器運行於 http://localhost:${PORT}`);
});

