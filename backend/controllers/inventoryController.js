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
    res.status(500).json({ message: "500 獲取失敗" + error.message });
  }
};

// 增加道具
exports.addItem = async (req, res) => {
  const { itemId, quantity, slot } = req.body;
  if (!itemId || !quantity || quantity < 1) {
    return res.status(400).json({ message: '無效的 itemId 或數量' });
  }
  try {
    let inventory = await UserInventory.findOne({ userId: req.params.userId });
    if (!inventory) {
      inventory = await UserInventory.create({ userId: req.params.userId, items: [] });
    }

    const itemDetails = await Item.findById(itemId);
    if (!itemDetails) {
      return res.status(404).json({ message: '404 物品不存在' });
    }

    const maxStack = itemDetails.maxStack;

    let remainingQuantity = quantity;

    // First, try to fill existing stacks that are not full
    for (let i = 0; i < inventory.items.length && remainingQuantity > 0; i++) {
      let item = inventory.items[i];
      if (item.itemId.toString() === itemId && item.quantity < maxStack) {
        const canAdd = Math.min(remainingQuantity, maxStack - item.quantity);
        item.quantity += canAdd;
        remainingQuantity -= canAdd;
      }
    }

    // Then, add new stacks if there's still remaining quantity
    while (remainingQuantity > 0) {
      const quantityToAdd = Math.min(remainingQuantity, maxStack);
      // If a slot is provided, try to use it. Otherwise, push new item.
      if (slot !== undefined && slot !== null) {
          // Check if slot is already occupied, if so, push new item without slot.
          const existingSlotItem = inventory.items.find(i => i.slot === slot);
          if (!existingSlotItem) {
              inventory.items.push({ itemId, quantity: quantityToAdd, slot });
          } else {
              inventory.items.push({ itemId, quantity: quantityToAdd });
          }
      } else {
          inventory.items.push({ itemId, quantity: quantityToAdd });
      }
      remainingQuantity -= quantityToAdd;
    }

    await inventory.save();
    await inventory.populate('items.itemId');
    res.json(inventory);
  } catch (error) {
    res.status(500).json({ message: "500 增加失敗" + error.message });
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

    let remainingToRemove = quantity;
    inventory.items = inventory.items.map(item => {
        if (item.itemId.toString() === itemId) {
            const removed = Math.min(item.quantity, remainingToRemove);
            item.quantity -= removed;
            remainingToRemove -= removed;
        }
        return item;
    }).filter(item => item.quantity > 0); // Remove items with quantity 0 or less

    if (remainingToRemove > 0) {
      return res.status(400).json({ message: '嘗試移除的數量超過背包中現有的道具數量。' });
    }

    await inventory.save();
    await inventory.populate('items.itemId');
    res.json({
        message: `成功刪除道具：${itemDetails.itemName}`,
        inventory: inventory
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
    if (!inventory) return res.status(404).json({ message: '找不到消耗品' });

    const itemIndex = inventory.items.findIndex(i => i.itemId.toString() === itemId.toString());
    if (itemIndex === -1 || inventory.items[itemIndex].quantity <= 0)
      return res.status(400).json({ message: 'Item not available' });

    // ✅ 正確用 _id 查道具主表
    const itemDetails = await Item.findById(itemId);
    if (!itemDetails) return res.status(404).json({ message: '物品不存在' });
    if (itemDetails.itemType !== 1)
      return res.status(400).json({ message: '此物品不可消耗' });

    // Decrease quantity
    inventory.items[itemIndex].quantity -= 1;

    // Remove item if quantity drops to 0
    if (inventory.items[itemIndex].quantity === 0) {
      inventory.items.splice(itemIndex, 1);
    }

    await inventory.save();
    await inventory.populate('items.itemId');

    res.json({
      message: `成功使用道具：${itemDetails.itemName}`,
      inventory
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

exports.craftItem = async (req, res) => {
  const { userId, itemId } = req.params;

  try {
    const inventory = await UserInventory.findOne({ userId });
    const targetItem = await Item.findById(itemId);

    if (!targetItem.isBlend || !targetItem.material) {
      return res.status(400).json({ message: "該物品不可合成" });
    }

    const materials = JSON.parse(targetItem.material); // 例如 { itemId1: 2, itemId2: 3 }

    // 檢查是否有足夠材料
    for (const [matId, requiredQty] of Object.entries(materials)) {
      const invEntry = inventory.items.find(i => i.itemId.toString() === matId);
      if (!invEntry || invEntry.quantity < requiredQty) {
        return res.status(400).json({ message: "材料不足" });
      }
    }

    // 扣材料 & 加入合成品
    for (const [matId, requiredQty] of Object.entries(materials)) {
      const invEntry = inventory.items.find(i => i.itemId.toString() === matId);
      invEntry.quantity -= requiredQty;
    }

    const existing = inventory.items.find(i => i.itemId.toString() === itemId);
    if (existing) {
      existing.quantity += 1;
    } else {
      inventory.items.push({ itemId: targetItem._id, quantity: 1 });
    }

    await inventory.save();
    res.json({ message: "合成成功" });
  } catch (e) {
    res.status(500).json({ message: "伺服器錯誤", error: e.message });
  }
};