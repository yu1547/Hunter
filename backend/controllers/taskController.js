const Event = require('../models/eventModel');
const Task = require('../models/taskModel');
const mongoose = require('mongoose');
const { ObjectId } = mongoose.Types;
const User = require('../models/userModel');
const Item = require('../models/itemModel');
const { completeEvent } = require('./eventController');
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
const gameStates = {}; // 暫時儲存遊戲狀態的記憶體物件

 // 處理 Wordle 遊戲開始的 API
const startGame = async (req, res) => {
    const { eventId } = req.params;
    const { userId } = req.body;

    try {        
        
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: '找不到用戶' });
        }
        
        // 1. 在 user.missions 中找到對應的任務
        const mission = user.missions.find(m => m.taskId.toString() === eventId);
        if (!mission) {
            return res.status(404).json({ success: false, message: '用戶沒有此任務' });
        }

        // 2. 檢查任務狀態，允許從 'available' 或 'in_progress' 開始
        if (mission.state !== 'available' && mission.state !== 'in_progress') {
            return res.status(400).json({ success: false, message: `任務狀態為 ${mission.state}，無法開始遊戲` });
        }

        // 3. 隨機選取一個謎底單字
        const secretWord = WORD_LIST[Math.floor(Math.random() * WORD_LIST.length)];
    

        // 4. 將遊戲狀態暫時儲存在記憶體中
        gameStates[userId] = {
            taskId: eventId, // taskId 就是 eventId
            secretWord: secretWord,
            guesses: [],
            attemptsLeft: 6,
            status: 'playing',
        };
        
        // 5. 僅在任務狀態為 'available' 時才更新用戶的任務狀態
        if (mission.state === 'available') {
            mission.state = 'in_progress';
            await user.save();
        }
        await user.save();

        res.status(200).json({
            success: true,
            message: "遊戲已開始！",
            // ✅ 修正: gameId 不是必要的，前端可以用 userId 繼續玩
            gameId: userId,
        });
        
    } catch (error) {
        console.error("startGame API 錯誤:", error);
        res.status(500).json({ success: false, message: "伺服器錯誤，請稍後再試。" });
    }
};

 // 處理玩家提交 Wordle 猜測的 API
