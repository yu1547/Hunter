const User = require("../models/userModel");
const Spot = require("../models/spotModel");

// 取得特定使用者的打卡紀錄
const getSpotsScanLogs = async (req, res) => {
    try {
        const { userId } = req.params;
        const user = await User.findById(userId);

        if (!user) {
            return res.status(404).json({ success: false, message: "使用者不存在" });
        }
        console.log("執行getSpotsScanLogs");

        return res.status(200).json({ success: true, spotsScanLogs: user.spotsScanLogs });
    } catch (error) {
        console.error("取得打卡紀錄錯誤：", error);
        return res.status(500).json({ success: false, message: "伺服器錯誤" });
    }
};


//取得所有的spot點位
const getAllSpots = async (req, res) => {
    try {
        const spots = await Spot.find({}, "-__v"); // 排除 __v 欄位（可選）
        console.log("執行getAllSpots");
        res.status(200).json({
            success: true,
            spots,
        });
    } catch (error) {
        console.error("Error fetching spots:", error);
        res.status(500).json({ success: false, message: "取得打卡點失敗" });
    }
};

module.exports = {
    getSpotsScanLogs,
    getAllSpots,
};

