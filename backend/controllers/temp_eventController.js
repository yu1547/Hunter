// controllers/eventController.js
const Event = require('../models/eventModel');
const User = require('../models/userModel');
const Item = require('../models/itemModel');
const Spot = require('../models/spotModel');

// 輔助函數：計算兩點間距離 (Haversine Formula)
const getDistance = (lat1, lon1, lat2, lon2) => {
    const R = 6371e3; // metres
    const φ1 = lat1 * Math.PI / 180; // φ, λ in radians
    const φ2 = lat2 * Math.PI / 180;
    const Δφ = (lat2 - lat1) * Math.PI / 180;
    const Δλ = (lon2 - lon1) * Math.PI / 180;

    const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
              Math.cos(φ1) * Math.cos(φ2) *
              Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    const d = R * c; // in metres
    return d;
};

// 輔助函數：根據獎勵資訊更新使用者資料
const updateUserRewards = async (userId, rewards, consume) => {
    const user = await User.findById(userId);
    if (!user) {
        throw new Error('User not found');
    }

    if (!user.items) {
        user.items = [];
    }

    // 檢查 rewards 是否存在
    if (rewards) {
        if (rewards.points) {
            user.points += rewards.points;
        }

        if (rewards.title) {
            user.titles.push(rewards.title);
        }

        // 處理道具獲得
        if (rewards.items) {
            for (const rewardItem of rewards.items) {
                // 將 findById 修正為 .find()
                const userItem = user.items.find(item => item.itemId.toString() === rewardItem.itemId.toString());
                if (userItem) {
                    userItem.quantity += rewardItem.quantity;
                } else {
                    user.items.push({ itemId: rewardItem.itemId, quantity: rewardItem.quantity });
                }
            }
        }
    }

    // 處理道具消耗
    if (consume && consume.items) {
        for (const consumeItem of consume.items) {
            // 將 findById 修正為 .find()
            const userItem = user.items.find(item => item.itemId.toString() === consumeItem.itemId.toString());
            if (userItem) {
                userItem.quantity -= consumeItem.quantity;
                if (userItem.quantity <= 0) {
                    user.items = user.items.filter(item => item.itemId.toString() !== consumeItem.itemId.toString());
                }
            }
        }
    }

    await user.save();
    return user;
};

// 每日刷新事件位置的邏輯
exports.refreshDailyEvents = async (req, res) => {
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

// 觸發事件的 API，根據使用者位置與事件地點比對
exports.triggerEvent = async (req, res) => {
    const { eventId } = req.params;
    const { userId, userLatitude, userLongitude } = req.body;

    try {
        const event = await Event.findById(eventId).populate('spotId');
        if (!event) {
            return res.status(404).json({ message: '找不到此事件' });
        }

        if (!event.spotId) {
            return res.status(400).json({ message: '此事件沒有地點資訊' });
        }

        const eventSpot = event.spotId;
        const distance = getDistance(userLatitude, userLongitude, eventSpot.latitude, eventSpot.longitude);

        // 設定觸發距離為 50 公尺
        if (distance <= 50) {
            res.status(200).json({ message: '觸發事件成功', eventData: event });
        } else {
            res.status(400).json({ message: '距離事件地點太遠' });
        }
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// 完成事件，並發放獎勵
exports.completeEvent = async (req, res) => {
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
            finalRewards = { points: 5, items: [slimeRewardItem] };
        }

        const updatedUser = await updateUserRewards(user._id, finalRewards, consumeItems);

        res.status(200).json({ message: '事件完成，獎勵已發放', rewards: finalRewards, updatedUser });

    } catch (error) {
        console.error('API 執行錯誤:', error);
        res.status(500).json({ message: error.message });
    }
};