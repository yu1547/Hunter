const mongoose = require('mongoose');
const { Schema } = mongoose;

// 定義使用者模型結構
const userSchema = new Schema({
  // uid 由 MongoDB 自動生成的 _id 提供
  backpackItems: [{
    itemId: {
      type: String,
      required: true
    },
    quantity: {
      type: Number,
      required: true,
      min: 1
    }
  }],
  missions: [{
    taskId: {
      type: String,
      required: true
    },
    state: {
      type: String,
      enum: ['available', 'in_progress', 'completed', 'claimed', 'declined', 'deleted'],
      required: true
    },
    acceptedAt: {
      type: Date,
      default: null
    },
    expiresAt: {
      type: Date,
      default: null
    },
    refreshedAt: { // 拒絕任務後，可刷新的時間
      type: Date,
      default: null
    },
    checkPlaces: [{
      spotId: {
        type: String,
        required: true
      },
      isCheck: {
        type: Boolean,
        default: false
      }
    }]
  }],
  settings: {
    music: {
      type: Boolean,
      default: true  // 預設開啟音效
    },
    notification: {
      type: Boolean,
      default: true  // 預設開啟推播通知
    },
    language: {
      type: String,
      default: 'zh-TW'  // 預設語言
    }
  },
  // 可加入其他用戶屬性，如用戶名稱、等級等
  username: {
    type: String,
    default: "Hunter"
  },
  createdAt: {
    type: Date,
    default: Date.now
  }
}, { collection: 'users' });

// 明確指定集合名稱為 'users'
const User = mongoose.model('User', userSchema);

module.exports = User;
