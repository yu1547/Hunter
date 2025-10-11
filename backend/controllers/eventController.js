// controllers/eventController.js
const Event = require('../models/eventModel');
const User = require('../models/userModel');
const Item = require('../models/itemModel');
const Spot = require('../models/spotModel');
const mongoose = require('mongoose');
const { addItemsToBackpack } = require('../services/backpackService');
const { generateDropItems } = require('../services/dropService');
const { ObjectId } = mongoose.Types;

// 每日刷新事件位置的邏輯
const refreshDailyEvents = async (req, res) => {
    try {
        // 1. 找到所有每日事件
        const dailyEvents = await Event.find({ type: 'daily' });

        // 2. 假設冷卻時間為 24 小時，並更新每個事件的冷卻時間
        // 這裡設定新的冷卻到期時間為現在時間 + 24 小時
        const cooldownDuration = 24 * 60 * 60 * 1000; // 毫秒
        const newCooldownExpiresAt = new Date(Date.now() + cooldownDuration);

        // 使用 Promise.all 優化批量更新，這會比迴圈中的 await 更有效率
        const updatePromises = dailyEvents.map(event => {
            // 確保位置是固定的，這裡不修改 event.spotId
            event.cooldownExpiresAt = newCooldownExpiresAt; // 更新冷卻時間
            return event.save();
        });

        await Promise.all(updatePromises);

        res.status(200).json({ message: '每日事件冷卻時間已刷新' });
    } catch (error) {
        console.error('每日事件刷新失敗:', error);
        res.status(500).json({ message: '每日事件冷卻時間刷新失敗', error: error.message });
    }
};

// controllers/eventController.js

