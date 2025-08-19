const express = require('express');
const router = express.Router();
const {
  getAllUsers,
  getUserById,
  createUser,
  updateUser,
  deleteUser,
} = require('../controllers/userController');
const {
  craftItem,
} = require('../controllers/backpackController');

// 用戶 API
router.get('/', getAllUsers);     // GET 所有用戶
router.get('/:id', getUserById);  // GET 單個用戶
router.post('/', createUser);     // POST 新用戶
router.put('/:id', updateUser);   // PUT 更新用戶信息
router.delete('/:id', deleteUser);// DELETE 刪除用戶

// 背包道具 API
router.post('/:id/craft', craftItem); // POST 合成道具

module.exports = router;
