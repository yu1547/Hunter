const User = require('../models/userModel');
const Item = require('../models/itemModel');

// 添加物品到背包
const addItemToBackpack = async (req, res) => {
  try {
    const { itemId, quantity } = req.body;
    
    if (!itemId || !quantity) {
      return res.status(400).json({ message: '物品ID和數量不能為空' });
    }
    
    const user = await User.findById(req.params.id);
    if (!user) {
      return res.status(404).json({ message: '找不到該用戶' });
    }
    
    // 檢查物品是否已在背包中
    const existingItemIndex = user.backpackItems.findIndex(
      item => item.itemId === itemId
    );
    
    if (existingItemIndex > -1) {
      // 如果物品已存在，增加數量
      user.backpackItems[existingItemIndex].quantity += quantity;
    } else {
      // 如果物品不存在，添加新物品
      user.backpackItems.push({ itemId, quantity });
    }
    
    await user.save();
    res.status(200).json(user);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
};

// 合成道具
const craftItem = async (req, res) => {
  try {
    const { id } = req.params; // userId
    const { itemId } = req.body; // the item to use for crafting

    if (!itemId) {
      return res.status(400).json({ message: '請提供要合成的道具 ID' });
    }

    const user = await User.findById(id);
    if (!user) {
      return res.status(404).json({ message: '找不到該用戶' });
    }

    // 找到要用於合成的道具在背包中的位置
    const itemToCraftIndex = user.backpackItems.findIndex(
      item => item.itemId === itemId
    );

    if (itemToCraftIndex === -1) {
      return res.status(404).json({ message: '背包中找不到該道具' });
    }

    const itemInBackpack = user.backpackItems[itemToCraftIndex];

    // 檢查數量是否足夠 (>= 3)
    if (itemInBackpack.quantity < 3) {
      return res.status(400).json({ message: '道具數量不足，無法合成' });
    }

    // 獲取道具的詳細資訊，特別是 resultId
    const sourceItem = await Item.findById(itemId);
    if (!sourceItem) {
      return res.status(404).json({ message: '找不到該道具的定義' });
    }

    if (!sourceItem.resultId) {
      return res.status(400).json({ message: '該道具無法被合成' });
    }

    // 減少合成材料的數量
    itemInBackpack.quantity -= 3;
    // 如果材料用完，就從背包中移除
    if (itemInBackpack.quantity === 0) {
      user.backpackItems.splice(itemToCraftIndex, 1);
    }

    const resultItemId = sourceItem.resultId.toString();

    // 找到合成結果道具在背包中的位置
    const resultItemIndex = user.backpackItems.findIndex(
      item => item.itemId === resultItemId
    );

    if (resultItemIndex > -1) {
      // 如果結果道具已存在，增加數量
      user.backpackItems[resultItemIndex].quantity += 1;
    } else {
      // 如果結果道具不存在，添加新道具
      user.backpackItems.push({ itemId: resultItemId, quantity: 1 });
    }

    await user.save();
    res.status(200).json(user);

  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

module.exports = {
  addItemToBackpack,
  craftItem,
};
