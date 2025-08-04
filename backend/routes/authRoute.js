const express = require("express");
const router = express.Router();
const User = require("../models/User"); // 你的 User schema

router.post("/google", async (req, res) => {
    const { email } = req.body;

    if (!email) return res.status(400).json({ message: "缺少 email" });

    try {
        let user = await User.findOne({ email });

        if (!user) {
            // 如果找不到就創建新使用者
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
                missions: null,
                spotsScanLogs: {},
                supplyScanLogs: {},
                settings: [],
                buff: null,
                username: ""
            });

            await user.save();
        } else {
            // 如果已存在則更新登入時間
            user.lastLogin = new Date();
            await user.save();
        }

        res.json({ message: "登入成功", user });
    } catch (err) {
        console.error(err);
        res.status(500).json({ message: "伺服器錯誤" });
    }
});

module.exports = router;
