//測試：npm run test:drop
const { generateDropForUser } = require("../services/dropService");

const test = () => {
    const drops = generateDropForUser("user001", 1, ); ;
    console.log("本次掉落：", drops);
};

test();
