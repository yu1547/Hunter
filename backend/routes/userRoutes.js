const express = require('express');
const router = express.Router();
const User = require("../models/userModel"); 
const {
  getAllUsers,
  getUserById,
  createUser,
  updateUser,
  deleteUser,
  getUserByEmail,
} = require('../controllers/userController');
const {
  addItemToBackpack,
  craftItem,
} = require('../controllers/backpackController');

// 用戶 API
router.get('/', getAllUsers);     // GET 所有用戶
router.get('/email/:email', getUserByEmail);
router.get('/:id', getUserById);  // GET 單個用戶
router.post('/', createUser);     // POST 新用戶
router.put('/:id', updateUser);   // PUT 更新用戶信息
router.delete('/:id', deleteUser);// DELETE 刪除用戶
router.patch("/:id/photo", async (req, res) => {
  const { id } = req.params;
  const { photoURL } = req.body;

  try {
    const updatedUser = await User.findByIdAndUpdate(
      id,
      { photoURL },
      { new: true }
    );

    if (!updatedUser) {
      return res.status(404).json({ message: "使用者不存在" });
    }

    res.json(updatedUser);
  } catch (err) {
    console.error("更新頭貼錯誤：", err);
    res.status(500).json({ message: "伺服器錯誤" });
  }
});


// 背包道具 API
router.post('/:id/backpack', addItemToBackpack);    // POST 添加物品到背包
router.post('/:id/craft', craftItem); // POST 合成道具

module.exports = router;