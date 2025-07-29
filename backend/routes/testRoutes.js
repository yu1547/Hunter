const express = require('express');
const router = express.Router();
const DropRules = require('../models/dropRulesModel');

router.get('/test-rules', async (req, res) => {
    try {
        const rules = await DropRules.find({});
        res.json(rules);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

module.exports = router;
