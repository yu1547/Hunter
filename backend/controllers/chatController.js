const axios = require('axios');
const Chat = require('../models/chatModel');
const mongoose = require('mongoose');

const chatWithLLM = async (req, res) => {
  const userId = req.params.id;
  // 這裡同時取得 message 與 history
  const { message, history } = req.body;

  if (!message) {
    return res.status(400).json({ error: '格式錯誤，需包含 message' });
  }

  if (!mongoose.Types.ObjectId.isValid(userId)) {
    return res.status(400).json({ error: 'userId 不是合法 ObjectId' });
  }

  console.log("========== 聊天 API 收到請求 ==========");
  console.log("userId:", userId);
  console.log("message:", message);
  //console.log("history:", Array.isArray(history) ? history.length : '無');

  try {
    // 優先用前端傳來的 history
    let chat = await Chat.findOne({ userId: new mongoose.Types.ObjectId(userId) });
    let usedHistory = [];
    if (Array.isArray(history) && history.length > 0) {
      usedHistory = history;
    } else if (chat && Array.isArray(chat.history)) {
      usedHistory = chat.history;
    } else {
      usedHistory = [];
    }

    // 呼叫 Flask /chat，傳入 message 與 history
    const flaskUrl = process.env.LLM_FLASK_URL || 'http://llm:5050/chat';
    let flaskRes;
    try {
      flaskRes = await axios.post(flaskUrl, { message, history: usedHistory }, { timeout: 300000 });
    } catch (flaskErr) {
      // 更詳細的錯誤日誌
      console.error("❌ Flask 連線或回傳錯誤：", flaskErr);
      console.error("❌ 傳送給 Flask 的參數：", { message, history: usedHistory });
      if (flaskErr.response) {
        console.error("❌ Flask 回傳狀態碼：", flaskErr.response.status);
        console.error("❌ Flask 回傳內容：", flaskErr.response.data);
      }
      if (flaskErr.response && flaskErr.response.data && flaskErr.response.data.error) {
        const flaskErrorMsg = flaskErr.response.data.error;
        console.error("❌ Flask 回傳錯誤內容：", flaskErrorMsg);
        // 特別處理 llama runner crash
        if (flaskErrorMsg.includes("llama runner process has terminated")) {
          return res.status(503).json({ error: "AI 服務暫時無法回應，請稍後再試。" });
        }
        return res.status(500).json({ error: flaskErrorMsg });
      }
      // 若 Flask 回傳不是預期格式
      if (flaskErr.response && flaskErr.response.data) {
        return res.status(500).json({ error: "Flask 伺服器錯誤", flaskData: flaskErr.response.data });
      }
      return res.status(500).json({ error: "Flask 連線或回傳錯誤", detail: flaskErr.message });
    }
    // 檢查 Flask 回傳格式
    if (!flaskRes.data || typeof flaskRes.data.reply !== "string") {
      console.error("❌ Flask 回傳格式錯誤：", flaskRes.data);
      return res.status(500).json({ error: "Flask 回傳格式錯誤", flaskData: flaskRes.data });
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
        userId: new mongoose.Types.ObjectId(userId),
        message: message,
        history: newHistoryItems
      });
    } else {
      chat.history = [...chat.history, ...newHistoryItems];
      if (chat.history.length > 6) {
        chat.history = chat.history.slice(chat.history.length - 6);
      }
      chat.message = message;
      chat.userId = new mongoose.Types.ObjectId(userId);
    }
    try {
      await chat.save();
      console.log("✅ 對話已累積存入 chat (object，history累積)");
    } catch (saveErr) {
      console.error("❌ 存入 chat 失敗：", saveErr);
      return res.status(500).json({ error: '存入 chat 失敗', detail: saveErr.message });
    }

    // 明確回傳 JSON 格式
    return res.json({ reply });
  } catch (error) {
    console.error("❌ chatWithLLM 發生錯誤：", error);
    return res.status(500).json({ error: error.message });
  }
};

const deleteChatHistory = async (req, res) => {
  const userId = req.params.id;
  if (!mongoose.Types.ObjectId.isValid(userId)) {
    return res.status(400).json({ error: 'userId 不是合法 ObjectId' });
  }
  try {
    // 只清空 history，不刪除 chat 文件
    const result = await Chat.updateOne(
      { userId: new mongoose.Types.ObjectId(userId) },
      { $set: { history: [] } }
    );
    if (result.matchedCount === 0) {
      return res.status(404).json({ error: '找不到對應 chat 紀錄' });
    }
    return res.json({ success: true });
  } catch (error) {
    console.error("❌ 清空 chat history 失敗：", error);
    return res.status(500).json({ error: '清空 chat history 失敗', detail: error.message });
  }
};

module.exports = { chatWithLLM, deleteChatHistory };
