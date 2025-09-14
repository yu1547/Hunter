const express = require('express');
const router = express.Router();
const {
  acceptTask,
  declineTask,
  completeTask,
  claimReward,
  refreshAllMissions,
  createLLMMission,
  checkSpotMission,
  assignDailyMissions,
} = require('../controllers/missionController');

// 刷新用戶任務列表 (例如：用戶打開任務面板時調用)
router.post('/users/:userId/missions/refresh', refreshAllMissions);

// 接受任務
router.post('/users/:userId/missions/:taskId/accept', acceptTask);

// 拒絕任務
router.post('/users/:userId/missions/:taskId/decline', declineTask);

// 完成任務 (此端點可能由其他服務器邏輯調用，例如驗證打卡成功後)
router.post('/users/:userId/missions/:taskId/complete', completeTask);

// 領取任務獎勵
router.post('/users/:userId/missions/:taskId/claim', claimReward);

// 產生 LLM 任務並分配給 user
router.post('/missions/llm/:userId', createLLMMission);

// 檢查地點任務
router.put('/check-spot/:userId/:spotId', checkSpotMission);

// 指派每日任務給使用者
router.post('/assign-daily/:userId', assignDailyMissions);

module.exports = router;
