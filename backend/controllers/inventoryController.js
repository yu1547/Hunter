const UserInventory = require('../models/UserInventory');
const Item = require('../models/Item');

// 獲取使用者背包（包含道具詳情）
exports.getInventory = async (req, res) => {
  try {
    let inventory = await UserInventory.findOne({ userId: req.params.userId }).populate('items.itemId');
    if (!inventory) {
      inventory = await UserInventory.create({ userId: req.params.userId, items: [] });
    }
    res.json(inventory);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// 增加道具
exports.addItem = async (req, res) => {
  const { itemId, quantity } = req.body;
  if (!itemId || !quantity || quantity < 1) return res.status(400).json({ message: 'Invalid itemId or quantity' });

  try {
    let inventory = await UserInventory.findOne({ userId: req.params.userId });
    if (!inventory) {
      inventory = await UserInventory.create({ userId: req.params.userId, items: [] });
    }

    const itemIndex = inventory.items.findIndex(i => i.itemId === itemId);
    if (itemIndex !== -1) {
      inventory.items[itemIndex].quantity += quantity;
    } else {
      inventory.items.push({ itemId, quantity });
    }

    await inventory.save();
    await inventory.populate('items.itemId');
    res.json(inventory);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// 移除道具
// 移除消耗品 (如果此函式只用於消耗品)
exports.removeItem = async (req, res) => {
  const { itemId, quantity } = req.body;
  if (!itemId || !quantity || quantity < 1) {
    return res.status(400).json({ message: '無效的道具ID或數量。' });
  }

  try {
    // 先檢查道具類型，如果不是消耗品就直接拒絕
    const itemDetails = await Item.findById(itemId);
    if (!itemDetails) {
        return res.status(404).json({ message: '道具不存在。' });
    }
    if (itemDetails.itemType !== 1) {
      return res.status(400).json({ message: '此道具不是消耗品，無法透過此接口移除。' });
    }

    const inventory = await UserInventory.findOne({ userId: req.params.userId });
    if (!inventory) {
      return res.status(404).json({ message: '找不到使用者背包。' });
    }

    const itemIndex = inventory.items.findIndex(i => i.itemId.toString() === itemId);
    if (itemIndex === -1) {
      return res.status(400).json({ message: '道具不在背包中。' });
    }

    if (inventory.items[itemIndex].quantity < quantity) {
        return res.status(400).json({ message: '移除數量超過背包中現有的道具數量。' });
    }

    inventory.items[itemIndex].quantity -= quantity;
    if (inventory.items[itemIndex].quantity <= 0) {
      inventory.items.splice(itemIndex, 1);
    }

    await inventory.save();
    await inventory.populate('items.itemId');
    res.json({
          message: `成功刪除道具：${itemDetails.itemName}`,
          inventory: inventory // 或者簡寫為 inventory
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// 使用消耗品
exports.useItem = async (req, res) => {
  const { itemId } = req.body;

  try {
    const inventory = await UserInventory.findOne({ userId: req.params.userId });
    if (!inventory) return res.status(404).json({ message: 'Inventory not found' });

    const itemEntry = inventory.items.find(i => i.itemId.toString() === itemId.toString());
    if (!itemEntry || itemEntry.quantity <= 0)
      return res.status(400).json({ message: 'Item not available' });

    // ✅ 正確用 _id 查道具主表
    const itemDetails = await Item.findById(itemId);
    if (!itemDetails) return res.status(404).json({ message: 'Item does not exist' });
    if (itemDetails.itemType !== 1)
      return res.status(400).json({ message: 'Item is not consumable' });

    // ✅ 這裡可加入 itemFunc 對應的使用邏輯（未來進化用）
    // triggerItemEffect(req.params.userId, itemDetails.itemFunc)

    itemEntry.quantity -= 1;
    if (itemEntry.quantity === 0) {
      inventory.items = inventory.items.filter(i => i.itemId.toString() !== itemId.toString());
    }

    await inventory.save();
    await inventory.populate('items.itemId');

    // ✅ 加入一點使用成功的訊息
    res.json({
      message: `成功使用道具：${itemDetails.itemName}`,
      inventory
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

