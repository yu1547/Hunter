const express = require("express");
const router = express.Router();
const { getSpotsScanLogs, getAllSpots } = require("../controllers/spotController");

// GET /api/spots
//取得所有spots地點
router.get("/", getAllSpots);

// GET /api/spots/:userId
//取的特定user的打卡點解鎖(收藏冊用)
router.get("/:userId", getSpotsScanLogs);



module.exports = router;
