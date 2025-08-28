// routes/eventRoutes.js
const express = require('express');
const router = express.Router();
const {
    getDailyEventBySpot,
    trade,
    triggerStonePile,
    completeEvent,
    refreshDailyEvents,
    getEventById,
} = require('../controllers/eventController');

// 每日自動刷新事件位置的 API，通常由排程任務 (cron job) 調用
router.post('/daily-refresh', refreshDailyEvents);

// 完成事件，並發放獎勵
router.post('/complete/:eventId', completeEvent);

// 新增的共用 API，用於查詢特定地點是否有每日事件
router.get('/daily-event/:spotId', getDailyEventBySpot);

// 獲取單個事件的詳細資訊
router.get('/:eventId', getEventById);

// 石堆事件相關的 API
router.post('/trigger-stone-pile', triggerStonePile);

// 商人交易相關的 API
router.post('/trade', trade);

module.exports = router;