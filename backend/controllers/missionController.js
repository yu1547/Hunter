const User = require('../models/userModel');
const Task = require('../models/taskModel');
const Item = require('../models/itemModel');
const Spot = require('../models/spotModel'); // 需有 Spot model
const Rank = require('../models/rankModel'); // 引入 Rank model
const { generateDropItems } = require('../services/dropService'); // 引入 generateDropItems
const { addItemsToBackpack } = require('../services/backpackService'); // 引入 backpackService
const axios = require('axios'); // 用於呼叫 Flask
const mongoose = require('mongoose');
const { calculateDrops } = require('../logic/dropLogic');

// =====================================================================
// API 路由處理函式
// =====================================================================
// 接受任務
const acceptTask = async (req, res) => {
  const { userId, taskId } = req.params;

  try {
    const user = await User.findById(userId);
    if (!user) return res.status(404).json({ message: '找不到用戶' });

    const mission = user.missions.find(m => m.taskId.toString() === taskId);
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

    const mission = user.missions.find(m => m.taskId.toString() === taskId);
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

    const mission = user.missions.find(m => m.taskId.toString() === taskId);
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

    const mission = user.missions.find(m => m.taskId.toString() === taskId);
    if (!mission) return res.status(404).json({ message: '用戶沒有此任務' });

    if (mission.state !== 'completed') {
      return res.status(400).json({ message: '任務狀態為 ${mission.state}，無法領取獎勵' });
    }

    const taskDetails = await Task.findById(taskId);
    const eventDetails = await Event.findById(taskId);

    if (!taskDetails && !eventDetails) {
      return res.status(404).json({ message: '找不到任務或事件詳細資訊' });
    }


    // 檢查是否有 expiresAt 欄位
    if (mission.expiresAt){
        // 檢查是否超時
        const isOvertime = mission.expiresAt && new Date() > mission.expiresAt;

        // 發放獎勵道具
        if (taskDetails.rewardItems && taskDetails.rewardItems.length > 0) {
          await addItemsToBackpack(user, taskDetails.rewardItems);
        }

        // 如果沒有超時，發放積分
        if (!isOvertime && taskDetails.rewardScore > 0) {
          const scoreToAdd = taskDetails.rewardScore;
        }
    }
    else if (eventDetails) {
          const scoreToAdd = eventDetails.rewards.points;
    }


      // 更新 Rank 集合中的分數
      await Rank.findOneAndUpdate(
        { userId: user._id },
        { $inc: { score: scoreToAdd } },
        { upsert: true, new: true } // 如果找不到用戶，就創建一個新的
      );

    mission.state = 'claimed';
    
    await user.save();

    res.status(200).json({ user, message: isOvertime ? "任務超時，已領取道具獎勵，但無積分獎勵" : "獎勵已領取" });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// 刷新非 LLM 任務
const refreshNormalMissions = async (user) => {
  const now = new Date();
  
  // 處理 declined 和 claimed 的非 LLM 任務
  user.missions.forEach(mission => {
    if (!mission.isLLM & eventDetails.type != 'daily') {
      if (mission.state === 'declined' && mission.refreshedAt && now > mission.refreshedAt) {
        mission.state = 'claimed'; // 標記為可替換
      }
    }
  });

  const normalMissions = user.missions.filter(m => !m.isLLM); // 過濾出非 LLM 任務
  // 計算需要新增的任務數量
  const replaceableMissionsCount = normalMissions.filter(m => m.state === 'claimed').length;
  // 確保至少有兩個非 LLM 任務
  const missionsToAddNew = 2 - (normalMissions.length - replaceableMissionsCount);
  // 確保不會新增負數任務
  const totalNewTasksNeeded = replaceableMissionsCount + Math.max(0, missionsToAddNew);

  if (totalNewTasksNeeded > 0) {
    const currentUserTaskIds = user.missions.map(m => m.taskId);
    const newTasks = await Task.aggregate([
      {
        $match: {
          _id: { $nin: currentUserTaskIds }, // 過濾掉已經存在的任務
          isLLM: { $ne: true } // 確保 isLLM 是 false 或不存在
        }
      },
      { $sample: { size: totalNewTasksNeeded } } // 隨機選取 N 個任務
    ]);

    if (newTasks.length > 0) {
      let newTaskIndex = 0;
      // 遍歷 user.missions，替換已 claimed 的任務
      const updatedMissions = user.missions.map(mission => {
        if (!mission.isLLM && mission.state === 'claimed' && newTaskIndex < newTasks.length) {
          const newTask = newTasks[newTaskIndex++];
          return {
            taskId: newTask._id,
            state: 'available',
            acceptedAt: null, expiresAt: null, refreshedAt: null,
            haveCheckPlaces: Array.isArray(newTask.checkPlaces) ? newTask.checkPlaces.map(place => ({ spotId: place.spotId, isCheck: false })) : [],
            isLLM: false
          };
        }
        return mission;
      });

      // 為了過濾掉已經被 claimed 的任務，但沒有被替換的情況（newTasks不夠）
      user.missions = updatedMissions.filter(m => m.isLLM || m.state !== 'claimed');

      // 填充新任務直到滿2個
      while (user.missions.filter(m => !m.isLLM).length < 2 && newTaskIndex < newTasks.length) {
        const newTask = newTasks[newTaskIndex++];
        user.missions.push({
          taskId: newTask._id,
          state: 'available',
          acceptedAt: null, expiresAt: null, refreshedAt: null,
          haveCheckPlaces: Array.isArray(newTask.checkPlaces) ? newTask.checkPlaces.map(place => ({ spotId: place.spotId, isCheck: false })) : [],
          isLLM: false
        });
      }
    }
  }
  return user;
};

// 刷新 LLM 任務
const refreshLLMMissions = async (user) => {
  const now = new Date();
  const tasksToRemove = [];

  user.missions.forEach(mission => {
    if (mission.isLLM) {
      const isDeclinedAndExpired = mission.state === 'declined' && mission.refreshedAt && now > mission.refreshedAt;
      const isClaimed = mission.state === 'claimed';
      if (isDeclinedAndExpired || isClaimed) {
        tasksToRemove.push(mission.taskId.toString());
      }
    }
  });

  if (tasksToRemove.length > 0) {
    // 從 user.missions 中移除
    user.missions = user.missions.filter(m => !tasksToRemove.includes(m.taskId.toString()));
    // 從 Task collection 中刪除
    await Task.deleteMany({ _id: { $in: tasksToRemove } });
  }
  return user;
};

// 刷新所有任務 (新的 API 端點)
const refreshAllMissions = async (req, res) => {
  const { userId } = req.params;
  try {
    let user = await User.findById(userId);
    if (!user) return res.status(404).json({ message: '找不到用戶' });

    // 1. 處理 LLM 任務 (刪除)
    user = await refreshLLMMissions(user);

    // 2. 處理一般任務 (替換/新增)
    user = await refreshNormalMissions(user);

    await user.save();
    
    res.status(200).json({ missions: user.missions });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const checkSpotMission = async (req, res) => {
  const { userId, spotId } = req.params;

  try {
    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ message: '找不到使用者' });
    }

    let missionUpdated = false;

    // 尋找使用者是否有需要檢查此地點的任務
    user.missions.forEach(mission => {
      // 確保任務處於進行中狀態，且有需要檢查的地點
      if (mission.state === 'accepted' && mission.haveCheckPlaces) {
        // 尋找匹配的補給站
        const checkPlace = mission.haveCheckPlaces.find(place => place.spotId.toString() === spotId);

        if (checkPlace && !checkPlace.isCheck) {
          checkPlace.isCheck = true;
          missionUpdated = true;
        }
      }
    });

    if (missionUpdated) {
      await user.save();
      return res.status(200).json({ message: '任務進度已更新', userMissions: user.missions });
    } else {
      return res.status(400).json({ message: '此補給站沒有需要檢查的任務' });
    }

  } catch (error) {
    console.error('檢查補給站任務時發生錯誤:', error);
    res.status(500).json({ message: '伺服器內部錯誤' });
  }
};



// 產生 LLM 任務並分配給指定 user
const createLLMMission = async (req, res) => {
  const { userId } = req.params;
  const { userLocation } = req.body;

  try {
    // 取得 user
    const user = await User.findById(userId);
    if (!user) return res.status(404).json({ message: '找不到用戶' });

    // 取得所有 spots
    const spots = await Spot.find();

    const candidateLandmarks = spots.map(spot => ({
      spotId: spot._id.toString(),
      spotName: spot.spotName,
      longitude: spot.longitude,
      latitude: spot.latitude
    }));

    const payload = {
      userLocation,
      candidateLandmarks
    };

    // 呼叫 Flask /route
    const flaskUrl = process.env.LLM_FLASK_URL || 'http://llm:5050/route'; // docker 內部網域
    const flaskRes = await axios.post(flaskUrl, payload, { timeout: 10000 });

    const result = flaskRes.data;

    // 根據 LLM 回傳的難度產生獎勵道具
    const difficultyMap = { easy: 2, normal: 3, hard: 4 };
    const difficulty = difficultyMap[result.taskDifficulty] || 3; // 預設為 normal
    const generatedDrops = await generateDropItems(difficulty); // returns [{itemId, quantity}]

    // 將掉落物轉換為 taskModel 需要的格式
    const rewardItems = generatedDrops.map(drop => ({
      itemId: new mongoose.Types.ObjectId(drop.itemId),
      quantity: drop.quantity
    }));

    // 依照 taskModel 組成任務
    const newTask = {
      taskName: result.taskName || 'LLM任務',
      taskDescription: result.taskDescription || '',
      taskDifficulty: result.taskDifficulty || 'normal',
      taskTarget: result.taskTarget || '',
      checkPlaces: result.route
        ? result.route.map(r => ({ spotId: new mongoose.Types.ObjectId(r.id) }))
        : [],
      taskDuration: result.taskDuration ? result.taskDuration * 1000 : null, // LLM 回傳秒，轉為毫秒
      rewardItems: rewardItems,
      rewardScore: 50, // LLM 任務固定 50 分
      isLLM: true
    };

    // 存入 tasks collection
    const createdTask = await Task.create(newTask);

    // 加入 user.missions，狀態設為 in_progress，checkPlaces 同步 task.checkPlaces
    user.missions.push({
      taskId: createdTask._id,
      state: 'in_progress',
      acceptedAt: new Date(),
      expiresAt: newTask.taskDuration ? new Date(Date.now() + newTask.taskDuration) : null,
      refreshedAt: null,
      haveCheckPlaces: Array.isArray(createdTask.checkPlaces)
        ? createdTask.checkPlaces.map(place => ({
            spotId: place.spotId,
            isCheck: false
          }))
        : [],
      isLLM: true
    });

    await user.save();

    res.status(200).json({ missions: user.missions });
  } catch (error) {
    console.error('createLLMMission error:', error);
    res.status(500).json({ message: error.message });
  }
};

module.exports = {
  acceptTask,
  declineTask,
  completeTask,
  claimReward,
  refreshAllMissions,
  createLLMMission,
  checkSpotMission,
};