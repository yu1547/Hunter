from flask import Flask, request, jsonify, session
import os
from dotenv import load_dotenv
import rag_v1
import json

app = Flask(__name__)
load_dotenv()
# Session 需要一個 secret key
app.secret_key = os.getenv("FLASK_SECRET_KEY", "a-default-secret-key-for-development")

@app.route('/chat', methods=['POST'])
def chat():
    data = request.get_json()
    prompt = data.get("prompt")
    # 從 session 中獲取對話歷史，如果沒有則初始化
    conversation_history = session.get('conversation_history', [])
    
    if not prompt:
        return jsonify({"error": "Missing prompt"}), 400

    conversation_history.append({"role": "user", "content": prompt})

    # 呼叫 rag_v1 中的聊天處理函式
    response_text = rag_v1.handle_chat_request(prompt, conversation_history)
    
    conversation_history.append({"role": "assistant", "content": response_text})
    # 更新 session 中的對話歷史
    session['conversation_history'] = conversation_history

    return jsonify({"response": response_text})

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

    try:
        # 呼叫 rag_v1 中的路線處理函式
        mission_result = rag_v1.handle_route_request(user_location, candidate_landmarks, enable_self_check)
        return jsonify(mission_result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    # 模擬來自 js_to_py.json 的請求進行測試
    try:
        with open("js_to_py.json", "r", encoding="utf-8") as f:
            test_data = json.load(f)
        
        with app.test_request_context('/route', method='POST', json=test_data):
            client = app.test_client()
            response = client.post('/route', json=test_data)
            print("\n--- Test Response for /route ---")
            print(json.dumps(response.get_json(), indent=2, ensure_ascii=False))
            print("------------------------------\n")
    except FileNotFoundError:
        print("js_to_py.json not found, skipping /route test.")
    except Exception as e:
        print(f"An error occurred during /route test request: {e}")

    app.run(host="0.0.0.0", port=5000, debug=True)