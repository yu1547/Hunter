const User = require('../models/userModel');
const Item = require('../models/itemModel');
const mongoose = require('mongoose');

// 處理神秘商人交易的 API
const trade = async (req, res) => {
    const { userId, tradeType } = req.body;

    try {
        const user = await User.findById(userId);
        if (!user) return res.status(404).json({ success: false, message: '使用者不存在。' });

        let requiredItemName, requiredCount, givenItemName;
        if (tradeType === "bronzeKey") {
            requiredItemName = "銅鑰匙碎片";
            requiredCount = 5;
            givenItemName = "銅鑰匙";
        } else if (tradeType === "silverKey") {
            requiredItemName = "銀鑰匙碎片";
            requiredCount = 5;
            givenItemName = "銀鑰匙";
        } else {
            return res.status(400).json({ success: false, message: '無效的交易類型' });
        }

        const requiredItem = await Item.findOne({ itemName: requiredItemName });
        if (!requiredItem) return res.status(404).json({ success: false, message: '所需物品不存在。' });

        const requiredItemInInv = user.backpackItems.find(item => item.itemId.toString() === requiredItem._id.toString());
        if (!requiredItemInInv || requiredItemInInv.quantity < requiredCount) {
            return res.json({ success: false, message: `你的${requiredItemName}不足${requiredCount}個。` });
        }

        const givenItem = await Item.findOne({ itemName: givenItemName });
        if (!givenItem) return res.status(404).json({ success: false, message: '給予物品不存在。' });

        // 執行交易：扣除所需物品，給予新物品
        requiredItemInInv.quantity -= requiredCount;

        const givenItemInInv = user.backpackItems.find(item => item.itemId.toString() === givenItem._id.toString());
        if (givenItemInInv) {
            givenItemInInv.quantity++;
        } else {
            user.backpackItems.push({ itemId: givenItem._id.toString(), quantity: 1 });
        }

        await user.save();

        return res.json({ success: true, message: `交易成功！你獲得了1個${givenItemName}。` });

    } catch (error) {
        console.error("交易失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};

// 獲取石堆事件狀態的 API
const getStonePileStatus = async (req, res) => {
    try {
        const { userId } = req.params;

        // 1. 確保使用者 ID 是有效的 ObjectId 格式，如果無效則直接返回錯誤。
        if (!mongoose.Types.ObjectId.isValid(userId)) {
            return res.status(400).json({ success: false, message: '無效的使用者 ID。' });
        }

        // 2. 查找使用者。
        const user = await User.findById(userId);

        // 3. 檢查使用者是否存在。如果不存在，返回 404 錯誤。
        if (!user) {
            return res.status(404).json({ success: false, message: '找不到使用者。' });
        }

        // 4. 執行核心邏輯：判斷今天是否已觸發過石堆事件。
        const today = new Date().toISOString().slice(0, 10);
        // 如果 user.lastStonePileTriggeredDate 為 null，則 lastTriggeredDate 也會是 null
        const lastTriggeredDate = user.lastStonePileTriggeredDate ? new Date(user.lastStonePileTriggeredDate).toISOString().slice(0, 10) : null;
        const canTrigger = lastTriggeredDate !== today;

        const available = {
            stonePile: canTrigger
        };

        return res.json({ success: true, available });

    } catch (error) {
        console.error("獲取石堆狀態失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤: ${error.message}' });
    }
};

// 處理觸發石堆事件的 API
const triggerStonePile = async (req, res) => {
    const { userId } = req.body; // <-- 請將這裡改為 req.body

    try {
        // 1. 確保使用者 ID 是有效的 ObjectId 格式，如果無效則直接返回錯誤。
        if (!mongoose.Types.ObjectId.isValid(userId)) {
            return res.status(400).json({ success: false, message: '無效的使用者 ID。' });
        }

        // 2. 查找使用者。
        const user = await User.findById(userId);

        const today = new Date().toISOString().slice(0, 10);
        const hasTriggeredToday = user.lastStonePileTriggeredDate && user.lastStonePileTriggeredDate.toISOString().slice(0, 10) === today;
        if (hasTriggeredToday) {
            return res.json({ success: false, message: '你今天已經搬開過石頭了，請明天再來。' });
        }

        // 確保 user.backpackItems 是陣列，如果不存在則初始化
        if (!user.backpackItems) {
            user.backpackItems = [];
        }

        const rewardItem = await Item.findOne({ itemName: "普通的史萊姆黏液" });

        // 檢查 `rewardItem` 是否存在，如果不存在，返回 404 錯誤
        if (!rewardItem) {
             return res.status(404).json({ success: false, message: '獎勵物品不存在。' });
        }

        const reward = { points: 10, items: [{ itemId: rewardItem._id.toString(), quantity: 1 }] };

        user.score = (user.score || 0) + reward.points;
        const existingItem = user.backpackItems.find(item => item.itemId.toString() === rewardItem._id.toString());
        if (existingItem) {
            existingItem.quantity++;
        } else {
            user.backpackItems.push({ itemId: rewardItem._id.toString(), quantity: 1 });
        }

        user.lastStonePileTriggeredDate = new Date();

        await user.save();

        return res.json({ success: true, message: '你搬開了石頭，獲得積分+${reward.points}和物品。' });

    } catch (error) {
        console.error("觸發石堆事件失敗：", error);
        return res.status(500).json({ success: false, message: `伺服器內部錯誤: ${error.message}` });
    }
};

module.exports = {
    trade,
    getStonePileStatus,
    triggerStonePile,
};
