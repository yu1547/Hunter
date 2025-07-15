const items = require("../mock/data/items");
const dropRules = require("../mock/data/dropRules");
const dropPools = require("../mock/data/dropPools");

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

function getRandomItemFromPool(poolType, rarity) {
    const pool = dropPools.find(p => p.poolType === poolType && p.rarity === rarity);
    if (!pool || pool.itemIds.length === 0) return null;
    const randIndex = getRandomInt(0, pool.itemIds.length - 1);
    return pool.itemIds[randIndex];
}

function getItemNameById(itemId) {
    const item = items.find(i => i.itemId === itemId);
    return item ? `${item.itemName}(稀有度${item.itemRarity}, ${item.itemId})` : itemId;
}


function generateDropForUser(userId, difficulty,selectedPoolType) {
    const rule = dropRules.find(
        r => r.difficulty === difficulty && r.poolType === selectedPoolType
    );
    if (!rule) throw new Error("Invalid difficulty or poolType");

    const { dropCountRange, rarityChances, guaranteedRarity, poolType } = rule;
    const totalDropCount = getRandomInt(dropCountRange[0], dropCountRange[1]);
    let remainingDropCount = totalDropCount;

    const drops = [];
    const fixed = [];
    const randoms = [];

    if (guaranteedRarity) {
        const itemId = getRandomItemFromPool(poolType, guaranteedRarity);
        if (itemId) {
            fixed.push(itemId);
            drops.push(itemId);
            remainingDropCount--;
        }
    }

    while (remainingDropCount-- > 0) {
        const rarity = getRarityByChance(rarityChances);
        const itemId = getRandomItemFromPool(poolType, rarity);
        if (itemId) {
            randoms.push(itemId);
            drops.push(itemId);
        }
    }

    console.log(`\n本次任務難度 ${difficulty} 掉落：`);
    if (fixed.length > 0) {
        console.log(`固定掉落(保底)：${fixed.map(getItemNameById).join(", ")}`);
    }
    if (randoms.length > 0) {
        console.log(`隨機掉落：${randoms.map(getItemNameById).join(", ")}`);
    }

    return drops;
}

module.exports = { generateDropForUser };
