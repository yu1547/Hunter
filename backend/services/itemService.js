// services/itemsService.js
const mongoose = require('mongoose');
const User = require('../models/userModel');
const Item = require('../models/itemModel');
const ItemUseLog = require('../models/itemUseLog');

function randInt(a, b) { return Math.floor(Math.random() * (b - a + 1)) + a; }

// --- 通用：把 user.buff 初始化為陣列並 push 一筆 ---
async function pushBuff(userId, buff, session) {
    await User.updateOne({ _id: userId, buff: null }, { $set: { buff: [] } }, { session });
    await User.updateOne({ _id: userId }, { $push: { buff } }, { session });
}

// --- 通用：背包扣 1 ---
async function decFromBackpack(userId, itemId, session) {
    const u = await User.findOne(
        { _id: userId, backpackItems: { $elemMatch: { itemId, quantity: { $gt: 0 } } } },
        { 'backpackItems.$': 1 }
    ).session(session);
    if (!u || !u.backpackItems?.length) throw new Error('NO_STOCK');
    
    // 從查詢結果 u 中取得物品的當前數量
    const currentQty = u.backpackItems[0].quantity;

    if (currentQty === 1) {
        // 會變 0：直接移除該項，避免把 quantity 寫成 0（因為 schema min:1）
        await User.updateOne(
            { _id: userId },
            { $pull: { backpackItems: { itemId } } },
            { session }
        );
        return;
    }
    // 其餘情況（≥2）正常扣 1
    await User.updateOne(
        { _id: userId, 'backpackItems.itemId': itemId },
        { $inc: { 'backpackItems.$.quantity': -1 } },
        { session }
    );
    await User.updateOne(
        { _id: userId },
        { $pull: { backpackItems: { itemId, quantity: { $lte: 0 } } } },
        { session }
    );
}

// --- 通用：背包加 n ---
async function addToBackpack(userId, itemId, n, session) {
    const r = await User.updateOne(
        { _id: userId, 'backpackItems.itemId': itemId },
        { $inc: { 'backpackItems.$.quantity': n } },
        { session }
    );
    if (r.matchedCount === 0) {
        await User.updateOne(
            { _id: userId },
            { $push: { backpackItems: { itemId, quantity: n } } },
            { session }
        );
    }
}

// --- 動態查詢並快取：優先用 itemPic，其次 itemName ---
const itemIdCache = new Map(); // key: lookup(itemPic 或 itemName) -> ObjectId
async function ensureItemIdByName(lookup, session) {
    if (itemIdCache.has(lookup)) return itemIdCache.get(lookup);
    let it = await Item.findOne({ itemPic: lookup }, { _id: 1 }).session(session);
    if (!it) it = await Item.findOne({ itemName: lookup }, { _id: 1 }).session(session);
    if (!it) throw new Error(`ITEM_NOT_FOUND:${lookup}`);
    itemIdCache.set(lookup, it._id);
    return it._id;
}

// 資料庫中鑰匙碎片的 itemPic
const PIC = {
    copperShard: 'copper_key_piece',   // DB的銅碎片 itemPic
    silverShard: 'silver_key_piece',   // DB的銀碎片 itemPic
    goldShard: 'gold_key_piece',     // DB的金碎片 itemPic
};

//檢查重複buff
async function upsertBuff(userId, { name, expiresAt, data }, session) {
    // 若 buff 還是 null，先初始化為 []
    await User.updateOne({ _id: userId, buff: null }, { $set: { buff: [] } }, { session });

    const setDoc = { 'buff.$.expiresAt': expiresAt };
    if (data !== undefined) setDoc['buff.$.data'] = data;

    const r = await User.updateOne(
        { _id: userId, 'buff.name': name },
        { $set: setDoc },
        { session }
    );

    if (r.matchedCount === 0) {
        await User.updateOne(
            { _id: userId },
            { $push: { buff: { name, expiresAt, data } } },
            { session }
        );
    }
}

// --- 時間沙漏減速function：延長所有 in_progress 的 expiresAt + N 分鐘 ---
async function extendClaimedMissions(userId, minutes, session) {
    const User = require('../models/userModel');
    const user = await User.findById(userId).session(session);
    if (!user) throw new Error('USER_NOT_FOUND');

    const now = new Date();
    let changed = 0;

    user.missions.forEach(m => {
        if (m.state === 'in_progress') {
            if (m.expiresAt) {
                m.expiresAt = new Date(m.expiresAt.getTime() + minutes * 60 * 1000);
            } else {
                // 若原本沒有時限，給一個「從現在起」的時限
                m.expiresAt = new Date(now.getTime() + minutes * 60 * 1000);
            }
            changed++;
        }
    });

    if (changed > 0) {
        await user.save({ session });
    }
    return changed; // 方便除錯/紀錄
}

// --- 時間沙漏加速function：把declined 中 refreshedAt 最晚的那一筆任務改為 claimed ---
async function refreshMissionsNow(userId, session) {
    const User = require('../models/userModel');
    const user = await User.findById(userId).session(session);
    if (!user) throw new Error('USER_NOT_FOUND');

    const declined = user.missions.filter(m => m.state === 'declined');
    if (declined.length === 0) return null;

    // 取 refreshedAt 最大；若為空則當作最小
    declined.sort((a, b) => {
        const ta = a.refreshedAt ? new Date(a.refreshedAt).getTime() : -Infinity;
        const tb = b.refreshedAt ? new Date(b.refreshedAt).getTime() : -Infinity;
        return tb - ta;
    });

    const target = declined[0];
    target.state = 'claimed'; // 改成 claimed，後續由刷新器替換
    await user.save({ session });

    return target.taskId; // 方便除錯/紀錄
}



