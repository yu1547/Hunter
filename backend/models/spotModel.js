const mongoose = require('mongoose');
const { Schema } = mongoose;

// 打卡點模型結構
const spotSchema = new Schema({
  spotName: {
    type: String,
    enum: ['anchor', 'ball', 'eagle', 'lovechair', 'moai', 'vending', 'book', 'bookcase', 'freedomship', 'fountain'],
    required: true
  },
  ChName: {
    type: String,
    required: true
  },
  longitude: {
    type: Number,
    required: true
  },
  latitude: {
    type: Number,
    required: true
  }
});

// 明確指定集合名稱為 'spots'
const Spot = mongoose.model('Spot', spotSchema, 'spots');

module.exports = Spot;
