const express = require('express');
const router = express.Router();
const {
  acceptTask,
  declineTask,
  completeTask,
  claimReward,
  refreshMissions,
  openTreasureBox,
  trade,
  blessTree,
  completeSlimeAttack,
  getStonePileStatus,
  triggerStonePile,
  startGame, // 新增
  submitGuess, // 新增
} = require('../controllers/missionController');

// 刷新用戶任務列表 (例如：用戶打開任務面板時調用)
router.post('/users/:userId/missions/refresh', refreshMissions);

// 接受任務
router.post('/users/:userId/missions/:taskId/accept', acceptTask);

// 拒絕任務
router.post('/users/:userId/missions/:taskId/decline', declineTask);

// 完成任務 (此端點可能由其他服務器邏輯調用，例如驗證打卡成功後)
router.post('/users/:userId/missions/:taskId/complete', completeTask);

// 領取任務獎勵
router.post('/users/:userId/missions/:taskId/claim', claimReward);

router.post('/events/start', startGame);
router.post('/events/guess', submitGuess);
router.post('/events/open-treasure-box', openTreasureBox);
router.post('/events/trade', trade);
router.post('/events/bless-tree', blessTree);
router.post('/events/complete-slime-attack', completeSlimeAttack);
router.get('/events/stone-pile-status/:userId', getStonePileStatus);
router.post('/events/trigger-stone-pile', triggerStonePile);

module.exports = router;
