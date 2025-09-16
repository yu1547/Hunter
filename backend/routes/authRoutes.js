const express = require("express");
const router = express.Router();
const User = require("../models/userModel"); // ✅ 確保這個檔案存在

router.post("/google", async (req, res) => {
    const { email } = req.body;

    console.log("📨 收到 Google 登入請求:", req.body);

    if (!email) {
        console.warn("⚠️ 缺少 email");
        return res.status(400).json({ message: "缺少 email" });
    }

    try {
        let user = await User.findOne({ email });

        if (!user) {
            console.log("🆕 建立新使用者:", email);
            user = new User({
                email,
                displayName: "",
                age: "",
                gender: "",
                photoURL: "",
                role: "player",
                createdAt: new Date(),
                lastLogin: new Date(),
                backpackItems: [],
                missions: [],
                spotsScanLogs: {},
                supplyScanLogs: {},
                settings: [],
                buff: null,
                username: ""
            });

            await user.save();
            console.log("✅ 使用者已儲存:", user._id);
        } else {
            console.log("🔁 使用者已存在:", user.email);
            user.lastLogin = new Date();
            await user.save();
            console.log("🕒 更新 lastLogin");
        }

        res.json({ message: "登入成功", user });
    } catch (err) {
        console.error("❌ MongoDB 操作錯誤:", err);
        res.status(500).json({ message: "伺服器錯誤" });
    }
});

module.exports = router;
