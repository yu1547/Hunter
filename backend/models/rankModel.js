// backend/models/rankModel.js

const mongoose = require('mongoose');

// 這個 Schema 定義了排行榜中每個條目的結構
// 它不會被直接用於創建一個 MongoDB Collection
// 而是作為一種數據格式的約定，供控制器在查詢 User 數據後進行轉換
const rankItemSchema = mongoose.Schema({
      userId: {
        type: String,
        required: true,
      },
      username: {
        type: String,
        required: true,
      },
      userImg: {
        type: String,
        default: null,
      },
      score: {
        type: Number,
        required: true,
        default: 0,
      },
    }
);

module.exports = mongoose.model('RankItem', rankItemSchema);
