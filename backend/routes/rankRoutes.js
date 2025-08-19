const express = require('express');
const router = express.Router();
const { getRank } = require('../controllers/rankController');

router.get('/:userId', getRank);

module.exports = router;