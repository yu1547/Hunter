from flask import Flask, request, jsonify, session
import os
from dotenv import load_dotenv
from compare_api import compare_vector
import rag_v1
import json

app = Flask(__name__)
load_dotenv()
# 設定 Flask 的 session 密鑰
app.secret_key = os.getenv("FLASK_SECRET_KEY", "a-default-secret-key-for-development")
GOOGLE_API_KEY = os.getenv("GOOGLE_MAPS_API_KEY")

@app.route('/chat', methods=['POST'])
def chat():
    data = request.get_json()
    print("收到 /chat 請求，body:", data)
    # 支援 CSR_input.json 格式
    message = data.get("message")
    history = data.get("history", [])

    # 若前端傳舊格式（prompt），也能兼容
    if not message:
        message = data.get("prompt")
    if not message:
        return jsonify({"error": "Missing message"}), 400

    # 將 history 轉換為 LLM 需要的格式
    conversation_history = []
    for h in history:
        role = h.get("role")
        # LLM 內部用 "assistant" 不是 "LLM"
        if role == "LLM":
            role = "assistant"
        elif role == "user":
            role = "user"
        else:
            role = "user"
        conversation_history.append({
            "role": role,
            "content": h.get("content", "")
        })
    # 加入本次 user 輸入
    conversation_history.append({"role": "user", "content": message})

    try:
        # 呼叫 rag_v1 處理
        response_text = rag_v1.handle_chat_request(message, conversation_history)
        # 回傳 CSR_output.json 格式
        return jsonify({"reply": response_text})
    except Exception as e:
        import traceback
        print("Error in /chat:", e)
        traceback.print_exc()
        print("請求內容:", data)
        return jsonify({"error": str(e)}), 500

@app.route('/route', methods=['POST'])
def route():
    data = request.get_json()
    if not data:
        return jsonify({"error": "Invalid JSON"}), 400

    user_location = data.get("userLocation")
    candidate_landmarks = data.get("candidateLandmarks")
    enable_self_check = data.get("enable_self_check", True) # 允許前端控制是否開啟

    if not user_location or not candidate_landmarks:
        return jsonify({"error": "Missing userLocation or candidateLandmarks"}), 400

    if not GOOGLE_API_KEY:
        return jsonify({"error": "Missing GOOGLE_MAPS_API_KEY in environment"}), 500

    try:
        # 呼叫 rag_v1 中的路線處理函式
        mission_result = rag_v1.handle_route_request(user_location, candidate_landmarks, enable_self_check, GOOGLE_API_KEY)
        return jsonify(mission_result)
    except Exception as e:
        print("Error in /route:", e)  # 印出詳細錯誤訊息
        return jsonify({"error": str(e)}), 500


# 特徵比對
@app.route("/compare", methods=["POST"])
def compare():
    data = request.get_json(force=True)
    spotName = data.get("spotName")
    vector = data.get("vector")
    if not isinstance(spotName, str) or not isinstance(vector, list):
        return jsonify({"error": "invalid body"}), 400
    try:
        result = compare_vector(spotName, vector)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": "compare_failed", "detail": str(e)}), 500

if __name__ == '__main__':
    # try:
    #     with open("js_to_py.json", "r", encoding="utf-8") as f:
    #         test_data = json.load(f)

    #     with app.test_request_context('/route', method='POST', json=test_data):
    #         client = app.test_client()
    #         response = client.post('/route', json=test_data)
    #         print("\n--- Test Response for /route ---")
    #         print(json.dumps(response.get_json(), indent=2, ensure_ascii=False))
    #         print("------------------------------\n")
    # except FileNotFoundError:
    #     print("js_to_py.json not found, skipping /route test.")
    # except Exception as e:
    #     print(f"An error occurred during /route test request: {e}")
    app.run(host="0.0.0.0", port=5050, debug=True, use_reloader=False)