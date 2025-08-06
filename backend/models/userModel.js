const mongoose = require('mongoose');
const { Schema } = mongoose;

// 客服對話紀錄（符合 CSR_input.json 格式）
const CSRChatHistoryItemSchema = new Schema({
  role: { type: String, required: true },      // "user" 或 "LLM"
  content: { type: String, required: true },
  timestamp: { type: String, required: true }
}, { _id: false });

const CSRChatHistorySchema = new Schema({
  message: { type: String, required: true },
  history: { type: [CSRChatHistoryItemSchema], required: true }
}, { _id: false });

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
    haveCheckPlaces: [{
      spotId: {
        type: Schema.Types.ObjectId,
        ref: 'Spot',
        required: true
      },
      isCheck: {
        type: Boolean,
        default: false
      }
    }],
    isLLM: {
      type: Boolean,
      required: false
    }
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
  },
  csrHistory: [CSRChatHistorySchema]
}, { collection: 'users' });

// 明確指定集合名稱為 'users'
const User = mongoose.model('User', userSchema);

module.exports = User;
