//調落機制
const Items = require("../models/itemModel");
const DropRules = require("../models/dropRulesModel");
const DropPools = require("../models/dropPoolsModel");
const { addItemsToBackpack } = require("../services/backpackService");//添加到背包
console.log("✅ DropRules model name:", DropRules.modelName);
console.log("✅ DropRules collection name:", DropRules.collection.name);

function getRandomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function getRarityByChance(rarityChances) {
    const entries = Object.entries(rarityChances);
    const total = entries.reduce((sum, [, p]) => sum + p, 0);
    const roll = Math.random() * total;
    let acc = 0;
    for (let [rarity, chance] of entries) {
        acc += chance;
        if (roll <= acc) return parseInt(rarity);
    }
}

async function getRandomItemFromPool(rarity) {
    const pool = await DropPools.findOne({ rarity });
    if (!pool || pool.itemIds.length === 0) return null;
    const randIndex = getRandomInt(0, pool.itemIds.length - 1);
    return pool.itemIds[randIndex];
}

async function getItemNameById(itemId) {
    const item = await Items.findById(itemId);
    return item ? item.itemName : "未知道具";
}

async function generateDropForUser(userId, difficulty) {

    console.log(`\nuserID ${userId} `);
    const rule = await DropRules.findOne({ difficulty: Number(difficulty) });


    if (!rule) throw new Error("Invalid difficulty");

    const { dropCountRange, rarityChances, guaranteedRarity } = rule;
    const totalDropCount = getRandomInt(dropCountRange[0], dropCountRange[1]);
    let remainingDropCount = totalDropCount;

    const drops = [];
    const fixed = [];
    const randoms = [];

    if (guaranteedRarity) {
        const itemId = await getRandomItemFromPool(guaranteedRarity);
        if (itemId) {
            fixed.push(itemId);
            drops.push(itemId);
            remainingDropCount--;
        }
    }

    while (remainingDropCount-- > 0) {
        const rarity = getRarityByChance(Object.fromEntries(rarityChances));
        const itemId = await getRandomItemFromPool(rarity);
        if (itemId) {
            randoms.push(itemId);
            drops.push(itemId);
        }
    }


    // 添加到背包
    await addItemsToBackpack(userId, drops); // drops 是 itemId 的陣列

    const dropNames = await Promise.all(drops.map(getItemNameById));

    console.log(`\n本次任務難度 ${difficulty} 掉落：`);
    if (fixed.length > 0) {
        const fixedNames = await Promise.all(fixed.map(getItemNameById));
        console.log(`固定掉落(保底)：${fixedNames.join(", ")}`);
    }
    if (randoms.length > 0) {
        const randomNames = await Promise.all(randoms.map(getItemNameById));
        console.log(`隨機掉落：${randomNames.join(", ")}`);
    }



    return dropNames;
}

module.exports = { generateDropForUser };
