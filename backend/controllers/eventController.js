// controllers/eventController.js
const Event = require('../models/eventModel');
const User = require('../models/userModel');
const Item = require('../models/itemModel');
const Spot = require('../models/spotModel');
const mongoose = require('mongoose');

// 每日刷新事件位置的邏輯
const refreshDailyEvents = async (req, res) => {
    try {
        const dailyEvents = await Event.find({ type: 'daily' });
        const allSpots = await Spot.find();

        for (const event of dailyEvents) {
            const randomSpot = allSpots[Math.floor(Math.random() * allSpots.length)];
            event.spotId = randomSpot._id;
            await event.save();
        }
        res.status(200).json({ message: '每日事件位置已刷新' });
    } catch (error) {
        res.status(500).json({ message: '每日事件刷新失敗', error: error.message });
    }
};

// 完成事件，並發放獎勵
const completeEvent = async (req, res) => {
    const { eventId } = req.params;
    const { userId, selectedOption, gameResult, keyUsed } = req.body;

    try {
        const event = await Event.findById(eventId);
        if (!event) {
            return res.status(404).json({ message: '找不到此事件' });
        }

        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ message: '找不到使用者' });
        }

        let rewardsToDistribute = { score: 0, items: [] };
        
        // ===========================================
        // Wordle 遊戲的特殊處理邏輯
        // ===========================================
        if (event.name === "在小小的 code 裡面抓阿抓阿抓") {
            if (gameResult === 'win') {
                event.status = 'completed';
                rewardsToDistribute = { 
                    items: [] 
                }; 
            } else if (gameResult === 'lose') {
                event.status = 'claimed';
                rewardsToDistribute = { items: [] };
            }
        }
        // ===========================================
        // 統一處理所有寶箱事件（銅、銀、金）
        // ===========================================
        else if (event.type === 'chest') {
            if (keyUsed) {
                const keyToConsume = await Item.findOne({ itemName: keyUsed });
                if (keyToConsume) {
                    await decFromBackpack(userId, keyToConsume._id);
                } else {
                    return res.status(400).json({ success: false, message: '無效的鑰匙類型。' });
                }
            }

            const dropItems = await generateDropItems(selectedOption);
            rewardsToDistribute.items = dropItems;
            event.status = 'completed';
        }

        // ===========================================
        // 史萊姆戰鬥的特殊處理邏輯
        // ===========================================
        else if (event.name === '打扁史萊姆' && gameResult !== undefined) {
            let finalDamage = gameResult;
            const slimeRewards = [];
            
            if (finalDamage > 100) {
                const item = await Item.findOne({ itemName: "黏稠的史萊姆黏液" });
                if (item) {
                    slimeRewards.push({ itemId: item._id, quantity: 1 });
                }
            } else {
                const item = await Item.findOne({ itemName: "普通的史萊姆黏液" });
                if (item) {
                    slimeRewards.push({ itemId: item._id, quantity: 1 });
                }
            }
            rewardsToDistribute.items = slimeRewards;
            event.status = 'completed';
        } 
        // ===========================================
        // 處理通用事件（古樹、神秘商人、石堆等）
        // ===========================================
        else {
            const optionData = event.options.find(opt => opt.name === selectedOption);
            if (!optionData) {
                return res.status(400).json({ message: '無效的事件選項' });
            }

            if (optionData.consume && optionData.consume.length > 0) {
                for (const consumeItem of optionData.consume) {
                    const itemInBackpack = user.backpackItems.find(
                        item => item.itemId.equals(consumeItem.itemId)
                    );
                    if (!itemInBackpack || itemInBackpack.quantity < consumeItem.quantity) {
                        const requiredItem = await Item.findById(consumeItem.itemId);
                        const itemName = requiredItem ? requiredItem.itemName : '未知物品';
                        return res.status(400).json({ 
                            success: false, 
                            message: `缺少必要物品：${itemName}，數量不足。` 
                        });
                    }
                    itemInBackpack.quantity -= consumeItem.quantity;
                }
            }
            rewardsToDistribute = optionData.rewards;
            event.status = 'completed';
        }

        // ===========================================
        // 統一發放獎勵
        // ===========================================
        if (rewardsToDistribute.score) {
            user.score += rewardsToDistribute.score;
        }
        if (rewardsToDistribute.items && rewardsToDistribute.items.length > 0) {
            await addItemsToBackpack(user, rewardsToDistribute.items);
        }

        await event.save();
        await user.save();

        res.status(200).json({
            success: true,
            message: '事件完成，獎勵已發放。',
            rewards: rewardsToDistribute.items
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

        const event = await Event.findOne({ name: '神秘商人' });
        if (!event) {
            return res.status(404).json({ success: false, message: '神秘商人事件不存在' });
        }

        // 檢查使用者是否已經在今天交易過
        const today = new Date().toISOString().slice(0, 10);
        const hasTriggeredToday = user.lastTradeDate && user.lastTradeDate.toISOString().slice(0, 10) === today;

        if (hasTriggeredToday) {
            return res.json({ success: false, message: '你今天已經交易過了，請明天再來。' });
        }

        const option = event.options.find(opt => opt.key === tradeType);
        if (!option) {
            return res.status(400).json({ success: false, message: '無效的交易類型' });
        }
        
        // 更新使用者的交易紀錄
        user.lastTradeDate = new Date();
        await user.save();

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
            return res.status(404).json({ success: false, message: '找不到使用者。' });
        }

        const today = new Date().toISOString().slice(0, 10);
        const hasTriggeredToday = user.lastStonePileTriggeredDate && user.lastStonePileTriggeredDate.toISOString().slice(0, 10) === today;

        if (hasTriggeredToday) {
            return res.json({ success: false, message: '你今天已經搬開過石頭了，請明天再來。' });
        }

        // 更新觸發紀錄
        user.lastStonePileTriggeredDate = new Date();
        await user.save();

        const event = await Event.findOne({ name: '石堆事件' });
        if (!event) {
            return res.status(404).json({ success: false, message: '石堆事件不存在' });
        }

        req.params.eventId = event._id;
        req.body.userId = userId;

        return await completeEvent(req, res);

    } catch (error) {
        console.error("觸發石堆事件失敗：", error);
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

module.exports = {
    getDailyEventBySpot,
    trade,
    triggerStonePile,
    completeEvent,
    refreshDailyEvents,
    getEventById,
};