const submitGuess = async (req, res) => {
    const { gameId, guessWord } = req.body;
    const userId = req.body.userId; // 確保有傳入 userId

    try {
        // 1. 確認用戶的遊戲狀態是否存在於記憶體中
        const game = gameStates[userId];
        if (!game) {
            return res.status(400).json({ success: false, message: '請先開始一場新遊戲' });
        }

        // 2. 確認用戶的任務狀態
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: '找不到用戶' });
        }

        const mission = user.missions.find(m => m.taskId.toString() === game.taskId);
        if (!mission || gameStates[userId].status !== 'playing') {
            delete gameStates[userId]; // 清除無效的遊戲
            return res.status(400).json({ success: false, message: '遊戲狀態不正確或已結束' });
        }

        // 檢查猜測單字長度
        if (guessWord.length !== 5) {
            return res.status(400).json({ success: false, message: '單字長度必須為 5 個字母' });
        }

        // ------------------------------------------
        // 核心比對邏輯
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
        // 3. 更新遊戲狀態（在記憶體中）
        game.guesses.push({ word: guessWord, feedback: feedback });
        game.attemptsLeft -= 1;

        let message = '';
        let success = true;

        // 檢查是否勝利
        if (guessWord.toUpperCase() === game.secretWord.toUpperCase()) {
            game.status = 'completed';
            mission.state = 'completed'; // 更新 user.missions 狀態
            message = '恭喜你！猜對了！';
            await user.save();
            
            // // 遊戲獲勝，交由 completeEvent 處理獎勵發放，傳遞正確的 eventId
            // const eventReq = {
            //     body: { userId: game.userId, gameResult: game.status },
            //     params: { eventId: game.eventId } // 從 game 物件取得關聯的 eventId
            // };
            // await completeEvent(eventReq, res);
            // return;
        } else if (game.attemptsLeft <= 0) {
            game.status = 'lose';
            mission.state = 'claimed'; // 任務狀態設為完成
            message = `猜測次數用完囉！謎底是 ${game.secretWord}`;
            success = false;
            await user.save();

            // // 呼叫 completeEvent 處理失敗狀態
            // const eventReq = {
            //     body: { userId: game.userId, gameResult: game.status },
            //     params: { eventId: game.eventId } // 從 game 物件取得關聯的 eventId
            // };
            // await completeEvent(eventReq, res);
        } else {
            message = '答案錯誤，請繼續猜測。';
        }

        // await game.save();

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
    const { keyType } = req.body;
    
    // 定義鑰匙類型與難度值的對應關係
    const keyToDifficultyMap = {
        'bronze': 3, // 銅鑰匙對應難度 3
        'silver': 4, // 銀鑰匙對應難度 4
        'gold': 5    // 金鑰匙對應難度 5
    };

    const difficulty = keyToDifficultyMap[keyType];
    
    if (!difficulty) {
        return res.status(400).json({ success: false, message: '無效的鑰匙類型。' });
    }

    try {
        // const eventName = `偶遇${keyType === 'bronze' ? '銅' : keyType === 'silver' ? '銀' : '金'}寶箱`;
        const eventName = `偶遇銅寶箱`;
        const event = await Event.findOne({ name: eventName });
        if (!event) {
            return res.status(404).json({ success: false, message: `${eventName} 不存在` });
        }

        // 將寶箱的 ID 和對應的 difficulty 、keyType 傳遞給 completeEvent
        req.params.eventId = event._id;
        req.body.selectedOption = difficulty;
        req.body.keyUsed = keyType; // 傳遞使用的鑰匙類型
        
        return await completeEvent(req, res);
    } catch (error) {
        console.error("開啟寶箱失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};



// 處理古樹獻祭的 API
const blessTree = async (req, res) => {
    const { userId, itemToOffer } = req.body;
    try {
        const event = await Event.findOne({ name: '古樹的祝福' });
        if (!event) {
            return res.status(404).json({ success: false, message: '古樹事件不存在' });
        }

        // ======================= START: 新增的驗證邏輯 =======================
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: '找不到使用者' });
        }

        const eventId = event._id.toString();
        const mission = user.missions.find(m => m.taskId.toString() === eventId);

        // 檢查任務是否存在
        if (!mission) {
            return res.status(400).json({ success: false, message: '你尚未領取古樹的祝福任務，無法進行獻祭。' });
        }
        
        // 檢查任務狀態是否正確
        if (mission.state !== 'available' && mission.state !== 'in_progress') {
             return res.status(400).json({ success: false, message: `任務狀態為 ${mission.state}，無法進行獻祭。` });
        }
        // ======================= END: 新增的驗證邏輯 =========================

        // 將古樹事件的 ID 和選定的選項傳遞給 completeEvent
        req.params.eventId = event._id;
        req.body.selectedOption = itemToOffer;
        console.log(`交出物品: ${itemToOffer}`);
        
        return await completeEvent(req, res);
    } catch (error) {
        console.error("獻祭古樹失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};


// 處理史萊姆戰鬥結果的 API
const completeSlimeAttack = async (req, res) => {
    const { userId, totalDamage } = req.body;
    try {
        const event = await Event.findOne({ name: '打扁史萊姆' });
        if (!event) {
            return res.status(404).json({ success: false, message: '史萊姆事件不存在' });
        }

        // 檢查 totalDamage 是否為有效數字
        if (typeof totalDamage !== 'number' || totalDamage < 0) {
            return res.status(400).json({ success: false, message: '無效的遊戲結果' });
        }

        // 尋找使用者並檢查是否有火把 buff
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: '找不到使用者。' });
        }

         // --- 檢查並應用增益效果 ---
        let finalDamage = totalDamage;
        const now = new Date();
        const torchBuff = user.buff?.find(b => b.name === 'torch' && b.expiresAt > now);

        if (torchBuff) {
            // 如果有火把增益，傷害加倍
            finalDamage = totalDamage * (torchBuff.data?.damageMultiplier || 1);
        }

        console.log(`原始傷害: ${totalDamage}, 最終傷害: ${finalDamage}`);

        // 將史萊姆事件的 ID 和遊戲結果傳遞給 completeEvent
        req.params.eventId = event._id;
        req.body.gameResult = finalDamage;
        
        return await completeEvent(req, res);
    } catch (error) {
        console.error("史萊姆戰鬥結算失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};

const getUserTasks = async (req, res) => {
    try {
        const { userId } = req.params;
        const user = await User.findById(userId).populate('missions.taskId'); // 使用 populate 來取得完整的 task 物件

        if (!user) {
            return res.status(404).json({ success: false, message: "使用者不存在" });
        }

        // user.tasks 現在會是完整的任務物件陣列
        res.status(200).json({
            success: true,
            tasks: user.tasks,
        });

    } catch (error) {
        console.error("取得使用者任務時發生錯誤:", error);
        res.status(500).json({ success: false, message: "伺服器錯誤" });
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
    getUserTasks,
};
