const Event = require('../models/eventModel');
const User = require('../models/userModel');
const Item = require('../models/itemModel');
const { calculateDistance } = require('../utils/locationUtils'); // 假設你有一個處理位置的工具
const mongoose = require('mongoose');

// 定義每日事件的 ID，方便查詢
const MYSTERIOUS_MERCHANT_ID = 'your_merchant_event_id';
const STONE_PILE_ID = 'your_stone_pile_event_id';

// 獲取所有可觸發的每日事件
exports.getDailyEvents = async (req, res) => {
  try {
    const { userId } = req.body; // 假設從請求中獲取使用者 ID
    const user = await User.findById(userId);

    // 獲取神秘商人事件
    const merchantEvent = await Event.findById(MYSTERIOUS_MERCHANT_ID);
    // 獲取石堆下的碎片事件
    const stonePileEvent = await Event.findById(STONE_PILE_ID);

    const dailyEvents = [];

    // 檢查石堆事件是否已觸發
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const userEventStatus = user.eventsStatus.find(e => e.eventId.toString() === STONE_PILE_ID);
    const canTriggerStonePile = !userEventStatus || userEventStatus.lastTriggered < today;

    if (canTriggerStonePile) {
      dailyEvents.push(stonePileEvent);
    }

    // 神秘商人位置每日刷新邏輯
    // 如果商人事件的 location 是過期的，則刷新它
    const lastLocationUpdate = merchantEvent.additionalInfo.lastLocationUpdate;
    if (!lastLocationUpdate || new Date(lastLocationUpdate).getDate() !== today.getDate()) {
        merchantEvent.location = {
            lat: Math.random() * (25.105 - 25.01) + 25.01, // 假設隨機範圍在台北市
            lng: Math.random() * (121.6 - 121.45) + 121.45,
        };
        merchantEvent.additionalInfo.lastLocationUpdate = new Date();
        await merchantEvent.save();
    }

    dailyEvents.push(merchantEvent);

    res.status(200).json({ success: true, dailyEvents });
  } catch (error) {
    res.status(500).json({ success: false, message: '獲取每日事件失敗', error: error.message });
  }
};

// 處理神秘商人的交易
exports.handleMerchantExchange = async (req, res) => {
  const { userId, option } = req.body;
  try {
    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ success: false, message: '找不到使用者' });
    }

    let requiredItemId, requiredQuantity, rewardItemId, rewardQuantity;
    let message = '交易成功！';

    // 根據選項設定交易物品
    if (option === 'bronzeToSilver') {
      const bronzeKeyFragment = await Item.findOne({ name: '銅鑰匙碎片' });
      const silverKeyFragment = await Item.findOne({ name: '銀鑰匙碎片' });
      requiredItemId = bronzeKeyFragment._id;
      requiredQuantity = 3;
      rewardItemId = silverKeyFragment._id;
      rewardQuantity = 1;
    } else if (option === 'silverToGold') {
      const silverKeyFragment = await Item.findOne({ name: '銀鑰匙碎片' });
      const goldKeyFragment = await Item.findOne({ name: '金鑰匙碎片' });
      requiredItemId = silverKeyFragment._id;
      requiredQuantity = 3;
      rewardItemId = goldKeyFragment._id;
      rewardQuantity = 1;
    } else {
      return res.status(400).json({ success: false, message: '無效的交易選項' });
    }

    const hasRequiredItems = user.inventory.some(item =>
      item.itemId.equals(requiredItemId) && item.quantity >= requiredQuantity
    );

    if (!hasRequiredItems) {
      return res.status(400).json({ success: false, message: '你的物品不足以進行交易' });
    }

    // 執行交易：扣除材料，增加獎勵
    const requiredItemIndex = user.inventory.findIndex(item => item.itemId.equals(requiredItemId));
    user.inventory[requiredItemIndex].quantity -= requiredQuantity;

    const rewardItemIndex = user.inventory.findIndex(item => item.itemId.equals(rewardItemId));
    if (rewardItemIndex !== -1) {
      user.inventory[rewardItemIndex].quantity += rewardQuantity;
    } else {
      user.inventory.push({ itemId: rewardItemId, quantity: rewardQuantity });
    }

    // 移除數量為 0 的物品
    user.inventory = user.inventory.filter(item => item.quantity > 0);

    user.points += 1;
    await user.save();

    res.status(200).json({ success: true, message, user });

  } catch (error) {
    res.status(500).json({ success: false, message: '交易失敗', error: error.message });
  }
};

