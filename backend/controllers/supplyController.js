const supplyService = require('../services/supplyService');

exports.getAll = async (req, res) => {
    try {
        const supplies = await supplyService.getAllSupplies();
        console.log("執行getAllSupplies");
        res.json({ success: true, data: supplies });
    } catch (err) {
        console.error('取得補給站失敗：', err);
        res.status(500).json({ success: false, message: '內部錯誤' });
    }
};

exports.claim = async (req, res) => {
    const { userId, supplyId } = req.params;
    try {
        const result = await supplyService.claimSupply({ userId, supplyId });
        res.json(result);
    } catch (err) {
        console.error('補給站領取錯誤：', err);
        res.status(500).json({ success: false, message: '內部錯誤' });
    }
};
