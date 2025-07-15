module.exports = [
    {
        difficulty: 1, // 補給站
        dropCountRange: [3, 4],
        rarityChances: {
            1: 70,
            2: 25,
            3: 5
        },
        guaranteedRarity: null,
        poolType: "testType"
    },
    {
        difficulty: 2,
        dropCountRange: [3, 4],
        rarityChances: {
            1: 70,
            2: 20,
            3: 10
        },
        guaranteedRarity: 2,
        poolType: "testType"
    },
    {
        difficulty: 3, // 銅寶箱
        dropCountRange: [3, 5],
        rarityChances: {
            2: 50,
            3: 20,
            4: 10
        },
        guaranteedRarity: 3,
        poolType: "testType"
    },
    {
        difficulty: 4, // 銀寶箱
        dropCountRange: [3, 5],
        rarityChances: {
            2: 30,
            3: 30,
            4: 20,
            5: 10
        },
        guaranteedRarity: 4,
        poolType: "testType"
    },
    {
        difficulty: 5, // 金寶箱
        dropCountRange: [3, 5],
        rarityChances: {
            3: 50,
            4: 30,
            5: 20
        },
        guaranteedRarity: 5,
        poolType: "testType"
    }
];
