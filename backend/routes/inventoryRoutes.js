const express = require('express');
const router = express.Router();
const inventoryController = require('../controllers/inventoryController');

router.get('/:userId/inventory', inventoryController.getInventory);
router.post('/:userId/inventory/add', inventoryController.addItem);
router.post('/:userId/inventory/remove', inventoryController.removeItem);
router.post('/:userId/inventory/use', inventoryController.useItem);

module.exports = router;
