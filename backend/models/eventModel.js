const mongoose = require('mongoose');

const eventSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
  type: {
    type: String,
    enum: ['daily', 'permanent'], // 區分每日事件和永久事件
    required: true,
  },
  mechanics: {
    // 儲存事件特有的邏輯和參數
    rewards: [
      {
        itemId: {
          type: mongoose.Schema.Types.ObjectId,
          ref: 'Item',
        },
        quantity: Number,
        points: Number,
        title: String,
      },
    ],
    // 額外的事件參數，例如：
    // - 史萊姆事件： 'attackMultiplier'
    // - 寶箱事件：'requiredItem'
    // - 神秘商人：'exchangeOptions'
    additionalInfo: {
      type: Object,
      default: {},
    },
  },
  // 每日事件專用
  lastTriggered: {
    type: Date,
  },
  location: {
    type: Object, // 儲存每日事件的位置座標 { lat, lng }
  },
});

module.exports = mongoose.model('Event', eventSchema);