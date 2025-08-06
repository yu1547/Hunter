const axios = require('axios');
const Chat = require('../models/chatModel');
const mongoose = require('mongoose');

const chatWithLLM = async (req, res) => {
  const userId = req.params.id;
  const { message } = req.body;

  if (!message) {
    return res.status(400).json({ error: '格式錯誤，需包含 message' });
  }

  // 檢查 userId 是否為合法 ObjectId
  if (!mongoose.Types.ObjectId.isValid(userId)) {
    return res.status(400).json({ error: 'userId 不是合法 ObjectId' });
  }

  console.log("========== 聊天 API 收到請求 ==========");
  console.log("userId:", userId);
  console.log("message:", message);

  try {
    // 查詢時要用 csrHistory.userId
    let chat = await Chat.findOne({ 'csrHistory.userId': new mongoose.Types.ObjectId(userId) });
    let history = [];
    if (chat && chat.csrHistory && Array.isArray(chat.csrHistory.history)) {
      history = chat.csrHistory.history;
    }

    // 呼叫 Flask /chat，傳入 message 與 history
    const flaskUrl = process.env.LLM_FLASK_URL || 'http://llm:5050/chat';
    let flaskRes;
    try {
      flaskRes = await axios.post(flaskUrl, { message, history }, { timeout: 300000 });
    } catch (flaskErr) {
      console.error("❌ Flask 連線或回傳錯誤：", flaskErr);
      if (flaskErr.response && flaskErr.response.data && flaskErr.response.data.error) {
        console.error("❌ Flask 回傳錯誤內容：", flaskErr.response.data.error);
        return res.status(500).json({ error: flaskErr.response.data.error });
      }
      return res.status(500).json({ error: "Flask 連線或回傳錯誤", detail: flaskErr.message });
    }
    const reply = flaskRes.data.reply;
    console.log("Flask 回覆 reply:", reply);

    // 新增本次 user/LLM 對話
    const now = new Date().toISOString();
    const newHistoryItems = [
      {
        role: "user",
        content: message,
        timestamp: now
      },
      {
        role: "LLM",
        content: reply,
        timestamp: now
      }
    ];

    // 更新資料庫
    if (!chat) {
      chat = new Chat({
        csrHistory: {
          userId: new mongoose.Types.ObjectId(userId),
          message: message,
          history: newHistoryItems
        }
      });
    } else {
      chat.csrHistory.history = [...chat.csrHistory.history, ...newHistoryItems];
      if (chat.csrHistory.history.length > 6) {
        chat.csrHistory.history = chat.csrHistory.history.slice(chat.csrHistory.history.length - 6);
      }
      chat.csrHistory.message = message;
      chat.csrHistory.userId = new mongoose.Types.ObjectId(userId);
    }
    try {
      await chat.save();
      console.log("✅ 對話已累積存入 chat.csrHistory (object，history累積)");
    } catch (saveErr) {
      console.error("❌ 存入 chat.csrHistory 失敗：", saveErr);
      return res.status(500).json({ error: '存入 chat.csrHistory 失敗', detail: saveErr.message });
    }

    res.json({ reply });
  } catch (error) {
    console.error("❌ chatWithLLM 發生錯誤：", error);
    res.status(500).json({ error: error.message });
  }
};

module.exports = { chatWithLLM };