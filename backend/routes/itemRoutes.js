const express = require('express');
const router = express.Router();
const itemsController = require('../controllers/itemController');
const {
  getAllItems,
  getItemById,
  createItem,
  updateItem,
  deleteItem,
} = require('../controllers/itemController');


router.get('/', getAllItems);     // GET 所有道具
router.get('/:id', getItemById);  // GET 單個道具
router.post('/use/:userId/:itemId', itemsController.useItem); // 使用道具：/api/items/use/:userId/:itemId
// router.post('/', createItem);     // POST 創建新道具
// router.put('/:id', updateItem);   // PUT 更新道具
// router.delete('/:id', deleteItem); // DELETE 刪除道具

module.exports = router;