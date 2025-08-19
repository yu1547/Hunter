const mongoose = require("mongoose");

const dropRuleSchema = new mongoose.Schema({
    difficulty: { type: Number, required: true },
    dropCountRange: { type: [Number], required: true },
    rarityChances: { type: Map, of: Number, required: true },
    guaranteedRarity: { type: Number, default: null },
});

module.exports = mongoose.model("DropRule", dropRuleSchema, "droprules");
