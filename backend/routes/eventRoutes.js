// routes/eventRoutes.js
const express = require('express');
const router = express.Router();
const eventController = require('../controllers/temp_eventController');

// 每日自動刷新事件位置的 API，通常由排程任務 (cron job) 調用
router.post('/daily-refresh', eventController.refreshDailyEvents);

// 獲取所有事件的詳細資訊
router.get('/all', eventController.getAllEvents);

// 觸發特定事件，並返回事件資料
router.post('/trigger/:eventId', eventController.triggerEvent);

// 完成事件，並發放獎勵
router.post('/complete/:eventId', eventController.completeEvent);

module.exports = router;