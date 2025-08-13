const User = require('../models/userModel');
const Task = require('../models/taskModel');
const mongoose = require('mongoose');

// =====================================================================
// 輔助函式 (Helper Functions)
// =====================================================================

/**
 * 自動指派新任務的輔助函式
 * 從資料庫中隨機選取一個使用者沒有的任務，並將其加入使用者的任務列表。
 * @param {object} user - 使用者物件
 */
// 自動指派新任務的輔助函數
const assignNewMission = async (user) => {
  // 找出使用者目前所有的任務 ID
  const currentUserTaskIds = user.missions.map(m => m.taskId.toString());

  // 從資料庫中隨機選取一個使用者沒有的任務
  const newTask = await Task.aggregate([
    { $match: { _id: { $nin: currentUserTaskIds }, isLLM: false } }, // 確保指派的不是 LLM 任務
    { $sample: { size: 1 } }
  ]);

  if (newTask.length > 0) {
    user.missions.push({
      taskId: newTask[0]._id.toString(),
      state: 'available',
      acceptedAt: null,
      expiresAt: null,
      refreshedAt: null,
      checkPlaces: []
    });
  }
};

// =====================================================================
// 寶箱掉落邏輯
// =====================================================================

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



// =====================================================================
// API 路由處理函式
// =====================================================================
// 接受任務
const acceptTask = async (req, res) => {
  const { userId, taskId } = req.params;

  try {
    const user = await User.findById(userId);
    if (!user) return res.status(404).json({ message: '找不到用戶' });

    const mission = user.missions.find(m => m.taskId === taskId);
    if (!mission) return res.status(404).json({ message: '用戶沒有此任務' });

    if (mission.state !== 'available') {
      return res.status(400).json({ message: '任務狀態為 ${mission.state}，無法接受' });
    }

    const taskDetails = await Task.findById(taskId);
    if (!taskDetails) return res.status(404).json({ message: '找不到任務詳細資訊' });

    mission.state = 'in_progress';
    mission.acceptedAt = new Date();
    if (taskDetails.taskDuration) {
      mission.expiresAt = new Date(mission.acceptedAt.getTime() + taskDetails.taskDuration * 1000);
    }

    await user.save();
    res.status(200).json(user);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// 拒絕任務
const declineTask = async (req, res) => {
  const { userId, taskId } = req.params;

  try {
    const user = await User.findById(userId);
    if (!user) return res.status(404).json({ message: '找不到用戶' });

    const mission = user.missions.find(m => m.taskId === taskId);
    if (!mission) return res.status(404).json({ message: '用戶沒有此任務' });

    if (!['available', 'in_progress'].includes(mission.state)) {
      return res.status(400).json({ message: '任務狀態為 ${mission.state}，無法拒絕' });
    }

    mission.state = 'declined';
    mission.refreshedAt = new Date(Date.now() + 5 * 60 * 60 * 1000); // 5 小時後

    await user.save();
    res.status(200).json({ message: '任務已拒絕', user });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// 完成任務 (由其他遊戲邏輯觸發，例如打卡成功)
const completeTask = async (req, res) => {
  const { userId, taskId } = req.params;

  try {
    const user = await User.findById(userId);
    if (!user) return res.status(404).json({ message: '找不到用戶' });

    const mission = user.missions.find(m => m.taskId === taskId);
    if (!mission) return res.status(404).json({ message: '用戶沒有此任務' });

    if (mission.state !== 'in_progress') {
      return res.status(400).json({ message: '任務狀態為 ${mission.state}，無法完成' });
    }

    mission.state = 'completed';
    await user.save();
    res.status(200).json(user);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// 領取獎勵
const claimReward = async (req, res) => {
  const { userId, taskId } = req.params;

  try {
    const user = await User.findById(userId);
    if (!user) return res.status(404).json({ message: '找不到用戶' });

    const mission = user.missions.find(m => m.taskId === taskId);
    if (!mission) return res.status(404).json({ message: '用戶沒有此任務' });

    if (mission.state !== 'completed') {
      return res.status(400).json({ message: '任務狀態為 ${mission.state}，無法領取獎勵' });
    }

    const taskDetails = await Task.findById(taskId);
    if (!taskDetails) return res.status(404).json({ message: '找不到任務詳細資訊' });

    // 檢查是否超時
    const isOvertime = mission.expiresAt && new Date() > mission.expiresAt;

    // 發放獎勵道具
    if (taskDetails.rewardItems && taskDetails.rewardItems.length > 0) {
      taskDetails.rewardItems.forEach(reward => {
        const itemIndex = user.backpackItems.findIndex(item => item.itemId.toString() === reward.itemId.toString());
        if (itemIndex > -1) {
          user.backpackItems[itemIndex].quantity += reward.quantity;
        } else {
          user.backpackItems.push({ itemId: reward.itemId.toString(), quantity: reward.quantity });
        }
      });
    }

    // 如果沒有超時，發放積分
    // 這邊等排行榜寫好再移除註解
    // if (!isOvertime && taskDetails.rewardScore > 0) {
    //   user.score = (user.score || 0) + taskDetails.rewardScore;
    // }

    mission.state = 'claimed';
    
    await user.save();

    res.status(200).json({ user, message: isOvertime ? "任務超時，已領取道具獎勵，但無積分獎勵" : "獎勵已領取" });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// 刷新任務 (當用戶打開任務版時觸發)
const refreshMissions = async (req, res) => {
  const { userId } = req.params;
  try {
    let user = await User.findById(userId);
    if (!user) return res.status(404).json({ message: '找不到用戶' });

    const now = new Date();
    let needsRefresh = false;

    // 檢查 declined 任務是否已到刷新時間
    user.missions.forEach(mission => {
      if (mission.state === 'declined' && mission.refreshedAt && now > mission.refreshedAt) {
        mission.state = 'claimed'; // 標記為可替換
        needsRefresh = true;
      }
      // 如果任務本身就是 claimed 狀態，也需要刷新
      if (mission.state === 'claimed') {
        needsRefresh = true;
      }
    });

    // 確保用戶始終有5個任務
    const missionsToFill = 5 - user.missions.length;
    if (missionsToFill > 0) {
      needsRefresh = true;
    }

    if (needsRefresh) {
      // 找出所有需要被替換的任務索引
      const replaceableIndices = user.missions
        .map((mission, index) => (mission.state === 'claimed' ? index : -1))
        .filter(index => index !== -1);
      
      const numToReplace = replaceableIndices.length;
      const numToAddNew = 5 - user.missions.length;
      const totalNewTasksNeeded = numToReplace + Math.max(0, numToAddNew);

      if (totalNewTasksNeeded > 0) {
        // 找出用戶當前所有任務ID，避免分配重複任務
        const currentUserTaskIds = user.missions.map(m => m.taskId.toString());

        // 從資料庫獲取新的、不重複的任務
        const newTasks = await Task.find({
          _id: { $nin: currentUserTaskIds }
        }).limit(totalNewTasksNeeded);

        if (newTasks.length > 0) {
          let newTaskIndex = 0;

          // 替換 'claimed' 狀態的任務
          replaceableIndices.forEach(index => {
            if (newTaskIndex < newTasks.length) {
              const newTask = newTasks[newTaskIndex++];
              user.missions[index] = {
                taskId: newTask._id.toString(),
                state: 'available',
                acceptedAt: null,
                expiresAt: null,
                refreshedAt: null,
                checkPlaces: []
              };
            }
          });

          // 填充任務直到滿5個
          while (user.missions.length < 5 && newTaskIndex < newTasks.length) {
            const newTask = newTasks[newTaskIndex++];
            user.missions.push({
              taskId: newTask._id.toString(),
              state: 'available',
              acceptedAt: null,
              expiresAt: null,
              refreshedAt: null,
              checkPlaces: []
            });
          }
        }
      }
    }

    await user.save();
    
    res.status(200).json(user);

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

/**
 * 處理寶箱開啟的 API
 * @param {object} req - Express request object
 * @param {object} res - Express response object
 */
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
/**
 * 處理神秘商人交易的 API
 * @param {object} req - Express request object
 * @param {object} res - Express response object
 */
const trade = async (req, res) => {
    const { userId, tradeType } = req.body;

    try {
        const user = await User.findById(userId);
        if (!user) return res.status(404).json({ success: false, message: '使用者不存在。' });

        let requiredItemName, requiredCount, givenItemName;
        if (tradeType === "bronzeKey") {
            requiredItemName = "銅鑰匙碎片";
            requiredCount = 5;
            givenItemName = "銅鑰匙";
        } else if (tradeType === "silverKey") {
            requiredItemName = "銀鑰匙碎片";
            requiredCount = 5;
            givenItemName = "銀鑰匙";
        } else {
            return res.status(400).json({ success: false, message: '無效的交易類型' });
        }

        const requiredItem = await Item.findOne({ itemName: requiredItemName });
        if (!requiredItem) return res.status(404).json({ success: false, message: '所需物品不存在。' });

        const requiredItemInInv = user.backpackItems.find(item => item.itemId.toString() === requiredItem._id.toString());
        if (!requiredItemInInv || requiredItemInInv.quantity < requiredCount) {
            return res.json({ success: false, message: `你的${requiredItemName}不足${requiredCount}個。` });
        }

        const givenItem = await Item.findOne({ itemName: givenItemName });
        if (!givenItem) return res.status(404).json({ success: false, message: '給予物品不存在。' });

        // 執行交易：扣除所需物品，給予新物品
        requiredItemInInv.quantity -= requiredCount;

        const givenItemInInv = user.backpackItems.find(item => item.itemId.toString() === givenItem._id.toString());
        if (givenItemInInv) {
            givenItemInInv.quantity++;
        } else {
            user.backpackItems.push({ itemId: givenItem._id.toString(), quantity: 1 });
        }

        await user.save();

        return res.json({ success: true, message: `交易成功！你獲得了1個${givenItemName}。` });

    } catch (error) {
        console.error("交易失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};
/**
 * 處理古樹獻祭的 API
 * @param {object} req - Express request object
 * @param {object} res - Express response object
 */
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

/**
 * 處理史萊姆戰鬥結果的 API
 * @param {object} req - Express request object
 * @param {object} res - Express response object
 */
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
/**
 * 獲取石堆事件狀態的 API
 * @param {object} req - Express request object
 * @param {object} res - Express response object
 */
const getStonePileStatus = async (req, res) => {
    const { userId } = req.params;

    try {
        // 這裡的 db.hasTriggeredEventToday 需要替換成您實際的資料庫查詢邏輯
        // 例如：查詢使用者 lastStonePileTriggeredDate 欄位是否為今天
        const user = await User.findById(userId);
        const today = new Date().toISOString().slice(0, 10);
        const hasTriggeredToday = user.lastStonePileTriggeredDate && user.lastStonePileTriggeredDate.toISOString().slice(0, 10) === today;
        return res.json({ hasTriggeredToday });
    } catch (error) {
        console.error("獲取石堆狀態失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};

/**
 * 處理觸發石堆事件的 API
 * @param {object} req - Express request object
 * @param {object} res - Express response object
 */
const triggerStonePile = async (req, res) => {
    const { userId } = req.body;

    try {
        const user = await User.findById(userId);
        if (!user) return res.status(404).json({ success: false, message: '使用者不存在。' });

        const today = new Date().toISOString().slice(0, 10);
        const hasTriggeredToday = user.lastStonePileTriggeredDate && user.lastStonePileTriggeredDate.toISOString().slice(0, 10) === today;
        if (hasTriggeredToday) {
            return res.json({ success: false, message: '你今天已經搬開過石頭了，請明天再來。' });
        }

        const rewardItem = await Item.findOne({ itemName: "普通的史萊姆黏液" });
        if (!rewardItem) return res.status(404).json({ success: false, message: '獎勵物品不存在。' });

        const reward = { points: 10, items: [{ itemId: rewardItem._id.toString(), quantity: 1 }] };

        user.score = (user.score || 0) + reward.points;
        const existingItem = user.backpackItems.find(item => item.itemId.toString() === rewardItem._id.toString());
        if (existingItem) {
            existingItem.quantity++;
        } else {
            user.backpackItems.push({ itemId: rewardItem._id.toString(), quantity: 1 });
        }

        user.lastStonePileTriggeredDate = new Date();

        await user.save();

        return res.json({ success: true, message: `你搬開了石頭，獲得積分+${reward.points}和物品。` });

    } catch (error) {
        console.error("觸發石堆事件失敗：", error);
        return res.status(500).json({ success: false, message: '伺服器內部錯誤' });
    }
};

module.exports = {
  acceptTask,
  declineTask,
  completeTask,
  claimReward,
  refreshMissions,
  completeBugHunt,
  openTreasureBox,
  trade,
  blessTree,
  completeSlimeAttack,
  getStonePileStatus,
  triggerStonePile,
};
