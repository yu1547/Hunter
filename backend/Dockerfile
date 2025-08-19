# 使用官方 Node.js 18 映像檔
FROM node:18-alpine

# 在容器中建立並設定工作目錄
WORKDIR /usr/src/app

# 複製 package.json 和 package-lock.json (如果存在)
COPY package*.json ./

# 安裝專案依賴
RUN npm install

# 複製專案的其餘檔案
COPY . .

# 開放容器的 4000 連接埠
EXPOSE 4000

# 執行啟動指令
CMD [ "npm", "start" ]
