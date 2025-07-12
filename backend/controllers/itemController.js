const Item = require('../models/Item');

// 建立新道具
exports.createItem = async (req, res) => {
  try {
    const newItem = new Item(req.body);
    await newItem.save();
    res.status(201).json(newItem);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
};

// 取得所有道具（支援過濾）
exports.getAllItems = async (req, res) => {
  try {
    const filter = {};
    if (req.query.itemType) filter.itemType = req.query.itemType;
    if (req.query.itemRarity) filter.itemRarity = req.query.itemRarity;

    const items = await Item.find(filter);
    res.json(items);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// 取得單一指定道具
exports.getItemById = async (req, res) => {
  try {
    const item = await Item.findOne({ itemId: req.params.itemId });
    if (!item) return res.status(404).json({ message: 'Item not found' });
    res.json(item);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// 更新道具資訊
exports.updateItem = async (req, res) => {
  try {
    const updated = await Item.findOneAndUpdate(
      { itemId: req.params.itemId },
      req.body,
      { new: true }
    );
    if (!updated) return res.status(404).json({ message: 'Item not found' });
    res.json(updated);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
};

// 刪除道具
exports.deleteItem = async (req, res) => {
  try {
    const deleted = await Item.findOneAndDelete({ itemId: req.params.itemId });
    if (!deleted) return res.status(404).json({ message: 'Item not found' });
    res.json({ message: 'Item deleted' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};
