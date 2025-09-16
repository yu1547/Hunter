const User = require('../models/userModel');
const Task = require('../models/taskModel');
const Item = require('../models/itemModel');
const Spot = require('../models/spotModel'); // 需有 Spot model
const Event = require('../models/eventModel'); // 引入 Event model
const Rank = require('../models/rankModel'); // 引入 Rank model
const { generateDropItems } = require('../services/dropService'); // 引入 generateDropItems
const { addItemsToBackpack } = require('../services/backpackService'); // 引入 backpackService
const axios = require('axios'); // 用於呼叫 Flask
const mongoose = require('mongoose');
const { calculateDrops } = require('../logic/dropLogic');

// 新增：確保 missions 一定是陣列，避免 forEach 讀取 null
function ensureMissionsArray(user) {
  if (!user.missions) user.missions = [];
  if (!Array.isArray(user.missions)) user.missions = Array.from(user.missions || []);
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
    ensureMissionsArray(user);

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
    ensureMissionsArray(user);

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
    ensureMissionsArray(user);

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
    ensureMissionsArray(user);

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

    mission.state = 'claimed';

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
    
    await user.save();

    res.status(200).json({ user, message: isOvertime ? "任務超時，已領取道具獎勵，但無積分獎勵" : "獎勵已領取" });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// 刷新非 LLM 任務
const refreshNormalMissions = async (user) => {
  const now = new Date();
  
  // 取得所有非 LLM 任務的 ID
  const nonLLMTaskIds = user.missions
    .filter(m => !m.isLLM)
    .map(m => m.taskId);

  // 一次性查詢所有相關的事件詳細資訊
  const events = await Event.find({ _id: { $in: nonLLMTaskIds } });
  const eventsMap = new Map(events.map(e => [e._id.toString(), e]));

  // 處理 declined 和 claimed 的非 LLM 任務
  user.missions.forEach(mission => {
    const eventDetails = eventsMap.get(mission.taskId.toString());
    // 確保 eventDetails 存在且類型不是 'daily'
    if (!mission.isLLM && eventDetails && eventDetails.type !== 'daily') {
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
    // 從 Event model 獲取新任務，且 type 不為 'daily'
    const newEvents = await Event.aggregate([
      {
        $match: {
          _id: { $nin: currentUserTaskIds }, // 過濾掉已經存在的任務
          type: { $ne: 'daily' } // 確保 type 不是 'daily'
        }
      },
      { $sample: { size: totalNewTasksNeeded } } // 隨機選取 N 個事件
    ]);

    if (newEvents.length > 0) {
      let newEventIndex = 0;
      // 遍歷 user.missions，替換已 claimed 的任務
      const updatedMissions = user.missions.map(mission => {
        if (!mission.isLLM && mission.state === 'claimed' && newEventIndex < newEvents.length) {
          const newEvent = newEvents[newEventIndex++];
          return {
            taskId: newEvent._id,
            state: 'available',
            acceptedAt: null, expiresAt: null, refreshedAt: null,
            haveCheckPlaces: newEvent.spotId ? [{ spotId: newEvent.spotId, isCheck: false }] : [],
            isLLM: false
          };
        }
        return mission;
      });

      // 為了過濾掉已經被 claimed 的任務，但沒有被替換的情況（newEvents不夠）
      user.missions = updatedMissions.filter(m => m.isLLM || m.state !== 'claimed');

      // 填充新任務直到滿2個
      while (user.missions.filter(m => !m.isLLM).length < 2 && newEventIndex < newEvents.length) {
        const newEvent = newEvents[newEventIndex++];
        user.missions.push({
          taskId: newEvent._id,
          state: 'available',
          acceptedAt: null, expiresAt: null, refreshedAt: null,
          haveCheckPlaces: newEvent.spotId ? [{ spotId: newEvent.spotId, isCheck: false }] : [],
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
    ensureMissionsArray(user);


    // 1. 取得所有每日事件的 ID
    const dailyEvents = await Event.find({ type: 'daily' });
    const dailyEventIds = dailyEvents.map(event => event._id.toString());

    let missionUpdated = false;
    let triggeredEvent = null;

    
    // 2. 在 missions 陣列中尋找符合條件的任務
    const mission = user.missions.find(m => {
        // 檢查任務是否為每日任務
        const isDailyMission = dailyEventIds.includes(m.taskId.toString());

        // 確保任務處於 'available' 或 'in_progress' 狀態，且有需要檢查的地點
        return (m.state === 'available' || m.state === 'in_progress') &&
                m.haveCheckPlaces &&
                m.haveCheckPlaces.some(place => place.spotId.toString() === spotId && !place.isCheck);
    });

    if (mission) {
      const checkPlace = mission.haveCheckPlaces.find(place => place.spotId.toString() === spotId);
      if (checkPlace) {
        checkPlace.isCheck = true;
        missionUpdated = true;

        // 3. 檢查任務是否為每日任務，並將其狀態更新為 'in_progress'
        const isDailyMission = dailyEventIds.includes(mission.taskId.toString());
        if (isDailyMission && mission.state === 'available') {
          mission.state = 'in_progress';
        }

        const eventDetails = await Event.findById(mission.taskId);
        if (eventDetails) {
            triggeredEvent = eventDetails;
        }
      }
    }

    // If a mission was updated, save the user state first
    if (missionUpdated) {
      await user.save();
    } else {
      return res.status(400).json({ message: '此補給站沒有需要檢查的任務' });
    }

    // After updating the mission, check and trigger the event if one was found
    if (triggeredEvent) {
        const eventRes = await checkAndTriggerEvent(userId, triggeredEvent);
        if (eventRes.success) {
            return res.status(200).json({ 
                message: '任務進度已更新並觸發事件', 
                userMissions: user.missions, 
                eventResult: eventRes
            });
        } else {
            return res.status(200).json({ 
                message: '任務進度已更新，但事件觸發失敗', 
                userMissions: user.missions,
                error: eventRes.message
            });
        }
    }

    // If no event was triggered but a mission was updated
    return res.status(200).json({ message: '任務進度已更新', userMissions: user.missions });

  } catch (error) {
    console.error('檢查補給站任務時發生錯誤:', error);
    res.status(500).json({ message: '伺服器內部錯誤' });
  }
};

// 新增函式：檢查並觸發事件
const checkAndTriggerEvent = async (userId, event) => {
    try {
        const eventReq = {
            body: { userId: userId },
            params: { eventId: event._id }
        };

        // 模擬 res 物件，以便捕捉回傳資料
        const eventRes = {
            status: (code) => ({
                json: (data) => ({ status: code, data: data })
            })
        };

        let result;
        // 使用一個獨立的物件來管理事件處理器，而不是在 switch case 中硬編碼
        const eventHandlers = {
            '神秘商人的試煉' : async () => await trade(eventReq, eventRes),
            '石堆下的碎片': async () => await triggerStonePile(eventReq, eventRes),
            '在小小的 code 裡面抓阿抓阿抓': async () => await startGame(eventReq, eventRes),
            '打扁史萊姆': async () => {
                // 從 req.body 取得 totalDamage
                if (!eventReq.body.totalDamage) {
                    return eventRes.status(400).json({ message: '缺少 totalDamage 參數' });
                }
                return await completeSlimeAttack(eventReq, eventRes);
            },
            '偶遇銅寶箱': async () => {
                // 從 req.body 中取得 keyType
                if (!eventReq.body.keyType) {
                    return eventRes.status(400).json({ message: '缺少 keyType 參數' });
                }
                return await openTreasureBox(eventReq, eventRes);
            },
            '古樹的祝福': async () => {
                eventReq.body.itemToOffer = event.options[0].text.split(' ')[0].replace('交出', '');
                return await blessTree(eventReq, eventRes);
            }
        };

        // 檢查是否有特定的處理器
        if (eventHandlers[event.name]) {
            result = await eventHandlers[event.name]();
        } else {
            // 如果沒有特殊處理器，則呼叫通用的 completeEvent
            result = await completeEvent(eventReq, eventRes);
        }

        // 檢查回傳的狀態碼，判斷是否成功
        if (result && result.status >= 200 && result.status < 300) {
            return { success: true, message: '事件觸發成功', result: result.data };
        } else {
            return { success: false, message: '事件觸發失敗', error: result.data || '未知錯誤' };
        }

    } catch (error) {
        console.error('觸發事件時發生錯誤:', error);
        return { success: false, message: '伺服器內部錯誤', error: error.message };
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

// 分配每日任務給單一使用者
const assignDailyMissions = async (req, res) => {
    const { userId } = req.params;
    try {
        const user = await User.findById(userId);
        if (!user) return res.status(404).json({ message: '找不到使用者' });

        // 取得所有每日事件
        const dailyEvents = await Event.find({ type: 'daily' });
        const dailyEventIds = dailyEvents.map(event => event._id.toString());
        
        // 檢查使用者今天是否已經被分配過每日任務
        // 判斷條件：任務的 taskId 是否在 dailyEventIds 列表中，且 refreshedAt 是今天
        const today = new Date().toISOString().slice(0, 10);
        const hasDailyMissionsToday = user.missions.some(
            m => dailyEventIds.includes(m.taskId.toString()) && m.refreshedAt && m.refreshedAt.toISOString().slice(0, 10) === today
        );

        if (hasDailyMissionsToday) {
            return res.status(200).json({ message: '今日任務已分配' });
        }

        // 刪除使用者舊的每日任務，然後加入新的
        user.missions = user.missions.filter(mission => !dailyEventIds.includes(mission.taskId.toString()));

        const missionsToAdd = dailyEvents.map(event => ({
            taskId: event._id,
            state: 'available',
            refreshedAt: new Date(),
            // 任務地點直接使用 Event 模型中的固定 spotId
            haveCheckPlaces: event.spotId ? [{ spotId: event.spotId, isCheck: false }] : []
        }));
        
        user.missions.push(...missionsToAdd);
        await user.save();

        res.status(200).json({ message: '每日任務分配成功' });
    } catch (error) {
        console.error("分配每日任務失敗：", error);
        res.status(500).json({ message: '每日任務分配失敗', error: error.message });
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
  assignDailyMissions,
};