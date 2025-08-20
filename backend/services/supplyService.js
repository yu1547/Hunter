// services/supplyService.js
const Supply = require('../models/supplyModel');
const User = require('../models/userModel');
const dropService = require('../services/dropService');

const COOLDOWN_MIN = 15;
const DIFFICULTY = 2;

exports.getAllSupplies = async () => {
    return await Supply.find({}, { __v: 0 }).lean();
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
    const drops = await dropService.generateDropForUser(userId, DIFFICULTY);

    // 掉落成功才刷新冷卻（此處以能執行完 generateDropForUser 視為成功）
    const next = new Date(now.getTime() + COOLDOWN_MIN * 60 * 1000);
    writeSupplyNextDate(user, supplyId, next);
    await user.save();

    return { success: true, drops, nextClaimTime: next };
};
