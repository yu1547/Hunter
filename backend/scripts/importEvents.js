const mongoose = require('mongoose');
const fs = require('fs');
const Event = require('../models/eventModel');
const events = JSON.parse(fs.readFileSync(`${__dirname}/../data/sampleEvents.json`, 'utf-8'));

const importData = async () => {
  try {
    await Event.deleteMany();
    await Event.insertMany(events);
    console.log('Events data successfully imported!');
    process.exit();
  } catch (err) {
    console.error(err);
    process.exit(1);
  }
};