const mongoose = require('mongoose');

const ItemSchema = new mongoose.Schema({
  // 不用寫 itemId 了，直接用 _id 當唯一識別
  itemId: { type: String, required: true, unique: true },
  itemFunc: { type: String, required: true },
  itemName: { type: String, required: true },
  itemType: { type: Number, required: true, enum: [0, 1, 2, 3] },
  itemEffect: { type: String, required: true },
  maxStack: { type: Number, default: 1, min: 1 },
  itemMethod: { type: String, required: true, enum: ['npc', 'chest', 'scan', 'randomEvent'] },
  itemRarity: { type: Number, required: true, min: 1, max: 5 },
  material: { type: String, default: null },
  isBlend: { type: Boolean, default: false },
  itemIcon: { type: String, required: true },
});

module.exports = mongoose.model('Item', ItemSchema);
