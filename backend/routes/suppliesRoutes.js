const express = require('express');
const router = express.Router();
const supplyController = require('../controllers/supplyController');

router.get('/', supplyController.getAll);
router.post('/claim/:userId/:supplyId', supplyController.claim); 
router.get('/status/:userId/:supplyId', supplyController.status);//查詢補給站冷卻狀態
module.exports = router;
