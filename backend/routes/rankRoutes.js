// backend/routes/rankRoutes.js

const express = require('express');
const router = express.Router();
const { getRank } = require('../controllers/rankController');
// const { protect } = require('../middleware/authMiddleware'); // 如果有需要使用者驗證才可查看排行榜，請啟用這行並建立 authMiddleware

router.get('/', getRank); // GET /api/rank，獲取排行榜
// router.get('/', protect, getRank); // 如果需要登入才能看排行榜，請改用這行

module.exports = router;