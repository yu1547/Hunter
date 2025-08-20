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

const getUserByEmail = async (req, res) => {
  try {
    const user = await User.findOne({ email: req.params.email });
    if (!user) {
      return res.status(404).json({ message: '找不到該用戶' });
    }
    console.log('回傳的 user:', user);
    res.status(200).json(user);

  } catch (error) {
    res.status(500).json({ message: error.message });
  }
  
};

exports.updatePhotoURL = async (req, res) => {
  try {
    const userId = req.params.id;
    const { photoURL } = req.body;

    const updatedUser = await User.findByIdAndUpdate(
      userId,
      { photoURL: photoURL },
      { new: true }
    );

    if (!updatedUser) {
      return res.status(404).json({ message: 'User not found.' });
    }

    res.json({ message: 'Photo URL updated successfully.', user: updatedUser });
  } catch (error) {
    console.error('Error updating photo URL:', error);
    res.status(500).json({ message: 'Server error.' });
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



module.exports = {
  getAllUsers,
  getUserById,
  getUserByEmail, 
  createUser,
  updateUser,
  deleteUser,
};