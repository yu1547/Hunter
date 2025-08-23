// backend/models/rankModel.js
const mongoose = require('mongoose');

const rankSchema = new mongoose.Schema(
  {
    userId: { type: String, required: true, index: true, unique: true }, // 建議唯一，避免重複
    username: { type: String, required: true },
    userImg: { type: String, default: null },
    score: { type: Number, required: true, default: 0, index: true },
  },
  { timestamps: true }
);

// 第三個參數 'ranks' 指定 collection 名稱
module.exports = mongoose.model('Rank', rankSchema, 'ranks');
