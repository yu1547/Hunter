// services/supplyService.js
const Supply = require('../models/supplyModel');
const User = require('../models/userModel');
const dropService = require('../services/dropService');

const COOLDOWN_MIN = 15;
const DIFFICULTY = 1;

exports.getAllSupplies = async () => {
    return await Supply.find({}, { __v: 0 }).lean();
};

// 查詢狀態（不觸發領取）
exports.getStatus = async ({ userId, supplyId }) => {
    const user = await User.findById(userId);
    if (!user) throw new Error('USER_NOT_FOUND');
    const now = new Date();
    const nextClaimTime = readSupplyNextDate(user, supplyId);
    const canClaim = !nextClaimTime || nextClaimTime <= now;
    return { canClaim, nextClaimTime };
};

// 讀：回傳 Date（相容舊資料 {nextClaimTime}）
function readSupplyNextDate(user, supplyId) {
    const logs = user.supplyScanLogs;
    if (!logs) return null;
    const key = String(supplyId);
    const v = logs instanceof Map ? logs.get(key) : logs[key];
    if (!v) return null;
    if (v instanceof Date) return v;                    // 新格式：Date
    if (v && v.nextClaimTime) return new Date(v.nextClaimTime); // 舊格式：{ nextClaimTime }
    if (typeof v === 'string' || typeof v === 'number') {
        const d = new Date(v); if (!isNaN(d)) return d;
    }
    return null;
}

// 寫：只存成 Date
function writeSupplyNextDate(user, supplyId, nextDate) {
    const key = String(supplyId);
    if (!user.supplyScanLogs) {
        user.supplyScanLogs = {}; // 若是 Map，下面 set 會覆蓋
    }
    if (user.supplyScanLogs instanceof Map) {
        user.supplyScanLogs.set(key, nextDate);
    } else {
        user.supplyScanLogs[key] = nextDate;
        if (typeof user.markModified === 'function') user.markModified('supplyScanLogs');
    }
}

// 核心：補給站領取
exports.claimSupply = async ({ userId, supplyId }) => {
    const user = await User.findById(userId);
    if (!user) throw new Error('USER_NOT_FOUND');

    const now = new Date();
    const nextClaimTime = readSupplyNextDate(user, supplyId);

    // 冷卻檢查
    if (nextClaimTime && nextClaimTime > now) {
        return { success: false, reason: 'COOLDOWN', nextClaimTime };
    }


    // 呼叫既有掉落（內部已負責加到背包、存檔）
    let drops = await dropService.generateDropForUser(userId, DIFFICULTY);
    let consumedAncient = false; // 是否消耗古樹 Buff

    // 檢查古樹的枝幹 buff：未過期的話再掉一次（稀有度 +1）
    try {
        let ancientActive = false;

        if (Array.isArray(user.buff)) {
            const before = user.buff.length;

            // 移除已過期的buff；未過期的保留並啟用
            user.buff = user.buff.filter((b) => {
                if (!b || b.name !== 'ancient_branch') return true;
                if (b.expiresAt && new Date(b.expiresAt) <= now) {
                    // 已過期 -> 移除
                    return false;
                }
                // 未過期 -> 保留且啟用加成
                ancientActive = true;
                return true;
            });

            // 有移除才標記並先保存
            if (user.buff.length !== before) {
                if (typeof user.markModified === 'function') user.markModified('buff');
                await user.save();
            }
        }

        // 有效，再掉一次（稀有度 +1）
        if (ancientActive) {
            const extraDrops = await dropService.generateDropForUser(
                userId,
                Math.min(DIFFICULTY + 1, 5) 
            );
            drops = drops.concat(extraDrops);
        }
    } catch (e) {
        console.warn('ancient_branch extra drop failed:', e);
        // 失敗不影響第一次掉落與冷卻刷新
    }


    // 掉落成功才刷新冷卻（15 分鐘）
    const next = new Date(now.getTime() + COOLDOWN_MIN * 60 * 1000);
    writeSupplyNextDate(user, supplyId, next);
    await user.save();

    return { success: true, drops, nextClaimTime: next };
};
