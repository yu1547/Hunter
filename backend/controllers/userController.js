const User = require('../models/userModel');

// GET 所有用戶
const getAllUsers = async (req, res) => {
  try {
    const users = await User.find();
    res.status(200).json(users);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// GET 單個用戶
const getUserById = async (req, res) => {
  try {
    const user = await User.findById(req.params.id);
    if (!user) {
      return res.status(404).json({ message: '找不到該用戶' });
    }
    res.status(200).json(user);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// POST 創建新用戶
const createUser = async (req, res) => {
  try {
    const newUser = new User(req.body);
    const savedUser = await newUser.save();
    res.status(201).json(savedUser);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
};

// PUT 更新用戶信息
const updateUser = async (req, res) => {
  try {
    const updatedUser = await User.findByIdAndUpdate(
      req.params.id,
      req.body,
      { new: true }
    );
    
    if (!updatedUser) {
      return res.status(404).json({ message: '找不到該用戶' });
    }
    
    res.status(200).json(updatedUser);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
};

// DELETE 刪除用戶
const deleteUser = async (req, res) => {
  try {
    const deletedUser = await User.findByIdAndDelete(req.params.id);
    
    if (!deletedUser) {
      return res.status(404).json({ message: '找不到該用戶' });
    }
    
    res.status(200).json({ message: '用戶已成功刪除' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// 添加物品到背包
const addItemToBackpack = async (req, res) => {
  try {
    const { itemId, quantity } = req.body;
    
    if (!itemId || !quantity) {
      return res.status(400).json({ message: '物品ID和數量不能為空' });
    }
    
    const user = await User.findById(req.params.id);
    if (!user) {
      return res.status(404).json({ message: '找不到該用戶' });
    }
    
    // 檢查物品是否已在背包中
    const existingItemIndex = user.backpackItems.findIndex(
      item => item.itemId === itemId
    );
    
    if (existingItemIndex > -1) {
      // 如果物品已存在，增加數量
      user.backpackItems[existingItemIndex].quantity += quantity;
    } else {
      // 如果物品不存在，添加新物品
      user.backpackItems.push({ itemId, quantity });
    }
    
    await user.save();
    res.status(200).json(user);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
};

// 從背包移除物品
const removeItemFromBackpack = async (req, res) => {
  try {
    const { id, itemId } = req.params;
    const { quantity } = req.body;
    
    const user = await User.findById(id);
    if (!user) {
      return res.status(404).json({ message: '找不到該用戶' });
    }
    
    const existingItemIndex = user.backpackItems.findIndex(
      item => item.itemId === itemId
    );
    
    if (existingItemIndex === -1) {
      return res.status(404).json({ message: '背包中找不到該物品' });
    }
    
    // 如果指定了數量且小於當前數量，則減少數量
    if (quantity && user.backpackItems[existingItemIndex].quantity > quantity) {
      user.backpackItems[existingItemIndex].quantity -= quantity;
    } else {
      // 否則完全移除物品
      user.backpackItems.splice(existingItemIndex, 1);
    }
    
    await user.save();
    res.status(200).json(user);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
};

module.exports = {
  getAllUsers,
  getUserById,
  createUser,
  updateUser,
  deleteUser,
  addItemToBackpack,
  removeItemFromBackpack,
};
