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
    const { userId, username, selectedOption, gameResult } = req.body;

    try {
        const event = await Event.findById(eventId);
        if (!event) {
            return res.status(404).json({ message: '找不到此事件' });
        }

        
        const user = await User.findById(userId);

        if (!user && username) {
            user = await User.findOne({ username: username });
        }

        if (!user) {
            return res.status(404).json({ message: '找不到使用者' });
        }

        let finalRewards = event.rewards;
        let consumeItems = event.consume;

        if (selectedOption) {
            const option = event.options.find(opt => opt.text === selectedOption);
            if (!option) {
                return res.status(400).json({ message: '無效的選項' });
            }
            if (option.rewards.consume) {
                for (const itemToConsume of option.rewards.consume) {
                    const userItem = user.items.find(item => item.itemId.toString() === itemToConsume.itemId.toString());
                    if (!userItem || userItem.quantity < itemToConsume.quantity) {
                        return res.status(400).json({ message: '道具數量不足' });
                    }
                }
            }
            finalRewards = option.rewards;
            consumeItems = option.rewards.consume;
        }

        if (event.name === '打扁史萊姆' && gameResult) {
            let slimeRewardItem;
            if (gameResult < 100) {
                slimeRewardItem = { itemId: (await Item.findOne({ name: '普通的史萊姆黏液' }))._id, quantity: 1 };
            } else if (gameResult < 150) {
                slimeRewardItem = { itemId: (await Item.findOne({ name: '普通的史萊姆黏液' }))._id, quantity: 2 };
            } else if (gameResult < 200) {
                slimeRewardItem = { itemId: (await Item.findOne({ name: '普通的史萊姆黏液' }))._id, quantity: 3 };
            } else {
                slimeRewardItem = { itemId: (await Item.findOne({ name: '黏稠的史萊姆黏液' }))._id, quantity: Math.floor(gameResult / 50) - 3 };
            }
            finalRewards = { items: [slimeRewardItem] };
        }
        // 發放獎勵
        if (finalRewards) {
            if (finalRewards.points) {
                user.points += finalRewards.points;
            }
            if (finalRewards.items) {
                for (const rewardItem of finalRewards.items) {
                    const userItem = user.items.find(item => item.itemId.toString() === rewardItem.itemId.toString());
                    if (userItem) {
                        userItem.quantity += rewardItem.quantity;
                    } else {
                        user.items.push({ itemId: rewardItem.itemId, quantity: rewardItem.quantity });
                    }
                }
            }
        }
        event.state = 'completed';
        await event.save();
        // 移除數量為零的道具
        user.items = user.items.filter(item => item.quantity > 0);
        
        await user.save();

        res.status(200).json({ message: '事件完成，獎勵已發放', rewards: finalRewards, updatedUser: user });

    } catch (error) {
        console.error('API 執行錯誤:', error);
        res.status(500).json({ message: error.message });
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