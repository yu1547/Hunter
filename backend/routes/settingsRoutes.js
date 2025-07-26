const express = require('express');
const router = express.Router();
const { getSettings, updateSettings } = require('../controllers/settingsController');

// 取得使用者設定
router.get('/:id', getSettings);

// 更新使用者設定
router.put('/:id', updateSettings);

module.exports = router;