// --- 道具策略表 ---
const registry = {
    // 普通史萊姆：銅碎片 1~3，銀碎片 0~2
    'spawn_slime_small': async ({ userId, session, effects }) => {
        const addCopper = randInt(1, 3);
        const addSilver = randInt(0, 2);
        const copperId = await ensureItemIdByName(PIC.copperShard, session); // 用 itemPic
        const silverId = await ensureItemIdByName(PIC.silverShard, session);
        await addToBackpack(userId, copperId, addCopper, session);
        await addToBackpack(userId, silverId, addSilver, session);
        effects.push({ fragments: { copperKeyShard: addCopper, silverKeyShard: addSilver } });
    },

    // 黃金史萊姆：銀碎片 1~3，金碎片 0~2
    'spawn_slime_big': async ({ userId, session, effects }) => {
        const addSilver = randInt(1, 3);
        const addGold = randInt(0, 2);
        const silverId = await ensureItemIdByName(PIC.silverShard, session); // 用 itemPic
        const goldId = await ensureItemIdByName(PIC.goldShard, session);
        await addToBackpack(userId, silverId, addSilver, session);
        await addToBackpack(userId, goldId, addGold, session);
        effects.push({ fragments: { silverKeyShard: addSilver, goldKeyShard: addGold } });
    },

    // 寶藏圖：設一次性 buff（TODO(在任務生成): 任務產生器讀到後注入金箱並移除）
    'treasure_map_trigger': async ({ userId, session, effects }) => {
        const expiresAt = new Date(Date.now() + 1000 * 60 * 60 * 24); // 1 天示意
        await upsertBuff(userId, {
            name: 'treasure_map_once',
            expiresAt,
            data: {}
        }, session);

        effects.push({ buffAdded: { name: 'treasure_map_once', expiresAt } });
    },

    // 時間沙漏 - 加速：立即刷新一項任務
    'hourglass_speed_refresh': async ({ userId, session, effects, refreshMissionsNow }) => {
        await refreshMissionsNow(userId, session); // TODO: 綁你既有的刷新實作
        effects.push({ missionsRefreshed: true });
    },

    // 時間沙漏 - 減速：所有已接任務 +15 分鐘
    'hourglass_slow_extend': async ({ userId, session, effects, extendClaimedMissions }) => {
        await extendClaimedMissions(userId, 15, session); // TODO: 綁你既有的延長實作
        effects.push({ missionsExtendedMin: 15 });
    },


    // 火把：加成 buff（TODO 史萊姆任務）
    'torch_buff': async ({ userId, session, effects }) => {
        const expiresAt = new Date(Date.now() + 1000 * 60 * 60 * 24 * 7); // 1 週
        await upsertBuff(userId, {
            name: 'torch',
            expiresAt,
            data: { damageMultiplier: 2 }
        }, session);

        effects.push({ buffAdded: { name: 'torch', expiresAt } });
    },

    // 古樹的枝幹：2 小時 buff（掉落/補給站額外運行一次、稀有度+1 上限5）
    'ancient_branch_buff': async ({ userId, session, effects }) => {
        const expiresAt = new Date(Date.now() + 1000 * 60 * 60 * 2); // 2 小時
        await upsertBuff(userId, {
            name: 'ancient_branch',
            expiresAt,
            data: { extraRoll: 1, rarityBoost: 1, rarityCap: 5 }
        }, session);

        effects.push({ buffAdded: { name: 'ancient_branch', expiresAt } });
    },
};

// --- 主要 ---
exports.useItem = async ({ userId, itemId, requestId }) => {
    const session = await mongoose.startSession();
    session.startTransaction();
    try {
        // 去重（避免重送重扣）
        if (requestId) {
            const dup = await ItemUseLog.findOne({ userId, itemId, requestId }).session(session);
            if (dup) throw new Error('DUPLICATE_REQUEST');
        }

        const item = await Item.findById(itemId).session(session);
        if (!item) throw new Error('ITEM_NOT_FOUND');

        // 鑰匙類：禁止在 /items/use 使用（只允許 /chest/open 消耗）
        // 規則：itemName 含「鑰匙」且不含「碎片」→ 視為鑰匙本體
        if (/鑰匙/.test(item.itemName) && !/碎片/.test(item.itemName)) {
            throw new Error('KEY_USE_NOT_ALLOWED');
        }

        // 扣庫存 1
        await decFromBackpack(userId, item._id, session);

        // 執行效果（藍色）
        const fn = registry[item.itemFunc];
        if (!fn) throw new Error('ITEM_FUNC_NOT_REGISTERED');
        const effects = [];
        await fn({
            userId, session, effects,
            refreshMissionsNow,
            extendClaimedMissions,
        });

        if (requestId) {
            await ItemUseLog.create([{ userId, itemId, requestId }], { session });
        }

        await session.commitTransaction();
        session.endSession();

        // 回傳最新背包與 buff
        const fresh = await User.findById(userId, { backpackItems: 1, buff: 1 });
        return { backpackItems: fresh.backpackItems || [], buff: fresh.buff || [], effects };
    } catch (e) {
        await session.abortTransaction();
        session.endSession();
        throw e;
    }
};
