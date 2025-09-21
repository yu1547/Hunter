const mongoose = require('mongoose');
const { Schema } = mongoose;

// // 定義補給站領取紀錄schema
// const supplyLogSchema = new Schema({
//   spotId: {
//     type: String, // 字串存 supplyId(key)
//     required: true
//   },
//   nextClaimTime: {
//     type: Date,
//     required: true
//   }
// }, { _id: false }); // 不要自動生成 _id

// 定義使用者模型結構
const userSchema = new Schema({
  email: {
    type: String,
    required: true,
    unique: true
  },
  displayName: {
    type: String,
    default: ""
  },
  photoURL: {
    type: String,
    default: ""
  },
  role: {
    type: String,
    default: "player"
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  lastLogin: {
    type: Date,
    default: Date.now
  },
  gender: {
    type: String,
    enum: ['男', '女', '不透露'],
    default: '不透露'
  },
  age: {
    type: String,
    default: "18"
  },
  username: {
    type: String,
    default: ""
  },
  backpackItems: [{
    _id: false, //不會自動生成 _id
    itemId: {
      type: mongoose.Schema.Types.ObjectId,
      required: true
    },
    quantity: {
      type: Number,
      required: true,
      min: 1
    }
  }],
  missions: [{
    _id: false,
    taskId: {
      type: mongoose.Schema.Types.ObjectId,
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
    refreshedAt: { 
      type: Date,
      default: null
    },
    haveCheckPlaces: [{
      _id: false,
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
  spotsScanLogs: {
    type: Map,
    of: Boolean,
    default: {}
  },
  supplyScanLogs: {
    type: Map,
    of: Date,
    default: {}
  },
  settings: {
      music: {
        type: Boolean,
        default: true
      },
      notification: {
        type: Boolean,
        default: true
      },
      language: {
        type: String,
        default: 'zh-TW'
      }
    },
  buff: {
    type: Object,
    default: null
  }
}, { collection: 'users' });

// 明確指定集合名稱為 'users'
const User = mongoose.model('User', userSchema);

module.exports = User;
