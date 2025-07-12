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
exports.removeItem = async (req, res) => {
  const { itemId, quantity } = req.body;
  if (!itemId || !quantity || quantity < 1) return res.status(400).json({ message: 'Invalid itemId or quantity' });

  try {
    const inventory = await UserInventory.findOne({ userId: req.params.userId });
    if (!inventory) return res.status(404).json({ message: 'Inventory not found' });

    const item = inventory.items.find(i => i.itemId === itemId);
    if (!item) return res.status(400).json({ message: 'Item not in inventory' });

    item.quantity -= quantity;
    if (item.quantity <= 0) {
      inventory.items = inventory.items.filter(i => i.itemId !== itemId);
    }

    await inventory.save();
    await inventory.populate('items.itemId');
    res.json(inventory);
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

    const itemEntry = inventory.items.find(i => i.itemId === itemId);
    if (!itemEntry || itemEntry.quantity <= 0) return res.status(400).json({ message: 'Item not available' });

    const itemDetails = await Item.findOne({ itemId });
    if (!itemDetails) return res.status(404).json({ message: 'Item does not exist' });
    if (itemDetails.itemType !== 1) return res.status(400).json({ message: 'Item is not consumable' });

    // 可在這裡呼叫實際使用道具邏輯，如回復生命、觸發特效等
    // 例如：triggerItemEffect(userId, itemDetails)

    itemEntry.quantity -= 1;
    if (itemEntry.quantity === 0) {
      inventory.items = inventory.items.filter(i => i.itemId !== itemId);
    }

    await inventory.save();
    await inventory.populate('items.itemId');
    res.json(inventory);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};
