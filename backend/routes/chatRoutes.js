const express = require('express');
const router = express.Router();
const { chatWithLLM, deleteChatHistory } = require('../controllers/chatController');

// 聊天對話 API
router.post('/:id', chatWithLLM);  // POST /chat/:id

// 新增：刪除對話紀錄
router.delete('/:id', deleteChatHistory); // DELETE /chat/:id

module.exports = router;
