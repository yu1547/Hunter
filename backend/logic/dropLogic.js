// 根據你提供的掉落規則，定義掉落物池和掉落規則的資料結構
const dropPools = [
    { poolType: "chest_bronze", rarity: 2, itemIds: ["item_r2_1", "item_r2_2"] },
    { poolType: "chest_bronze", rarity: 3, itemIds: ["item_r3_1", "item_r3_2"] },
    { poolType: "chest_bronze", rarity: 4, itemIds: ["item_r4_1"] },
    { poolType: "chest_silver", rarity: 2, itemIds: ["item_r2_3", "item_r2_4"] },
    { poolType: "chest_silver", rarity: 3, itemIds: ["item_r3_3", "item_r3_4"] },
    { poolType: "chest_silver", rarity: 4, itemIds: ["item_r4_2", "item_r4_3"] },
    { poolType: "chest_silver", rarity: 5, itemIds: ["item_r5_1"] },
    { poolType: "chest_gold", rarity: 3, itemIds: ["item_r3_5", "item_r3_6"] },
    { poolType: "chest_gold", rarity: 4, itemIds: ["item_r4_4", "item_r4_5"] },
    { poolType: "chest_gold", rarity: 5, itemIds: ["item_r5_2", "item_r5_3"] }
];

const dropRules = [
    {
        difficulty: 3,
        dropCountRange: [3, 5],
        rarityChances: { 2: 50, 3: 20, 4: 10 },
        guaranteedRarity: 3
    },
    {
        difficulty: 4,
        dropCountRange: [3, 5],
        rarityChances: { 2: 30, 3: 30, 4: 20, 5: 10 },
        guaranteedRarity: 4
    },
    {
        difficulty: 5,
        dropCountRange: [3, 5],
        rarityChances: { 3: 50, 4: 30, 5: 20 },
        guaranteedRarity: 5
    }
];

/**
 * 根據難度計算掉落物清單
 * @param {number} difficulty - 寶箱或事件的難度
 * @returns {Array<string>} - 掉落物品的 ID 列表
 */
function calculateDrops(difficulty) {
    const rule = dropRules.find(r => r.difficulty === difficulty);
    if (!rule) return [];

    const drops = [];
    let totalDropCount = Math.floor(Math.random() * (rule.dropCountRange[1] - rule.dropCountRange[0] + 1)) + rule.dropCountRange[0];

    // 處理固定掉落
    if (rule.guaranteedRarity) {
        const pool = dropPools.filter(p => p.rarity === rule.guaranteedRarity);
        if (pool.length > 0) {
            const randomPool = pool[Math.floor(Math.random() * pool.length)];
            drops.push(randomPool.itemIds[Math.floor(Math.random() * randomPool.itemIds.length)]);
            totalDropCount--;
        }
    }

    // 處理剩餘掉落
    while (totalDropCount > 0) {
        const rarityRoll = Math.floor(Math.random() * 100) + 1;
        let cumulativeChance = 0;
        let selectedRarity = null;

        for (const rarity in rule.rarityChances) {
            cumulativeChance += rule.rarityChances[rarity];
            if (rarityRoll <= cumulativeChance) {
                selectedRarity = parseInt(rarity);
                break;
            }
        }

        if (selectedRarity) {
            const pool = dropPools.filter(p => p.rarity === selectedRarity);
            if (pool.length > 0) {
                const randomPool = pool[Math.floor(Math.random() * pool.length)];
                drops.push(randomPool.itemIds[Math.floor(Math.random() * randomPool.itemIds.length)]);
                totalDropCount--;
            }
        } else {
            totalDropCount--; // 如果沒有選中任何稀有度，則減少掉落計數
        }
    }
    return drops;
}

module.exports = {
    calculateDrops
};
