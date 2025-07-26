const mongoose = require('mongoose');
const User = require('../models/User'); // 根據你的 userSchema 所在位置調整路徑

// 取得設定
const getSettings = async (req, res) => {
  const { id } = req.params;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return res.status(400).json({ message: '無效的使用者 ID' });
  }

  const user = await User.findById(id, 'settings');
  if (!user) {
    return res.status(404).json({ message: '找不到該用戶' });
  }

  res.json(user.settings);
};

// 更新設定
const updateSettings = async (req, res) => {
  const { id } = req.params;
  const { music, notification, language } = req.body;

  if (!mongoose.Types.ObjectId.isValid(id)) {
    return res.status(400).json({ message: '無效的使用者 ID' });
  }

  const user = await User.findById(id);
  if (!user) {
    return res.status(404).json({ message: '找不到該用戶' });
  }

  user.settings = { music, notification, language };
  await user.save();

  res.json({ message: '設定已更新', settings: user.settings });
};

module.exports = {
  getSettings,
  updateSettings
};
