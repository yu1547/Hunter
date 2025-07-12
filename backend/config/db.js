const mongoose = require('mongoose');

const connectDB = async () => {
  try {
    console.log("⛳ MONGO_URI:", process.env.MONGO_URI); // 確認 .env 有載入
    await mongoose.connect(process.env.MONGO_URI);
    console.log('✅ MongoDB connected');
  } catch (err) {
    console.error('❌ MongoDB connection error:', err.message);
    process.exit(1);
  }
};

module.exports = connectDB;
