const User = require('../models/userModel');
const Spot = require('../models/spotModel');
exports.updateUserRecognition = async (userId, spotName) => {
    const user = await User.findById(userId);
    if (!user) throw new Error("找不到使用者");

    console.log("更新 spotsScanLogs");

    // 1️ 更新 spotsScanLogs
    if (user.spotsScanLogs instanceof Map && user.spotsScanLogs.has(spotName)) {
        user.spotsScanLogs.set(spotName, true);
        console.log("---");
    }

    // if (user.spotsScanLogs && spotName in user.spotsScanLogs) {
    //     user.spotsScanLogs[spotName] = true;
    //     console.log("---");
    // }

    console.log("spotName", spotName);
    console.log("user.spotsScanLogs ", user.spotsScanLogs);
    console.log("spotName in user.spotsScanLogs:", user.spotsScanLogs.has(spotName));

    console.log("更新 claimed 任務中的 haveCheckPlaces");

    // 2. 查詢 spotName 對應的 spotId
    const spot = await Spot.findOne({ spotName });
    if (!spot) throw new Error("找不到對應的 Spot");

    const targetSpotId = spot._id.toString();

    // 3. 更新 mission 中符合的 isCheck
    if (Array.isArray(user.missions)) {
        user.missions.forEach((mission) => {
            if (mission.state === 'claimed' && Array.isArray(mission.haveCheckPlaces)) {
                mission.haveCheckPlaces.forEach((place) => {
                    const spotId = place.spotId?.toString();
                    if (spotId === targetSpotId) {
                        place.isCheck = true;
                    }
                });
            }
        });
    }
    await user.save();
    return user.spotsScanLogs;
};
