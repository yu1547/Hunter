const mongoose = require('mongoose');
const { Schema } = mongoose;

const supplySchema = new Schema({
    name: { type: String, required: true },
    latitude: { type: Number, required: true },
    longitude: { type: Number, required: true },
}, { timestamps: true });

// 明確指定集合為 'supplies'
module.exports = mongoose.model('Supply', supplySchema, 'supplies');