const completeEvent = async (req, res) => {
    const { eventId } = req.params;
    const { userId, selectedOption, gameResult, keyUsed } = req.body;

    try {
        const event = await Event.findById(eventId);
        if (!event) {
            return res.status(404).json({ message: '找不到此事件' });
        }

        // --- ✅ 步驟 1: 只在函式開頭獲取一次 user 物件 ---
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ message: '找不到使用者' });
        }

        let rewardsToDistribute = null;
        let consumeItems = null;

        // --- 步驟 2: 根據事件類型決定獎勵與消耗 ---
        if (event.name === "在小小的 code 裡面抓阿抓阿抓") {
            if (gameResult === 'win') rewardsToDistribute = event.rewards;
        } else if (event.type === 'chest') {
            if (keyUsed) {
                const keyName = `${keyUsed === 'bronze' ? '銅' : keyUsed === 'silver' ? '銀' : '金'}鑰匙`;
                console.log("keyName:", keyName);
                const keyToConsume = await Item.findOne({ itemName: keyName });

                if (keyToConsume) {
                    const itemIndex = user.backpackItems.findIndex(
                        p => p.itemId.equals(keyToConsume._id)
                    );
                    if (itemIndex > -1 && user.backpackItems[itemIndex].quantity >= 1) {
                        user.backpackItems[itemIndex].quantity -= 1;
                        if (user.backpackItems[itemIndex].quantity <= 0) {
                            user.backpackItems.splice(itemIndex, 1);
                        }
                        // ✅ 手動標記背包已被修改
                        user.markModified('backpackItems');
                    } else {
                        return res.status(400).json({ success: false, message: `你沒有${keyName}。` });
                    }
                } else {
                    return res.status(400).json({ success: false, message: '無效的鑰匙類型。' });
                }
            }
            const dropItems = await generateDropItems(selectedOption);
            rewardsToDistribute = { items: dropItems };
            console.log("掉落物品:", dropItems);
        } else if (event.name === '打扁史萊姆' && typeof gameResult === 'number') {
            const itemName = (gameResult > 30) ? "黃金的史萊姆黏液" : "普通的史萊姆黏液";
            const item = await Item.findOne({ itemName: itemName });
            console.log("打扁史萊姆item:", item);
            if (item) rewardsToDistribute = { items: [{ itemId: item._id, quantity: 1 }] };
            console.log("打扁史萊姆獎勵:", rewardsToDistribute);
        } else if (event.name === '石堆下的碎片') {
            rewardsToDistribute = { items: [{ itemId: "6880f3f7d80b975b33f23e30", quantity: 2 }] };
        } else if (event.name === '古樹的祝福') {
            const selectedSlimeName = selectedOption.trim();
            const slimeToConsume = await Item.findOne({ itemName: selectedSlimeName });

            if (!slimeToConsume) {
                return res.status(400).json({ success: false, message: '無效的獻祭物品' });
            }

            consumeItems = [{ itemId: slimeToConsume._id, quantity: 1 }];

            if (selectedSlimeName === "普通的史萊姆黏液") {
                const rewardItem = await Item.findOne({ itemName: "銅鑰匙碎片" });
                if (rewardItem) {
                    rewardsToDistribute = { items: [{ itemId: rewardItem._id, quantity: 2 }] };
                }
            } else if (selectedSlimeName === "黃金的史萊姆黏液") {
                const rewardItem = await Item.findOne({ itemName: "銀鑰匙碎片" });
                if (rewardItem) {
                    rewardsToDistribute = { items: [{ itemId: rewardItem._id, quantity: 2 }] };
                }
            } else {
                return res.status(400).json({ success: false, message: '不接受此物品的獻祭' });
            }

        }else if (event.name === '神秘商人的試煉') {
            const optionData = event.options.find(opt => opt.text.trim() === selectedOption.trim());
            if (!optionData) {
                return res.status(400).json({ message: '無效的事件選項' });
            }

            // 手動定義消耗物品
            if (selectedOption.includes('銅鑰匙')) {
                const itemToConsume = await Item.findOne({ itemName: "銅鑰匙碎片" });
                if (itemToConsume) {
                    consumeItems = [{ itemId: itemToConsume._id, quantity: 5 }];
                }
            } else if (selectedOption.includes('銀鑰匙')) {
                const itemToConsume = await Item.findOne({ itemName: "銀鑰匙碎片" });
                if (itemToConsume) {
                    consumeItems = [{ itemId: itemToConsume._id, quantity: 5 }];
                }
            }
            
            // 獎勵的部分仍然從資料庫讀取
            rewardsToDistribute = optionData.rewards;

        }  else {
            const optionData = event.options.find(opt => opt.text.trim() === selectedOption.trim());
            if (!optionData) {
                return res.status(400).json({ message: '無效的事件選項' });
            }
            consumeItems = optionData.consume ? optionData.consume.items : [];
            rewardsToDistribute = optionData.rewards;
        }

        // --- ✅ 步驟 3: 在同一個 user 物件上處理消耗 ---
        if (consumeItems && consumeItems.length > 0) {
            for (const itemToConsume of consumeItems) {
                const hasEnough = user.backpackItems.some(
                    p => p.itemId.equals(itemToConsume.itemId) && p.quantity >= itemToConsume.quantity
                );
                if (!hasEnough) {
                    const requiredItem = await Item.findById(itemToConsume.itemId);
                    return res.status(400).json({
                        success: false,
                        message: `缺少必要物品：${requiredItem ? requiredItem.itemName : '未知物品'}，數量不足。`
                    });
                }
            }
            
            // 直接在這裡實作物品扣除邏輯，取代 decFromBackpack
            for (const itemToConsume of consumeItems) {
                const itemIndex = user.backpackItems.findIndex(
                    p => p.itemId.equals(itemToConsume.itemId)
                );

                if (itemIndex > -1) {
                    // 找到物品，直接更新數量
                    user.backpackItems[itemIndex].quantity -= itemToConsume.quantity;

                    // 如果數量歸零或更少，就從背包中移除
                    if (user.backpackItems[itemIndex].quantity <= 0) {
                        user.backpackItems.splice(itemIndex, 1);
                    }
                }
            }
        }
        
        // --- ✅ 步驟 4: 在同一個 user 物件上處理獎勵 ---
        const finalDropNames = [];
        if (rewardsToDistribute) {
            if (rewardsToDistribute.points) {
                console.log("原本積分:", user.score || 0);
                user.score = (user.score || 0) + rewardsToDistribute.points;
                console.log("獲得積分:", rewardsToDistribute.points);
                console.log("使用者新積分:", user.score);
            }
            if (rewardsToDistribute.items && rewardsToDistribute.items.length > 0) {
                await addItemsToBackpack(user, rewardsToDistribute.items);
                for (const rewardItem of rewardsToDistribute.items) {
                    const itemInfo = await Item.findById(rewardItem.itemId).lean();
                    if (itemInfo) finalDropNames.push(itemInfo.itemName);
                    console.log("獲得物品:", itemInfo ? itemInfo.itemName : '未知物品');
                }
            }
        }
        
        // --- ✅ 步驟 5: 在同一個 user 物件上更新任務狀態 ---
        const mission = user.missions.find(m => m.taskId.toString() === eventId.toString());
        console.log("找到的任務:", mission);
        console.log("任務 ID:", eventId);
        console.log("使用者的任務列表:", user.missions);
        if (mission) {
            mission.state = 'completed'; // 將任務標記為 'completed'，以便前端可以觸發領取獎勵
        }

        // --- ✅ 步驟 6: 在所有操作結束後，只儲存一次 ---
        await user.save();

        // --- 步驟 7: 回傳結果 ---
        res.status(200).json({
            success: true,
            message: '事件完成，獎勵已發放。',
            drops: finalDropNames
        });
    } catch (error) {
        console.error("完成事件時發生錯誤：", error);
        res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};

