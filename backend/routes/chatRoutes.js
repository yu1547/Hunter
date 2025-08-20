const express = require('express');
const router = express.Router();
const { chatWithLLM } = require('../controllers/chatController');

// 聊天對話 API
router.post('/:id', chatWithLLM);  // POST /chat/:id

module.exports = router;
