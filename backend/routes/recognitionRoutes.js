const express = require('express');
const router = express.Router();
const recognitionController = require('../controllers/recognitionController');


router.post('/true', recognitionController.handleRecognitionTrue); // 測試用，假設辨識成功，後續刪除
router.post('/', recognitionController.handleRecognition);

module.exports = router;
