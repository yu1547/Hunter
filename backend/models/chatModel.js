const mongoose = require('mongoose');
const { Schema } = mongoose;

// 客服對話紀錄（符合 CSR_input.json 格式）
const CSRChatHistoryItemSchema = new Schema({
  role: { type: String, required: true },
  content: { type: String, required: true },
  timestamp: { type: String, required: true }
}, { _id: false });

const CSRChatHistorySchema = new Schema({
  userId: {
    type: Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  message: { type: String, required: true },
  history: { type: [CSRChatHistoryItemSchema], required: true }
}, { _id: false });

const chatSchema = new Schema({
  csrHistory: CSRChatHistorySchema
}, { collection: 'chats' });

const Chat = mongoose.model('Chat', chatSchema);

module.exports = Chat;