const mongoose = require('mongoose');
const { Schema } = mongoose;

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
  age: {
    type: String,
    default: ""
  },
  gender: {
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
  lastLogin: {
    type: Date,
    default: Date.now
  },

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
    refreshedAt: {
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
  username: {
    type: String,
    default: "Hunter"
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  spotsScanLogs: {
    type: Object,
    default: {}
  },
  supplyScanLogs: {
    type: Object,
    default: {}
  },
  settings: {
    type: Array,
    default: []
  },
  buff: {
    type: Object,
    default: null
  }
}, { collection: 'users' });


// 明確指定集合名稱為 'users'
const User = mongoose.model('User', userSchema);

module.exports = User;