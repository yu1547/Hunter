const express = require("express");
const router = express.Router();
const dropController = require("../controllers/dropController");

// POST /api/drop/:userId/:difficulty
router.post("/:userId/:difficulty", dropController.generateDrop);

module.exports = router;
