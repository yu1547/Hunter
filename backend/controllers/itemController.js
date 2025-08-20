const Item = require('../models/itemModel');
const itemsService = require('../services/itemService');
const mongoose = require('mongoose');
const { ObjectId } = mongoose.Types;

// GET 所有道具
const getAllItems = async (req, res) => {
    try {
        const items = await Item.find();
        res.status(200).json(items);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// GET 單個道具
const getItemById = async (req, res) => {
    try {
        const id = req.params.id;

        // 檢查 id 是否為有效的 ObjectId
        if (!ObjectId.isValid(id)) {
            return res.status(400).json({ message: '無效的 ID 格式' });
        }

        // 使用 _id 進行查詢
        const item = await Item.findById(id);

        if (!item) {
            return res.status(404).json({ message: '找不到該道具' });
        }

        res.status(200).json(item);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

//使用道具
const useItem = async (req, res) => {
    const { userId, itemId } = req.params;
    const { requestId } = req.body || {};
    console.log('[items/use]', { userId, itemId, requestId });

    try {
        const result = await itemsService.useItem({ userId, itemId, requestId });
        res.json({ success: true, ...result });
    } catch (e) {
        console.error('useItem error:', e);
        res.status(400).json({ success: false, message: e.message });
    }
};

module.exports = {
    getAllItems,
    getItemById,
    useItem,
};
