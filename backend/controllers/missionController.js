const User = require('../models/userModel');
const Task = require('../models/taskModel');
const mongoose = require('mongoose');

// 接受任務
const acceptTask = async (req, res) => {
  const { userId, taskId } = req.params;

  try {
    const user = await User.findById(userId);
    if (!user) return res.status(404).json({ message: '找不到用戶' });

    const mission = user.missions.find(m => m.taskId === taskId);
    if (!mission) return res.status(404).json({ message: '用戶沒有此任務' });

    if (mission.state !== 'available') {
      return res.status(400).json({ message: `任務狀態為 ${mission.state}，無法接受` });
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
      return res.status(400).json({ message: `任務狀態為 ${mission.state}，無法拒絕` });
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
      return res.status(400).json({ message: `任務狀態為 ${mission.state}，無法完成` });
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
      return res.status(400).json({ message: `任務狀態為 ${mission.state}，無法領取獎勵` });
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


module.exports = {
  acceptTask,
  declineTask,
  completeTask,
  claimReward,
  refreshMissions,
};