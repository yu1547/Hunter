# 使用官方 Python 映像檔作為基礎
FROM python:3.9-slim

# 設定工作目錄
WORKDIR /app

# 複製 LLM 目錄下的所有檔案到容器的 /app 目錄
COPY . .

# 安裝 requirements.txt 中定義的依賴套件
RUN pip install --no-cache-dir -r requirements.txt

# 設定 OLLAMA_HOST 讓容器能連線主機上的 Ollama
ENV OLLAMA_HOST=http://host.docker.internal:11434

# 開放容器的 5000 連接埠
EXPOSE 5000

# 執行 app.py
CMD ["python", "app.py"]
