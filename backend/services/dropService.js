//調落機制
const Items = require("../models/itemModel");
const DropRules = require("../models/dropRulesModel");
const DropPools = require("../models/dropPoolsModel");
const { addItemsToBackpack } = require("../services/backpackService");//添加到背包
console.log("✅ DropRules model name:", DropRules.modelName);
console.log("✅ DropRules collection name:", DropRules.collection.name);

// 產生一個介於 min 和 max 之間的隨機整數（包含 min 和 max）
function getRandomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

// 根據提供的稀有度機率，隨機決定一個稀有度
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

// 從指定稀有度的掉落池中隨機獲取一個道具 ID
async function getRandomItemFromPool(rarity) {
    const pool = await DropPools.findOne({ rarity });
    if (!pool || pool.itemIds.length === 0) return null;
    const randIndex = getRandomInt(0, pool.itemIds.length - 1);
    return pool.itemIds[randIndex];
}

// 根據指定的任務難度生成掉落道具
async function generateDropItems(difficulty) {
    // 根據難度查找掉落規則
    const rule = await DropRules.findOne({ difficulty: Number(difficulty) });
    if (!rule) throw new Error("Invalid difficulty");

    const { dropCountRange, rarityChances, guaranteedRarity } = rule;
    // 決定本次掉落的總數量
    const totalDropCount = getRandomInt(dropCountRange[0], dropCountRange[1]);
    let remainingDropCount = totalDropCount;

    const drops = {}; // 使用物件來累計道具 ID 和數量
    const fixed = []; // 儲存保底掉落物 ID
    const randoms = []; // 儲存隨機掉落物 ID

    const addItemToDrops = (itemId) => {
        const itemIdStr = itemId.toString();
        drops[itemIdStr] = (drops[itemIdStr] || 0) + 1;
    };

    // 處理保底掉落
    if (guaranteedRarity) {
        const itemId = await getRandomItemFromPool(guaranteedRarity);
        if (itemId) {
            fixed.push(itemId);
            addItemToDrops(itemId);
            remainingDropCount--;
        }
    }

    // 處理剩餘的隨機掉落
    while (remainingDropCount-- > 0) {
        const rarity = getRarityByChance(Object.fromEntries(rarityChances));
        const itemId = await getRandomItemFromPool(rarity);
        if (itemId) {
            randoms.push(itemId);
            addItemToDrops(itemId);
        }
    }

    // 為了偵錯，在控制台輸出生成的掉落物
    console.log(`\n任務難度 ${difficulty} 預計掉落：`);
    if (fixed.length > 0) {
        const fixedNames = await Promise.all(fixed.map(getItemNameById));
        console.log(`固定掉落(保底)：${fixedNames.join(", ")}`);
    }
    if (randoms.length > 0) {
        const randomNames = await Promise.all(randoms.map(getItemNameById));
        console.log(`隨機掉落：${randomNames.join(", ")}`);
    }

    // 將 drops 物件轉換為 { itemId, quantity } 格式的陣列
    const dropArray = Object.entries(drops).map(([itemId, quantity]) => ({
        itemId: itemId,
        quantity: quantity
    }));

    return dropArray; // 返回包含掉落物物件的陣列
}

// 根據道具 ID 獲取道具名稱
async function getItemNameById(itemId) {
    const item = await Items.findById(itemId);
    return item ? item.itemName : "未知道具";
}

// 為指定使用者生成掉落物，並將其添加到使用者的背包中
async function generateDropForUser(userId, difficulty) {
    console.log(`\nuserID ${userId} `);
    
    // 生成掉落物
    const drops = await generateDropItems(difficulty);
    console.log("🎁 drops =", JSON.stringify(drops, null, 2));


    // 將掉落物添加到背包
    if (drops.length > 0) {
        await addItemsToBackpack(userId, drops); // drops 是 [{itemId, quantity}] 的陣列
    }
    console.log(`測試分割線`);

    // 獲取掉落物的名稱以供顯示
    const dropNames = await Promise.all(drops.map(async (drop) => {
        const name = await getItemNameById(drop.itemId);
        return `${name} x${drop.quantity}`;
    }));

    console.log(`\n本次任務難度 ${difficulty} 掉落並已加入背包：`);
    console.log(dropNames.join(", "));

    return dropNames;
}

module.exports = { generateDropForUser, generateDropItems };
