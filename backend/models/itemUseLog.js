const mongoose = require('mongoose');
//用於道具使用的去重
const schema = new mongoose.Schema({
    userId: { type: mongoose.Schema.Types.ObjectId, index: true },
    itemId: { type: mongoose.Schema.Types.ObjectId, index: true },
    requestId: { type: String, index: true },
    createdAt: { type: Date, default: Date.now }
}, { versionKey: false });
schema.index({ userId: 1, itemId: 1, requestId: 1 }, { unique: true, sparse: true });
module.exports = mongoose.model('ItemUseLog', schema);
