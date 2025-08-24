const Task = require('../models/taskModel');
const mongoose = require('mongoose');
const { ObjectId } = mongoose.Types;
const User = require('../models/userModel');
const Item = require('../models/itemModel');
const Game = require('../models/eventModel');
const { calculateDrops } = require('../logic/dropLogic');

// GET 所有任務
const getAllTasks = async (req, res) => {
    try {
        const tasks = await Task.find();
        res.status(200).json(tasks);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// GET 單個任務
const getTaskById = async (req, res) => {
    try {
        const { id } = req.params;
        
        if (!ObjectId.isValid(id)) {
            return res.status(400).json({ message: '無效的 ID 格式' });
        }
        
        const task = await Task.findById(id);
        
        if (!task) {
            return res.status(404).json({ message: '找不到該任務' });
        }
        
        res.status(200).json(task);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

 // 假設你有一個單字庫
const WORD_LIST = ["APPLE", "POWER", "TIGER", "HOUSE", "CHAIR"];

 // 處理 Wordle 遊戲開始的 API
const startGame = async (req, res) => {
    const { userId } = req.body;

    try {
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: '找不到用戶' });
        }

        // 隨機選取一個謎底單字
        const secretWord = WORD_LIST[Math.floor(Math.random() * WORD_LIST.length)];

        // 在資料庫中建立一場新遊戲的紀錄
        const newGame = new Game({
            userId: user._id,
            secretWord: secretWord,
            guesses: [],
            status: 'playing',
            attemptsLeft: 6,
        });
        await newGame.save();

        res.status(200).json({
            success: true,
            message: "遊戲已開始！",
            gameId: newGame._id,
            attemptsLeft: newGame.attemptsLeft
        });
    } catch (error) {
        console.error("startGame API 錯誤:", error);
        res.status(500).json({ success: false, message: "伺服器錯誤，請稍後再試。" });
    }
};

 // 處理玩家提交 Wordle 猜測的 API
const submitGuess = async (req, res) => {
    const { gameId, guessWord } = req.body;

    try {
        const game = await Game.findById(gameId);
        if (!game) {
            return res.status(404).json({ success: false, message: '找不到遊戲' });
        }

        if (game.status !== 'playing') {
            return res.status(400).json({ success: false, message: '遊戲已經結束' });
        }

        // 檢查猜測單字長度
        if (guessWord.length !== 5) {
            return res.status(400).json({ success: false, message: '單字長度必須為 5 個字母' });
        }

        // ------------------------------------------
        // 核心比對邏輯 (與前端邏輯類似，但在後端執行)
        // ------------------------------------------
        const secretChars = game.secretWord.split('');
        const guessChars = guessWord.split('');
        const feedback = new Array(5).fill({ letter: '', status: 'not_in_word' });

        // 第一輪：檢查位置和字母都正確 (綠色)
        for (let i = 0; i < 5; i++) {
            if (guessChars[i].toUpperCase() === secretChars[i].toUpperCase()) {
                    feedback[i] = { letter: guessChars[i], status: 'correct' };
                    secretChars[i] = '_'; // 標記為已使用
                }
        }

        // 第二輪：檢查位置不對但字母存在 (黃色)
        for (let i = 0; i < 5; i++) {
        if (feedback[i].status !== 'correct') {
            const secretIndex = secretChars.findIndex(
                (char) => char.toUpperCase() === guessChars[i].toUpperCase()
            );
            if (secretIndex !== -1) {
                feedback[i] = { letter: guessChars[i], status: 'wrong_position' };
                secretChars[secretIndex] = '_'; // 標記為已使用
            } else {
                feedback[i] = { letter: guessChars[i], status: 'not_in_word' };
            }
        }
        }

        // ------------------------------------------
        // 更新遊戲狀態
        // ------------------------------------------
        game.guesses.push({ word: guessWord, feedback: feedback });
        game.attemptsLeft -= 1;

        let message = '';
        let success = true;

        // 檢查是否勝利
        if (guessWord.toUpperCase() === game.secretWord.toUpperCase()) {
            game.status = 'win';
            message = '恭喜你！猜對了！';
        // 處理積分和任務移除 (與你原本的邏輯相同)
        const user = await User.findById(game.userId);
        if (user) {
            user.score = (user.score || 0) + 500;
            await user.save();
            const bugHuntTask = await Task.findOne({ name: "Bug Hunt" });
            if (bugHuntTask) {
                user.missions = user.missions.filter(mission => mission.taskId.toString() !== bugHuntTask._id.toString());
                await user.save();
            }
        }
        } else if (game.attemptsLeft <= 0) {
            game.status = 'lose';
            message = `猜測次數用完囉！謎底是 ${game.secretWord}`;
            success = false;
        } else {
            message = '答案錯誤，請繼續猜測。';
        }

    await game.save();

    res.status(200).json({
        success: success,
        message: message,
        feedback: feedback,
        status: game.status,
        attemptsLeft: game.attemptsLeft,
    });

    } catch (error) {
        console.error("submitGuess API 錯誤:", error);
        res.status(500).json({ success: false, message: "伺服器錯誤，請稍後再試。" });
    }
};

// 處理寶箱開啟的 API
const openTreasureBox = async (req, res) => {
    const { userId, keyType } = req.body;
    const keyItemName = `${keyType === 'bronze' ? '銅' : keyType === 'silver' ? '銀' : '金'}鑰匙`;

    try {
        // 這裡的 db.findUserById 和 db.updateUserInventory 需要替換成您實際的 Mongoose 或其他 ORM 方法
        const user = await User.findById(userId);
        if (!user) return res.status(404).json({ success: false, message: '使用者不存在。' });

        const keyItem = await Item.findOne({ itemName: keyItemName });
        if (!keyItem) return res.status(404).json({ success: false, message: '鑰匙物品不存在。' });

        const keyInInv = user.backpackItems.find(item => item.itemId.toString() === keyItem._id.toString());
        if (!keyInInv || keyInInv.quantity <= 0) {
            return res.json({ success: false, message: `你沒有${keyItemName}，無法開啟寶箱。` });
        }

        keyInInv.quantity--;
        const drops = calculateDrops(keyType === 'bronze' ? 3 : keyType === 'silver' ? 4 : 5);

        // 處理掉落物
        for (const dropItemId of drops) {
            const existingItem = user.backpackItems.find(item => item.itemId.toString() === dropItemId);
            if (existingItem) {
                existingItem.quantity++;
            } else {
                user.backpackItems.push({ itemId: dropItemId, quantity: 1 });
            }
        }

        // 更新分數
        const points = keyType === 'bronze' ? 15 : keyType === 'silver' ? 25 : 40;
        user.score = (user.score || 0) + points;

        await user.save();

        return res.json({ success: true, message: `你用${keyItemName}打開了寶箱`, drops });

    } catch (error) {
        console.error("開啟寶箱失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};
// 處理古樹獻祭的 API
const blessTree = async (req, res) => {
    const { userId, itemToOffer } = req.body;

    try {
        const user = await User.findById(userId);
        if (!user) return res.status(404).json({ success: false, message: '使用者不存在。' });

        const offerItem = await Item.findOne({ itemName: itemToOffer });
        if (!offerItem) return res.status(404).json({ success: false, message: '獻祭物品不存在。' });

        const itemInInv = user.backpackItems.find(item => item.itemId.toString() === offerItem._id.toString());
        if (!itemInInv || itemInInv.quantity <= 0) {
            return res.json({ success: false, message: `你的${itemToOffer}不足，無法獻祭。` });
        }

        let rewardItemName, rewardCount;
        if (itemToOffer === "普通的史萊姆黏液") {
            rewardItemName = "銅鑰匙碎片";
            rewardCount = 2;
        } else if (itemToOffer === "黏稠的史萊姆黏液") {
            rewardItemName = "銀鑰匙碎片";
            rewardCount = 2;
        } else {
            return res.status(400).json({ success: false, message: '無效的獻祭物品' });
        }

        const rewardItem = await Item.findOne({ itemName: rewardItemName });
        if (!rewardItem) return res.status(404).json({ success: false, message: '獎勵物品不存在。' });

        // 扣除獻祭物品，給予獎勵
        itemInInv.quantity--;

        const rewardItemInInv = user.backpackItems.find(item => item.itemId.toString() === rewardItem._id.toString());
        if (rewardItemInInv) {
            rewardItemInInv.quantity += rewardCount;
        } else {
            user.backpackItems.push({ itemId: rewardItem._id.toString(), quantity: rewardCount });
        }

        await user.save();

        return res.json({ success: true, message: `古樹給予了你祝福，獲得${rewardItemName} x${rewardCount}。` });

    } catch (error) {
        console.error("獻祭古樹失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};

// 處理史萊姆戰鬥結果的 API
const completeSlimeAttack = async (req, res) => {
    const { userId, totalDamage } = req.body;

    try {
        const user = await User.findById(userId);
        if (!user) return res.status(404).json({ success: false, message: '使用者不存在。' });

        let rewards = { points: totalDamage * 2, items: [] };
        if (totalDamage > 10) {
            const item = await Item.findOne({ itemName: "黏稠的史萊姆黏液" });
            if (item) rewards.items.push({ itemId: item._id.toString(), quantity: 1 });
        } else {
            const item = await Item.findOne({ itemName: "普通的史萊姆黏液" });
            if (item) rewards.items.push({ itemId: item._id.toString(), quantity: 1 });
        }

        user.score = (user.score || 0) + rewards.points;
        for (const reward of rewards.items) {
            const existingItem = user.backpackItems.find(item => item.itemId.toString() === reward.itemId);
            if (existingItem) {
                existingItem.quantity += reward.quantity;
            } else {
                user.backpackItems.push(reward);
            }
        }

        await user.save();

        return res.json({
            success: true,
            message: `你造成了${totalDamage}點傷害，獲得積分+${rewards.points}和物品。`,
            rewards: rewards.items.map(item => item.name)
        });
    } catch (error) {
        console.error("史萊姆戰鬥結算失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};

module.exports = {
    getAllTasks,
    getTaskById,
    startGame,
    submitGuess,
    openTreasureBox,
    blessTree,
    completeSlimeAttack,
};
