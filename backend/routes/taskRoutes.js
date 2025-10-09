const express = require('express');
const router = express.Router();
const {
  getAllTasks,
  getTaskById,
  startGame,
  submitGuess,
  openTreasureBox,
  blessTree,
  completeSlimeAttack,
  getUserTasks,
} = require('../controllers/taskController');

// GET 所有任務
router.get('/', getAllTasks);

// GET 單個任務
router.get('/:id', getTaskById);

// 取得特定使用者的所有任務
router.get("/user/:userId", getUserTasks);

// 事件相關路由
router.post('/start/:eventId', startGame);
router.post('/guess', submitGuess);
router.post('/open-treasure-box', openTreasureBox);
router.post('/bless-tree', blessTree);
router.post('/complete-slime-attack', completeSlimeAttack);

module.exports = router;
