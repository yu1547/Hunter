// Express 主程式，註冊路由、啟動伺服器

const express = require('express');
const cors = require('cors');
const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// 掉落機制入口
const dropRoutes = require("./routes/dropRoutes");
app.use("/api/drop", dropRoutes);

app.get('/', (req, res) => {
    res.send('Hello from backend!');
});

app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});

