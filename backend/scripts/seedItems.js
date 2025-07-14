const mongoose = require('mongoose');
const dotenv = require('dotenv');
const Item = require('../models/Item');

dotenv.config();

const itemsData = [
  // 素材
    {
        itemId: 'copper_key_fragment',
        itemFunc: 'none',
        itemName: '銅鑰匙碎片',
        itemType: 0,
        itemEffect: '普通的鑰匙碎片，集齊3個可用來合成銅鑰匙。',
        maxStack: 99,
        itemMethod: 'randomEvent',
        itemRarity: 2,
        material: 'copper_key', // ✅ 新增
        isBlend: true,
        itemIcon: 'copper_key_fragment.png',
    },
    {
        itemId: 'silver_key_fragment',
        itemFunc: 'none',
        itemName: '銀鑰匙碎片',
        itemType: 0,
        itemEffect: '還不錯的鑰匙碎片，集齊3個可用來合成銀鑰匙。',
        maxStack: 99,
        itemMethod: 'randomEvent',
        itemRarity: 3,
        material: 'silver_key', // ✅ 新增
        isBlend: true,
        itemIcon: 'silver_key_fragment.png',
    },
    {
        itemId: 'gold_key_fragment',
        itemFunc: 'none',
        itemName: '金鑰匙碎片',
        itemType: 0,
        itemEffect: '稀有的鑰匙碎片，集齊3個可用來合成金鑰匙。',
        maxStack: 99,
        itemMethod: 'randomEvent',
        itemRarity: 4,
        material: 'gold_key', // ✅ 新增
        isBlend: true,
        itemIcon: 'gold_key_fragment.png',
    },
    {
        itemId: 'slime_gel_small',
        itemFunc: 'none',
        itemName: '普通的史萊姆黏液',
        itemType: 0,
        itemEffect: '史萊姆的掉落物。集齊3個可合成小史萊姆。',
        maxStack: 99,
        itemMethod: 'randomEvent',
        itemRarity: 1,
        material: 'small_slime', // ✅ 新增
        isBlend: true,
        itemIcon: 'slime_gel_small.png',
    },
    {
        itemId: 'slime_gel_big',
        itemFunc: 'none',
        itemName: '黏稠的史萊姆黏液',
        itemType: 0,
        itemEffect: '史萊姆的掉落物。集齊3個可合成大史萊姆。',
        maxStack: 99,
        itemMethod: 'randomEvent',
        itemRarity: 2,
        material: 'big_slime', // ✅ 新增
        isBlend: true,
        itemIcon: 'slime_gel_big.png',
    },
    {
        itemId: 'map_fragment',
        itemFunc: 'none',
        itemName: '散落的地圖殘片',
        itemType: 0,
        itemEffect: '看起來像某個人的日記。集齊3個可合成寶藏圖。',
        maxStack: 99,
        itemMethod: 'randomEvent',
        itemRarity: 4,
        material: 'treasure_map', // ✅ 新增
        isBlend: true,
        itemIcon: 'map_fragment.png',
    },
  {
    itemId: 'copper_key',
    itemFunc: 'unlock_copper_chest',
    itemName: '銅鑰匙',
    itemType: 1,
    itemEffect: '普通的鑰匙。可用來開啟銅寶箱。',
    maxStack: 1,
    itemMethod: 'chest',
    itemRarity: 3,
    material: null,
    isBlend: false,
    itemIcon: 'copper_key.png',
  },
  {
    itemId: 'silver_key',
    itemFunc: 'unlock_silver_chest',
    itemName: '銀鑰匙',
    itemType: 1,
    itemEffect: '還不錯的鑰匙。可用來開啟銀寶箱。',
    maxStack: 1,
    itemMethod: 'chest',
    itemRarity: 4,
    material: null,
    isBlend: false,
    itemIcon: 'silver_key.png',
  },
  {
    itemId: 'gold_key',
    itemFunc: 'unlock_gold_chest',
    itemName: '金鑰匙',
    itemType: 1,
    itemEffect: '稀有的鑰匙。可用來開啟金寶箱。',
    maxStack: 1,
    itemMethod: 'chest',
    itemRarity: 5,
    material: null,
    isBlend: false,
    itemIcon: 'gold_key.png',
  },
  {
    itemId: 'small_slime',
    itemFunc: 'summon_small_slime',
    itemName: '小史萊姆',
    itemType: 1,
    itemEffect: '使用後可召喚小史萊姆為你尋找鑰匙碎片，找到後自動消失。',
    maxStack: 1,
    itemMethod: 'npc',
    itemRarity: 2,
    material: null,
    isBlend: false,
    itemIcon: 'small_slime.png',
  },
  {
    itemId: 'big_slime',
    itemFunc: 'summon_big_slime',
    itemName: '大史萊姆',
    itemType: 1,
    itemEffect: '使用後可召喚大史萊姆為你尋找鑰匙碎片，找到後自動消失。',
    maxStack: 1,
    itemMethod: 'npc',
    itemRarity: 3,
    material: null,
    isBlend: false,
    itemIcon: 'big_slime.png',
  },
  {
    itemId: 'treasure_map',
    itemFunc: 'trigger_treasure_event',
    itemName: '寶藏圖',
    itemType: 1,
    itemEffect: '使用後立即觸發金色寶箱事件。',
    maxStack: 1,
    itemMethod: 'randomEvent',
    itemRarity: 5,
    material: null,
    isBlend: false,
    itemIcon: 'treasure_map.png',
  },

  // 一般消耗物（不可合成）
  {
    itemId:'hourglass_speedup',
    itemFunc: 'speed_up_task',
    itemName: '時間沙漏 - 加速',
    itemType: 1,
    itemEffect: '立即刷新一項任務，不需要等待刷新時間。',
    maxStack: 1,
    itemMethod: 'npc',
    itemRarity: 3,
    material: null,
    isBlend: false,
    itemIcon: 'hourglass_speedup.png',
  },
  {
    itemId: 'hourglass_slowdown',
    itemFunc: 'extend_task_timer',
    itemName: '時間沙漏 - 減速',
    itemType: 1,
    itemEffect: '立即增加當前限時任務的時間15 min。',
    maxStack: 1,
    itemMethod: 'npc',
    itemRarity: 3,
    material: null,
    isBlend: false,
    itemIcon: 'hourglass_slowdown.png',
  },
  {
    itemId: 'torch',
    itemFunc: 'boost_damage_slime',
    itemName: '火把',
    itemType: 1,
    itemEffect: '史萊姆的剋星。使用後提高對史萊姆造成的傷害。',
    maxStack: 1,
    itemMethod: 'npc',
    itemRarity: 2,
    material: null,
    isBlend: false,
    itemIcon: 'torch.png',
  },
  {
    itemId: 'ancient_branch',
    itemFunc: 'boost_loot_quality',
    itemName: '古樹的枝幹',
    itemType: 1,
    itemEffect: '古樹的祝福。使用後2h內大幅提高寶箱或補給站獲得的物品數量及品質。',
    maxStack: 1,
    itemMethod: 'npc',
    itemRarity: 5,
    material: null,
    isBlend: false,
    itemIcon: 'ancient_branch.png',
  },
];

async function seedItems() {
  try {
    await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/your-game-db', {
      useNewUrlParser: true,
      useUnifiedTopology: true,
    });

    console.log('MongoDB connected');

    await Item.deleteMany({});
    console.log('舊的道具資料已刪除');

    await Item.insertMany(itemsData);
    console.log('✅ 基礎道具資料已建立完成！');

    process.exit(0);
  } catch (error) {
    console.error('❌ 道具初始化失敗:', error);
    process.exit(1);
  }
}

seedItems();
