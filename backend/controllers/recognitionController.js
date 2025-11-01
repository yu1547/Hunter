// recognitionController.js
const axios = require('axios');
const recognitionService = require('../services/recognitionService');

const LLM_BASE = process.env.LLM_BASE || 'http://llm:5050';

// 印前端收到的body（特徵向量只印長度與前 8 筆）
function logIncoming(req) {
    const { userId, spotName, vector } = req.body || {};
    console.log('[recognize] body =', JSON.stringify({
        userId,
        spotName,
        vectorLen: Array.isArray(vector) ? vector.length : null,
        vectorHead: Array.isArray(vector) ? vector.slice(0, 8) : null
    }, null, 2));
}

//正常比對路徑
exports.handleRecognition = async (req, res) => {
    const { userId, spotName, vector } = req.body;
    logIncoming(req);

    try {
        // 呼叫 llm 容器的比對 API
        const { data } = await axios.post(`${LLM_BASE}/compare`, { spotName, vector }, { timeout: 20000 });

        const recognitionSuccess = data.matched === true && data.predicted === spotName;

        if (recognitionSuccess) {
            const result = await recognitionService.updateUserRecognition(userId, spotName);
            return res.json({ success: true, updatedLogs: result, score: data.score, predicted: data.predicted, reason: data.reason });
        } else {
            return res.json({
                success: false,
                message: "辨識失敗或與 spotName 不一致",
                predicted: data.predicted,
                score: data.score,
                reason: data.reason
            });

        }
    } catch (error) {
        console.error("辨識流程出錯：", error?.response?.data || error.message);
        return res.status(500).json({ success: false, message: "內部錯誤" });
    }
};

// 測試用：強制成功，不經過比對
exports.handleRecognitionTrue = async (req, res) => {
    const { userId, spotName, vector } = req.body; // 名稱保留
    logIncoming(req);
    try {
        const result = await recognitionService.updateUserRecognition(userId, spotName);
        return res.json({ success: true, updatedLogs: result });
    } catch (error) {
        console.error("測試流程出錯：", error);
        return res.status(500).json({ success: false, message: "內部錯誤" });
    }
};
