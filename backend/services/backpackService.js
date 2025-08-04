const mongoose = require("mongoose");
const Users = require("../models/userModel");

async function addItemsToBackpack(userId, itemsToAdd) {
    const user = await Users.findById(userId);
    if (!user) throw new Error("找不到使用者");

    // 初始化背包欄位（如果尚未存在）
    if (!user.backpackItems) user.backpackItems = [];

    for (const item of itemsToAdd) {
        const itemId = new mongoose.Types.ObjectId(item.itemId);
        const quantity = item.quantity;

        const existing = user.backpackItems.find(entry => entry.itemId.equals(itemId));

        if (existing) {
            existing.quantity += quantity;
        } else {
            user.backpackItems.push({
                itemId: itemId,
                quantity: quantity
            });
        }
    }

    await user.save();
    console.log("背包更新完成！");
}

module.exports = { addItemsToBackpack };
