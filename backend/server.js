const express = require('express');
const cors = require('cors');
const { connectDB } = require('./config/db');
const itemRoutes = require('./routes/itemRoutes');
const userRoutes = require('./routes/userRoutes');

const dropRoutes = require('./routes/dropRoutes'); // 掉落機制
const spotRoutes = require("./routes/spotRoutes"); // 收藏冊
const suppliesRoutes = require('./routes/suppliesRoutes');//補給站

const settingsRoutes = require('./routes/settingsRoutes');
const rankRoutes = require('./routes/rankRoutes');

const authRoutes = require('./routes/authRoutes'); // 認證路由

// 事件相關
const taskRoutes = require('./routes/taskRoutes');
const missionRoutes = require('./routes/missionRoutes');
const eventRoutes = require('./routes/eventRoutes');

// LLM
const chatRoutes = require('./routes/chatRoutes');
const recognitionRoutes = require('./routes/recognitionRoutes');

const app = express();
const PORT = process.env.PORT || 4000;
const mongoose = require('mongoose');

// ========== 🔑 Firebase Admin 初始化 ==========
const admin = require("firebase-admin");
const gac = process.env.GOOGLE_APPLICATION_CREDENTIALS;
if (!gac) throw new Error('GOOGLE_APPLICATION_CREDENTIALS not set');

let serviceAccount;

if (gac.trim().startsWith('{')) {
  // Render：環境變數放整包 JSON
  serviceAccount = JSON.parse(gac);
  if (serviceAccount.private_key?.includes('\\n')) {
    serviceAccount.private_key = serviceAccount.private_key.replace(/\\n/g, '\n');
  }
} else {
  serviceAccount = require(process.env.GOOGLE_APPLICATION_CREDENTIALS);
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});


// 🔑 驗證 Token 的 middleware
const authenticate = async (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ message: "缺少授權標頭" });
  }

  const idToken = authHeader.split(" ")[1];
  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    req.user = decoded; // 把解碼結果存進 req.user，後續 controller 可以用
    next();
  } catch (error) {
    console.error("驗證失敗:", error);
    return res.status(401).json({ message: "Token 無效或過期" });
  }
};

// ==============================================

// 連接到資料庫
connectDB();

// 添加數據庫診斷代碼
mongoose.connection.once('open', async () => {
  console.log('數據庫連接已打開');
  try {
    const collections = await mongoose.connection.db.listCollections().toArray();
    console.log('可用集合:', collections.map(c => c.name));
  } catch (err) {
    console.error('數據庫診斷失敗:', err);
  }
});

app.use(cors());
app.use(express.json());

// debug
const testRoutes = require('./routes/testRoutes');
app.use('/api/debug', testRoutes);

// 認證路由 (不需要驗證 Token，因為登入就是在這裡做)
app.use("/api/auth", authRoutes);

// 基本路由
app.get('/', (req, res) => {
  res.send('Hunter 遊戲後端 API');
});

app.use('/api/items', authenticate, itemRoutes);
app.use('/api/users', authenticate, userRoutes);
app.use('/api/tasks', authenticate, taskRoutes);
app.use('/api', authenticate, missionRoutes);
app.use('/api/events', authenticate, eventRoutes);
app.use('/api/rank', authenticate, rankRoutes);
app.use('/api/drop', authenticate, dropRoutes);
app.use("/api/spots", authenticate, spotRoutes);
app.use('/api/settings', authenticate, settingsRoutes);
app.use('/api/recognize', authenticate, recognitionRoutes);
app.use('/api/chat', authenticate, chatRoutes);
app.use('/api/supplies', authenticate, suppliesRoutes);
app.get('/health', (req,res)=>res.status(200).send('ok'));

app.listen(PORT, '0.0.0.0', () => console.log(`Server on ${PORT}`));
