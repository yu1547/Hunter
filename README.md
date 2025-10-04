# Hunter（寶藏獵人）- 結合場景辨識與 LLM 之校園導覽系統

Hunter 是一個以「寶藏獵人」為主題的專案。本倉庫包含前端與後端兩個主要模組，並使用多種技術實作，適合用於學習、擴充或進一步打造完整應用。

---

## 目錄
- [專案簡介](#專案簡介)
- [功能特色](#功能特色)
- [專案結構](#專案結構)
- [技術與語言](#技術與語言)
- [Docker](#docker)

---

## 專案簡介
- 名稱：Hunter（寶藏獵人）
- 目標：構建一個以「尋寶」為概念的應用／服務（包含前端介面與後端服務）
- 狀態：持續開發中

---

## 功能特色
- 使用者登入／收藏／喜好設定
- 利用場景辨識實作打卡比對地標
- LLM 作為客服人員提供關於遊戲之回應
- 排行比較讓遊戲更具競爭力
- 完成任務以獲得更多有幫助的道具

---

## 專案結構
```text
.
├─ backend/          # 後端服務（語言/框架請依實作補充說明）
├─ frontend/         # 前端專案（如 Web 前端）
└─ README.md
```

---

## 技術與語言
- Kotlin：前端
- JavaScript：後端語言
- Python：處理路線規劃、場景辨識、LLM 回應
- Dockerfile：容器化部署設定

---

## Docker
若欲以 Docker 執行，請於存在 Dockerfile 的對應目錄中執行（範例）：
```bash
# 於 backend/ 或 frontend/ 等包含 Dockerfile 的目錄
docker build -t hunter-backend:latest .
docker run --rm -p 8000:8000 --env-file .env hunter-backend:latest
```
如有 Compose 範例，可新增 `docker-compose.yml` 並附上啟動指令：
```bash
docker compose up -d
```

