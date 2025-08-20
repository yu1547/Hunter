// routes/eventRoutes.js
const express = require('express');
const router = express.Router();
const temp_eventController = require('../controllers/temp_eventController');
const {
  trade,
  getStonePileStatus,
  triggerStonePile,
} = require('../controllers/eventController');

// 每日自動刷新事件位置的 API，通常由排程任務 (cron job) 調用
router.post('/daily-refresh', temp_eventController.refreshDailyEvents);

// 獲取所有事件的詳細資訊
router.get('/all', temp_eventController.getAllEvents);

// 觸發特定事件，並返回事件資料
router.post('/trigger/:eventId', temp_eventController.triggerEvent);

// 完成事件，並發放獎勵
router.post('/complete/:eventId', temp_eventController.completeEvent);



// 石堆事件相關的 API
router.get('/stone-pile-status/:userId', getStonePileStatus);
router.post('/trigger-stone-pile', triggerStonePile);

// 交易相關的 API
router.post('/trade', trade);

module.exports = router;