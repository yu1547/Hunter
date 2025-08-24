// controllers/eventController.js
const Event = require('../models/eventModel');
const User = require('../models/userModel');
const Item = require('../models/itemModel');
const Spot = require('../models/spotModel');

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

        let user = null;
        // 優先使用 userId 查詢
        if (userId) {
            user = await User.findById(userId);
        }

        // 如果沒有 userId 或找不到使用者，則使用 username 查詢
        if (!user && username) {
            user = await User.findOne({ username: username })
        }

        if (!user) {
            return res.status(404).json({ message: '找不到使用者' });
        }


        let finalRewards = event.rewards;
        let consumeItems = event.consume;

        // 處理有選項的事件（如神秘商人、古樹）
        if (selectedOption) {
            const option = event.options.find(opt => opt.text === selectedOption);
            if (!option) {
                return res.status(400).json({ message: '無效的選項' });
            }
            // 檢查使用者是否有足夠的道具進行交易
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

        // 處理打扁史萊姆的小遊戲結果
        if (event.name === '打扁史萊姆' && gameResult) {
            // 根據 gameResult (例如：點擊次數) 計算獎勵
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

        const updatedUser = await updateUserRewards(user._id, finalRewards, consumeItems);

        res.status(200).json({ message: '事件完成，獎勵已發放', rewards: finalRewards, updatedUser });

    } catch (error) {
        console.error('API 執行錯誤:', error);
        res.status(500).json({ message: error.message });
    }
};


module.exports = {
    completeEvent, 
    refreshDailyEvents,
};