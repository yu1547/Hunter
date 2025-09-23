const express = require("express");
const router = express.Router();
const admin = require("firebase-admin"); // 後端驗證 Token
const User = require("../models/userModel"); 

router.post("/google", async (req, res) => {
    const { idToken } = req.body; // 前端傳 ID Token

    console.log("📨 收到 Google 登入請求:", req.body);

    if (!idToken) {
        console.warn("⚠️ 缺少 idToken");
        return res.status(400).json({ message: "缺少 idToken" });
    }

    try {
        // ✅ 驗證 ID Token
        const decodedToken = await admin.auth().verifyIdToken(idToken);
        const email = decodedToken.email;
        const uid = decodedToken.uid;

        console.log("✅ Firebase 驗證成功:", email, uid);

        // ✅ 查找或建立使用者
        let user = await User.findOne({ email });

        if (!user) {
            console.log("🆕 建立新使用者:", email);
            user = new User({
                email,
                firebaseUid: uid,
                displayName: decodedToken.name || "",
                age: "20",
                gender: "不透露",
                photoURL: decodedToken.picture || "",
                role: "player",
                createdAt: new Date(),
                lastLogin: new Date(),
                backpackItems: [],
                missions: [],
                spotsScanLogs: {
                    anchor: false,
                    ball: false,
                    eagle: false,
                    lovechair: false,
                    moai: false,
                    vending: false,
                    book: false,
                    bookcase: false,
                    freedomship: false,
                    fountain: false
                },
                supplyScanLogs: {},
                settings: {
                    music: false,
                    notification: false,
                    language: "zh-TW"
                },
                buff: null,
                username: "user"
            });

            await user.save();
            console.log("✅ 使用者已儲存:", user._id);
        }
        else {
            console.log("🔁 使用者已存在:", user.email);
            user.lastLogin = new Date();
            await user.save();
            console.log("🕒 更新 lastLogin");
        }

        res.json({ message: "登入成功", user });
    } catch (err) {
        console.error("❌ Firebase 驗證錯誤:", err);
        res.status(401).json({ message: "ID Token 無效或過期" });
    }
});

module.exports = router;
