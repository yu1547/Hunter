// routes/eventRoutes.js
const express = require('express');
const router = express.Router();
const {
  trade,
  getStonePileStatus,
  triggerStonePile,
} = require('../controllers/eventController');

const {
  refreshDailyEvents,
  completeEvent,
} = require('../controllers/temp_eventController');

// 每日自動刷新事件位置的 API，通常由排程任務 (cron job) 調用
router.post('/daily-refresh', refreshDailyEvents);

// 完成事件，並發放獎勵
router.post('/complete/:eventId', completeEvent);

// 石堆事件相關的 API
router.get('/stone-pile-status/:userId', getStonePileStatus);
router.post('/trigger-stone-pile', triggerStonePile);

// 商人交易相關的 API
router.post('/trade', trade);

module.exports = router;