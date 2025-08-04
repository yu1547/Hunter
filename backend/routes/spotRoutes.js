const express = require("express");
const router = express.Router();
const { getSpotsScanLogs } = require("../controllers/spotController");

// GET /api/spots/:userId
router.get("/:userId", getSpotsScanLogs);

module.exports = router;
