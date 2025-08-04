// models/eventModel.js
const mongoose = require('mongoose');

const rewardSchema = new mongoose.Schema({
    points: { type: Number, default: 0 },
    items: [{
        itemId: { type: mongoose.Schema.Types.ObjectId, ref: 'Item' },
        quantity: { type: Number, default: 1 }
    }],
    title: { type: String }
}, { _id: false });

const optionSchema = new mongoose.Schema({
    text: { type: String, required: true },
    rewards: { type: rewardSchema, required: true },
    consume: { type: rewardSchema }
}, { _id: false });

const eventSchema = new mongoose.Schema({
    name: {
        type: String,
        required: true,
        trim: true,
    },
    description: {
        type: String,
        required: true,
    },
    type: {
        type: String,
        enum: ['daily', 'puzzle', 'game', 'chest', 'bless'],
        required: true,
    },
    spotId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Spot',
        required: false,
    },
    options: [optionSchema],
    rewards: rewardSchema,
    consume: rewardSchema
}, { timestamps: true });

const Event = mongoose.model('Event', eventSchema);
module.exports = Event;