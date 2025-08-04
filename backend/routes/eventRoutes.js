const express = require('express');
const router = express.Router();
const eventController = require('../controllers/eventController');

// 獲取每日事件
router.get('/daily', eventController.getDailyEvents);

// 處理神秘商人交易
router.post('/merchant/exchange', eventController.handleMerchantExchange);

// 處理石堆下的碎片事件
router.post('/stonepile', eventController.handleStonePileEvent);

// 獲取永久事件 (例如任務板上的事件)
router.get('/permanent', eventController.getPermanentEvents);

// 處理所有永久事件的通用觸發
router.post('/:eventId/trigger', eventController.triggerEvent);

// 處理寶箱事件
router.post('/treasurebox/open', eventController.handleTreasureBox);

// 處理史萊姆事件
router.post('/slime/attack', eventController.handleSlimeAttack);

// 處理古樹祝福事件
router.post('/ancienttree/bless', eventController.handleAncientTreeBlessing);

module.exports = router;