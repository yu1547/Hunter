const mongoose = require('mongoose');
const { Schema } = mongoose;

const itemSchema = new Schema({
  itemId: {
    type: Schema.Types.ObjectId,
    required: true,
    unique: true
  },
  itemFunc: {
    type: String,
    required: true
  },
  itemName: {
    type: String,
    required: true
  },
  itemType: {
    type: Number,
    enum: [0, 1], // 0:素材, 1:消耗物
    required: true
  },
  itemEffect: {
    type: String,
    required: true
  },
  maxStack: {
    type: Number,
    default: 1
  },
  itemMethod: {
    type: String,
    enum: ['npc', 'chest', 'scan', 'randomEvent'],
    required: true
  },
  itemRarity: {
    type: Number,
    min: 1,
    max: 5,
    required: true
  },
  resultId: {
    type: Schema.Types.ObjectId,
    default: null
  }
});

// 明確指定集合名稱為 'items'
const Item = mongoose.model('Item', itemSchema, 'items');

module.exports = Item;
