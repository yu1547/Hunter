const User = require("../models/userModel");

// 取得特定使用者的打卡紀錄
const getSpotsScanLogs = async (req, res) => {
    try {
        const { userId } = req.params;
        const user = await User.findById(userId);

        if (!user) {
            return res.status(404).json({ success: false, message: "使用者不存在" });
        }

        return res.status(200).json({ success: true, spotsScanLogs: user.spotsScanLogs });
    } catch (error) {
        console.error("取得打卡紀錄錯誤：", error);
        return res.status(500).json({ success: false, message: "伺服器錯誤" });
    }
};

module.exports = { getSpotsScanLogs };
