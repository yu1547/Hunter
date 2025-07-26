// backend/models/rankModel.js

const mongoose = require('mongoose');

const rankSchema = mongoose.Schema({
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

module.exports = mongoose.model('Rank', rankSchema,'ranks');
