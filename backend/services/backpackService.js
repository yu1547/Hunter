const mongoose = require("mongoose");
const Users = require("../models/userModel");

async function addItemsToBackpack(userId, itemIdsArray) {
    const user = await Users.findById(userId);
    if (!user) throw new Error("找不到使用者");

    // 初始化背包欄位（如果尚未存在）
    if (!user.backpackItems) user.backpackItems = [];

    // 將 itemIdsArray 統一轉為 ObjectId
    const objectIds = itemIdsArray.map(id => new mongoose.Types.ObjectId(id));

    for (const itemId of objectIds) {
        const existing = user.backpackItems.find(entry => entry.itemId.equals(itemId));

        if (existing) {
            existing.quantity += 1;
        } else {
            user.backpackItems.push({
                itemId: itemId,
                quantity: 1
            });
        }
    }

    await user.save();
    console.log("背包更新完成！");
}

module.exports = { addItemsToBackpack };