// 查詢地點是否有每日事件的共用 API
const getDailyEventBySpot = async (req, res) => {
    try {
        const { spotId } = req.params;
        const event = await Event.findOne({ spotId: spotId, type: 'daily' });
        if (!event) {
            return res.status(200).json({ success: true, hasEvent: false });
        }
        return res.status(200).json({ success: true, hasEvent: true, eventName: event.name });
    } catch (error) {
        console.error("查詢每日事件失敗:", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};

// 處理神秘商人交易的 API
const trade = async (req, res) => {
    const { userId, tradeType } = req.body;

    try {
        if (!mongoose.Types.ObjectId.isValid(userId)) {
            return res.status(400).json({ success: false, message: '無效的使用者 ID。' });
        }

        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: '找不到使用者。' });
        }

        const event = await Event.findOne({ name: '神秘商人的試煉' });
        if (!event) {
            return res.status(404).json({ success: false, message: '神秘商人的試煉事件不存在' });
        }

        // 檢查使用者是否已經在今天交易過
        const today = new Date().toISOString().slice(0, 10);
        const hasTriggeredToday = user.lastTradeDate && user.lastTradeDate.toISOString().slice(0, 10) === today;

        if (hasTriggeredToday) {
            return res.json({ success: false, message: '你今天已經交易過了，請明天再來。' });
        }
        
        // 根據 tradeType 找到對應的 option
        let option;
        if (tradeType === 'bronzeKey') {
            // 尋找 text 中包含 "銅鑰匙" 的選項
            option = event.options.find(opt => opt.text.includes('銅鑰匙'));
        } else if (tradeType === 'silverKey') {
            // 尋找 text 中包含 "銀鑰匙" 的選項
            option = event.options.find(opt => opt.text.includes('銀鑰匙'));
        }
        
        console.log("使用者選項:", event.options);
        console.log("選擇的交易類型:", tradeType, option);
        if (!option) {
            return res.status(400).json({ success: false, message: '無效的交易類型' });
        }
        
        // 更新使用者的交易紀錄
        user.lastTradeDate = new Date();
        // await user.save();

        req.params.eventId = event._id;
        req.body.userId = userId;
        req.body.selectedOption = option.text;

        return await completeEvent(req, res);

    } catch (error) {
        console.error("交易失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};


// 處理觸發石堆事件的 API
const triggerStonePile = async (req, res) => {
    const { userId } = req.body;

    try {
        if (!mongoose.Types.ObjectId.isValid(userId)) {
            return res.status(400).json({ success: false, message: '無效的使用者 ID。' });
        }

        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: userId+'找不到使用者。' });S
        }

        const today = new Date().toISOString().slice(0, 10);
        const hasTriggeredToday = user.lastStonePileTriggeredDate && user.lastStonePileTriggeredDate.toISOString().slice(0, 10) === today;

        if (hasTriggeredToday) {
            return res.json({ success: false, message: '你今天已經搬開過石頭了，請明天再來。' });
        }

        // 更新觸發紀錄
        user.lastStonePileTriggeredDate = new Date();
        await user.save();

        const event = await Event.findOne({ name: '石堆下的碎片' });
        if (!event) {
            return res.status(404).json({ success: false, message: '石堆下的碎片不存在' });
        }

        req.params.eventId = event._id;
        req.body.userId = userId;

        return await completeEvent(req, res);

    } catch (error) {
        console.error("觸發石堆下的碎片失敗：", error);
        return res.status(500).json({ success: false, message: `伺服器內部錯誤: ${error.message}` });
    }
};

// 獲取單個事件
const getEventById = async (req, res) => {
    try {
        const event = await Event.findById(req.params.eventId);
        if (!event) {
            return res.status(404).json({ message: '找不到此事件' });
        }
        res.status(200).json(event);
    } catch (error) {
        res.status(500).json({ message: '獲取事件失敗', error: error.message });
    }
};

const getStonePileStatus = async (req, res) => {
    try {
        const { userId } = req.params;
        const user = await User.findById(userId);

        if (!user) {
            return res.status(404).json({ success: false, message: '找不到使用者。' });
        }

        const today = new Date().toISOString().slice(0, 10);
        const hasTriggeredToday = user.lastStonePileTriggeredDate && user.lastStonePileTriggeredDate.toISOString().slice(0, 10) === today;

        return res.status(200).json({ hasTriggeredToday });
    } catch (error) {
        console.error("獲取石堆狀態失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};

module.exports = {
    getDailyEventBySpot,
    trade,
    triggerStonePile,
    completeEvent,
    refreshDailyEvents,
    getEventById,
    getStonePileStatus,
};
