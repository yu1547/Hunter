const mongoose = require('mongoose');

const UserInventorySchema = new mongoose.Schema({
  userId: { type: String, required: true, unique: true },
  items: [
    {
      itemId: { type: String, required: true, ref: 'Item' },
      quantity: { type: Number, required: true, min: 1 },
      slot: { type: Number, default: null },
    },
  ],
});

module.exports = mongoose.model('UserInventory', UserInventorySchema);