// 處理石堆下的碎片事件
exports.handleStonePileEvent = async (req, res) => {
  const { userId } = req.body;
  try {
    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ success: false, message: '找不到使用者' });
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // 檢查今日是否已觸發過此事件
    const eventStatusIndex = user.eventsStatus.findIndex(e => e.eventId.toString() === STONE_PILE_ID);
    if (eventStatusIndex !== -1 && user.eventsStatus[eventStatusIndex].lastTriggered >= today) {
      return res.status(400).json({ success: false, message: '你今天已經發現過石堆下的碎片了！' });
    }

    const bronzeKeyFragment = await Item.findOne({ name: '銅鑰匙碎片' });
    const rewardItemId = bronzeKeyFragment._id;
    const rewardQuantity = 2;

    // 增加獎勵
    const rewardItemIndex = user.inventory.findIndex(item => item.itemId.equals(rewardItemId));
    if (rewardItemIndex !== -1) {
      user.inventory[rewardItemIndex].quantity += rewardQuantity;
    } else {
      user.inventory.push({ itemId: rewardItemId, quantity: rewardQuantity });
    }

    // 更新事件觸發狀態
    if (eventStatusIndex !== -1) {
      user.eventsStatus[eventStatusIndex].lastTriggered = new Date();
    } else {
      user.eventsStatus.push({ eventId: STONE_PILE_ID, lastTriggered: new Date() });
    }

    await user.save();

    res.status(200).json({ success: true, message: '你獲得了2個銅鑰匙碎片！', user });

  } catch (error) {
    res.status(500).json({ success: false, message: '事件觸發失敗', error: error.message });
  }
};

// 處理史萊姆攻擊事件
exports.handleSlimeAttack = async (req, res) => {
  const { userId, totalDamage, usedTorch } = req.body;
  try {
    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ success: false, message: '找不到使用者' });
    }

    // 獎勵積分
    user.points += 5;

    // 根據傷害值發放史萊姆黏液
    const ordinarySlimeMucus = await Item.findOne({ name: '普通的史萊姆黏液' });
    const stickySlimeMucus = await Item.findOne({ name: '黏稠的史萊姆黏液' });
    let rewardItems = [];

    if (totalDamage >= 200) {
      const stickyQuantity = Math.floor(totalDamage / 50);
      rewardItems.push({ itemId: stickySlimeMucus._id, quantity: stickyQuantity });
    } else if (totalDamage >= 150) {
      rewardItems.push({ itemId: ordinarySlimeMucus._id, quantity: 3 });
    } else if (totalDamage >= 100) {
      rewardItems.push({ itemId: ordinarySlimeMucus._id, quantity: 2 });
    } else {
      rewardItems.push({ itemId: ordinarySlimeMucus._id, quantity: 1 });
    }

    // 更新使用者背包
    for (const reward of rewardItems) {
        const itemIndex = user.inventory.findIndex(item => item.itemId.equals(reward.itemId));
        if (itemIndex !== -1) {
            user.inventory[itemIndex].quantity += reward.quantity;
        } else {
            user.inventory.push({ itemId: reward.itemId, quantity: reward.quantity });
        }
    }

    // 如果使用了火把，扣除一個火把
    if (usedTorch) {
        const torch = await Item.findOne({ name: '火把' });
        const torchIndex = user.inventory.findIndex(item => item.itemId.equals(torch._id));
        if (torchIndex !== -1) {
            user.inventory[torchIndex].quantity -= 1;
            user.inventory = user.inventory.filter(item => item.quantity > 0);
        }
    }

    await user.save();

    res.status(200).json({ success: true, message: '史萊姆被擊敗了！', user, rewards: rewardItems });

  } catch (error) {
    res.status(500).json({ success: false, message: '史萊姆事件處理失敗', error: error.message });
  }
};

