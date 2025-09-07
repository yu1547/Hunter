const Task = require('../models/taskModel');
const mongoose = require('mongoose');
const { ObjectId } = mongoose.Types;
const User = require('../models/userModel');
const Item = require('../models/itemModel');
const Game = require('../models/eventModel');

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
    const { eventId } = req.params;
    const { userId } = req.body;

    try {
        const event = await Event.findById(eventId);
        if (!event) {
            return res.status(404).json({ success: false, message: '找不到事件' });
        }

        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: '找不到用戶' });
        }
        
        // 在startGame時就更新任務狀態，並存入資料庫
        const mission = user.missions.find(m => m.taskId.toString() === eventId);
        if (mission) {
            mission.state = 'claimed';
            await user.save();
        }

        // 隨機選取一個謎底單字
        const secretWord = WORD_LIST[Math.floor(Math.random() * WORD_LIST.length)];
    

        // 在資料庫中建立一場新遊戲的紀錄
        const newGame = new Game({
            userId: user._id,
            eventId: eventId, // 儲存 Event ID 來建立關聯
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
        game.guesses.push({ word: guessWord, feedback: feedback });
        game.attemptsLeft -= 1;

        let message = '';
        let success = true;

        // 檢查是否勝利
        if (guessWord.toUpperCase() === game.secretWord.toUpperCase()) {
            game.status = 'completed';
            message = '恭喜你！猜對了！';
            await game.save();
            // 遊戲獲勝，交由 completeEvent 處理獎勵發放，傳遞正確的 eventId
            const eventReq = {
                body: { userId: game.userId, gameResult: 'win' },
                params: { eventId: game.eventId } // 從 game 物件取得關聯的 eventId
            };
            await completeEvent(eventReq, res);
            return;
        } else if (game.attemptsLeft <= 0) {
            game.status = 'claimed';
            message = `猜測次數用完囉！謎底是 ${game.secretWord}`;
            success = false;
            await game.save();

            // 呼叫 completeEvent 處理失敗狀態
            const eventReq = {
                body: { userId: game.userId, gameResult: 'lose' },
                params: { eventId: game.eventId } // 從 game 物件取得關聯的 eventId
            };
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

        // 將寶箱的 ID 和對應的 difficulty 傳遞給 completeEvent
        req.params.eventId = event._id;
        req.body.selectedOption = difficulty;
        
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

        // 將古樹事件的 ID 和選定的選項傳遞給 completeEvent
        req.params.eventId = event._id;
        req.body.selectedOption = `交出${itemToOffer}`;
        
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


module.exports = {
    getAllTasks,
    getTaskById,
    startGame,
    submitGuess,
    openTreasureBox,
    blessTree,
    completeSlimeAttack,
};
