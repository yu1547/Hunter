const mongoose = require('mongoose');
const { Schema } = mongoose;

// 子文檔：獎勵道具
const rewardItemSchema = new Schema({
  itemId: {
    type: Schema.Types.ObjectId,
    ref: 'Item', // 建議關聯到 Item 模型
    required: true
  },
  quantity: {
    type: Number,
    required: true,
    min: 1
  }
}, { _id: false }); // 不為子文檔生成 _id

// 任務模型結構
const taskSchema = new Schema({
  taskName: {
    type: String,
    required: true
  },
  taskDescription: {
    type: String,
    default: null
  },
  taskDifficulty: {
    type: String,
    enum: ['easy', 'normal', 'hard'],
    required: true
  },
  taskTarget: {
    type: String,
    required: true
  },
  checkPlaces: [
    {
      spotId: { type: Schema.Types.ObjectId, ref: 'Spot' }
    }
  ],
  taskDuration: { // 任務時長 (秒)
    type: Number,
    default: null
  },
  rewardItems: {
    type: [rewardItemSchema],
    default: []
  },
  rewardScore: {
    type: Number,
    default: 0
  },
  isLLM: {
    type: Boolean,
    default: false
  }
}, {
  timestamps: true // 自動添加 createdAt 和 updatedAt
});

// 明確指定集合名稱為 'tasks'
const Task = mongoose.model('Task', taskSchema, 'tasks');

module.exports = Task;
