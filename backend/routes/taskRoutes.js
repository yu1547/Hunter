const express = require('express');
const router = express.Router();
const {
  getAllTasks,
  getTaskById,
} = require('../controllers/taskController');

// GET 所有任務
router.get('/', getAllTasks);

// GET 單個任務
router.get('/:id', getTaskById);

module.exports = router;