// 處理開寶箱事件
exports.handleTreasureBox = async (req, res) => {
  const { userId, keyId } = req.body;
  try {
    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ success: false, message: '找不到使用者' });
    }

    // 檢查是否有對應的鑰匙
    const key = await Item.findById(keyId);
    if (!key) {
        return res.status(404).json({ success: false, message: '無效的鑰匙' });
    }

    const userHasKey = user.inventory.some(item => item.itemId.equals(keyId) && item.quantity >= 1);
    if (!userHasKey) {
        return res.status(400).json({ success: false, message: '你沒有這把鑰匙' });
    }

    // 扣除鑰匙
    const keyIndex = user.inventory.findIndex(item => item.itemId.equals(keyId));
    user.inventory[keyIndex].quantity -= 1;
    user.inventory = user.inventory.filter(item => item.quantity > 0);

    // 根據鑰匙稀有度給予獎勵
    let points = 0;
    const randomItems = [];

    if (key.name.includes('銅鑰匙')) {
      points = 15;
      // 隨機獲得普通道具
    } else if (key.name.includes('銀鑰匙')) {
      points = 25;
      // 隨機獲得較好道具
    } else if (key.name.includes('金鑰匙')) {
      points = 40;
      // 隨機獲得稀有道具
    }

    user.points += points;

    // 這裡可以加入更複雜的隨機物品掉落邏輯
    const allItems = await Item.find({});
    // 隨機從 allItems 中選出 1~3 個道具
    const shuffledItems = allItems.sort(() => 0.5 - Math.random());
    const randomCount = Math.floor(Math.random() * 3) + 1;

    for (let i = 0; i < randomCount; i++) {
        const item = shuffledItems[i];
        const quantity = Math.floor(Math.random() * 3) + 1;
        randomItems.push({ itemId: item._id, quantity });
    }

    // 更新使用者背包
    for (const reward of randomItems) {
        const itemIndex = user.inventory.findIndex(item => item.itemId.equals(reward.itemId));
        if (itemIndex !== -1) {
            user.inventory[itemIndex].quantity += reward.quantity;
        } else {
            user.inventory.push({ itemId: reward.itemId, quantity: reward.quantity });
        }
    }

    await user.save();

    res.status(200).json({ success: true, message: '寶箱已開啟！', user, rewards: { points, items: randomItems } });

  } catch (error) {
    res.status(500).json({ success: false, message: '開寶箱失敗', error: error.message });
  }
};

// 處理古樹的祝福事件
exports.handleAncientTreeBlessing = async (req, res) => {
  const { userId, option } = req.body;
  try {
    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ success: false, message: '找不到使用者' });
    }

    let requiredItemId, requiredQuantity, rewardItemId;

    if (option === 'ordinary') {
      const ordinarySlimeMucus = await Item.findOne({ name: '普通的史萊姆黏液' });
      const bronzeKeyFragment = await Item.findOne({ name: '銅鑰匙碎片' });
      requiredItemId = ordinarySlimeMucus._id;
      requiredQuantity = 10;
      rewardItemId = bronzeKeyFragment._id;
    } else if (option === 'sticky') {
      const stickySlimeMucus = await Item.findOne({ name: '黏稠的史萊姆黏液' });
      const silverKeyFragment = await Item.findOne({ name: '銀鑰匙碎片' });
      requiredItemId = stickySlimeMucus._id;
      requiredQuantity = 10;
      rewardItemId = silverKeyFragment._id;
    } else {
      return res.status(400).json({ success: false, message: '無效的選項' });
    }

    const hasRequiredItems = user.inventory.some(item =>
      item.itemId.equals(requiredItemId) && item.quantity >= requiredQuantity
    );

    if (!hasRequiredItems) {
      return res.status(400).json({ success: false, message: '你沒有足夠的材料' });
    }

    // 扣除材料
    const requiredItemIndex = user.inventory.findIndex(item => item.itemId.equals(requiredItemId));
    user.inventory[requiredItemIndex].quantity -= requiredQuantity;
    user.inventory = user.inventory.filter(item => item.quantity > 0);

    // 增加獎勵：古樹的枝幹
    const ancientTreeBranch = await Item.findOne({ name: '古樹的枝幹' });
    const branchIndex = user.inventory.findIndex(item => item.itemId.equals(ancientTreeBranch._id));
    if (branchIndex !== -1) {
        user.inventory[branchIndex].quantity += 1;
    } else {
        user.inventory.push({ itemId: ancientTreeBranch._id, quantity: 1 });
    }

    await user.save();

    res.status(200).json({ success: true, message: '你獲得了古樹的枝幹！', user });

  } catch (error) {
    res.status(500).json({ success: false, message: '古樹祝福失敗', error: error.message });
  }
};