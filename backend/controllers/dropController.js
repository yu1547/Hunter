const dropService = require("../services/dropService");
//掉落機制
exports.generateDrop = async (req, res) => {
  const { userId, difficulty } = req.params;

  try {
    const drops = await dropService.generateDropForUser(userId, parseInt(difficulty));
    res.json({ success: true, drops });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
};
