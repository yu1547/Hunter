const Item = require('../models/itemModel');
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

// // POST 創建新道具
// const createItem = async (req, res) => {
//     try {
//         const newItem = new Item(req.body);
    
//         const savedItem = await newItem.save();
//         res.status(201).json(savedItem);
//     } catch (error) {
//         res.status(400).json({ message: error.message });
//     }
// };

// // PUT 更新道具
// const updateItem = async (req, res) => {
//     try {
//         const id = req.params.id;
//         const updatedItem = await Item.findByIdAndUpdate(
//             id,
//             req.body,
//             { new: true }
//         );
        
//         if (!updatedItem) {
//             return res.status(404).json({ message: '找不到該道具' });
//         }
        
//         res.status(200).json(updatedItem);
//     } catch (error) {
//         res.status(400).json({ message: error.message });
//     }
// };

// // DELETE 刪除道具
// const deleteItem = async (req, res) => {
//     try {
//         const id = req.params.id;
//         const deletedItem = await Item.findByIdAndDelete(id);
        
//         if (!deletedItem) {
//             return res.status(404).json({ message: '找不到該道具' });
//         }
        
//         res.status(200).json({ message: '道具已成功刪除' });
//     } catch (error) {
//         res.status(500).json({ message: error.message });
//     }
// };

module.exports = {
    getAllItems,
    getItemById,
    // createItem,
    // updateItem,
    // deleteItem,
};
