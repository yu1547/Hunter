// models/dropPoolsModel.js
const mongoose = require("mongoose");

const dropPoolSchema = new mongoose.Schema({
    rarity: { type: Number, required: true },
    itemIds: [{ type: mongoose.Schema.Types.ObjectId, ref: "items" }]
});

module.exports = mongoose.model("DropPool", dropPoolSchema ,"droppools");

