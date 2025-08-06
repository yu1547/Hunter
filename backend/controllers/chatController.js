const axios = require('axios');
const User = require('../models/userModel');

const chatWithLLM = async (req, res) => {
  const userId = req.params.id;
  const { message } = req.body;

  if (!message) {
    return res.status(400).json({ error: '格式錯誤，需包含 message' });
  }

  console.log("========== 聊天 API 收到請求 ==========");
  console.log("userId:", userId);
  console.log("message:", message);

  try {
    const user = await User.findById(userId);
    let history = [];
    if (user && user.csrHistory && user.csrHistory.length > 0) {
      // 只取唯一一個 CSRChatHistory 的 history
      history = user.csrHistory[0].history || [];
    }

    // 呼叫 Flask /chat，傳入 message 與 history
    const flaskUrl = process.env.LLM_FLASK_URL || 'http://llm:5050/chat';
    //console.log("送到 Flask 的 payload:", JSON.stringify({ message, history }, null, 2));
    let flaskRes;
    try {
      flaskRes = await axios.post(flaskUrl, { message, history }, { timeout: 300000 });
    } catch (flaskErr) {
      console.error("❌ Flask 連線或回傳錯誤：", flaskErr);
      // 若有回傳錯誤內容，印出
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
    if (user) {
      // 如果沒有csrHistory，初始化唯一一筆
      if (!user.csrHistory || user.csrHistory.length === 0) {
        user.csrHistory = [{
          message: message,
          history: newHistoryItems
        }];
      } else {
        // 只更新唯一一個 CSRChatHistory
        const chatObj = user.csrHistory[0];
        chatObj.history = [...chatObj.history, ...newHistoryItems];
        // 限制最多6個物件，超過則刪除最舊的兩個
        if (chatObj.history.length > 6) {
          chatObj.history = chatObj.history.slice(chatObj.history.length - 6);
        }
        chatObj.message = message;
        user.csrHistory[0] = chatObj;
      }
      try {
        await user.save();
        console.log("✅ 對話已累積存入 user.csrHistory (僅一個物件，history累積)");
      } catch (saveErr) {
        console.error("❌ 存入 csrHistory 失敗：", saveErr);
      }
    } else {
      console.warn("⚠️ 找不到 user，無法存入對話紀錄");
    }

    res.json({ reply });
  } catch (error) {
    console.error("❌ chatWithLLM 發生錯誤：", error);
    res.status(500).json({ error: error.message });
  }
};

module.exports = { chatWithLLM };