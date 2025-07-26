const express = require('express');
const cors = require('cors');
const { connectDB } = require('./config/db');
const itemRoutes = require('./routes/itemRoutes');
const userRoutes = require('./routes/userRoutes');
const taskRoutes = require('./routes/taskRoutes');
const missionRoutes = require('./routes/missionRoutes'); // 引入 mission 路由
const settingsRoutes = require('./routes/settingsRoutes');
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

// 道具 API 路由
app.use('/api/items', itemRoutes);

// 用戶 API 路由
app.use('/api/users', userRoutes);

// 任務 API 路由
app.use('/api/tasks', taskRoutes);

app.use('/api/settings', settingsRoutes);

// 使用者任務操作 API 路由
app.use('/api', missionRoutes);

app.listen(PORT, () => {
    console.log(`伺服器運行於 http://localhost:${PORT}`);
});

