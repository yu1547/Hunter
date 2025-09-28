import requests
import random
import json
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from llm_utils import embed

# 用 google api 查距離
def get_real_distance_google(lat1, lon1, lat2, lon2, api_key):
    url = "https://maps.googleapis.com/maps/api/directions/json"
    
    params = {
        "origin": f"{lat1},{lon1}",
        "destination": f"{lat2},{lon2}",
        "mode": "walking",
        "language": "zh-TW",
        "key": api_key
    }
    response = requests.get(url, params=params)
    data = response.json()

    if data["status"] == "OK":
        # 距離（公尺）
        distance_meters = data["routes"][0]["legs"][0]["distance"]["value"]
        # 預估時間（秒）
        duration_seconds = data["routes"][0]["legs"][0]["duration"]["value"]
        return distance_meters, duration_seconds
    else:
        raise Exception(f"Google API Error: {data['status']}")

def detect_route_intent(user_input: str, threshold: float = 0.75) -> bool:
    route_intent_examples = [
        "請幫我規劃一條路線",
        "請給我一條路線",
        "幫我規劃任務路線",
        "請給我一條路線規劃任務",
    ]
    user_embedding = embed(user_input)
    route_embeddings = [embed(text) for text in route_intent_examples]
    similarities = cosine_similarity([user_embedding], route_embeddings)[0]
    max_score = np.max(similarities)
    return max_score >= threshold

def classify_difficulty():
    """隨機決定難度：簡單(60%)、普通(30%)、困難(10%)"""
    return random.choices(["easy", "normal", "hard"], weights=[60, 30, 10], k=1)[0]

def generate_route_from_start(start_lat, start_lon, candidate_landmarks, api_key):
    """從指定起點生成一條路線到兩個隨機景點，並回傳 JSON 格式的任務"""
    # 從候選地標中隨機選擇2個
    if len(candidate_landmarks) < 2:
        raise ValueError("候選地標數量不足，至少需要2個。")
    selected_spots = random.sample(candidate_landmarks, 2)
    
    # 建立路線資訊
    start_point = {"spotId": "start", "spotName": "目前位置", "latitude": start_lat, "longitude": start_lon}
    route_points = [start_point] + selected_spots
    
    route_for_json = []
    total_distance = 0
    total_duration_seconds = 0

    # 計算各段距離與時間
    for i in range(len(route_points) - 1):
        p1 = route_points[i]
        p2 = route_points[i+1]
        
        dist, dur_sec = get_real_distance_google(
            p1["latitude"], p1["longitude"],
            p2["latitude"], p2["longitude"],
            api_key
        )
        # 新增：印出距離與時間
        print(f"get_real_distance_google -> 距離: {dist} 公尺, 時間: {dur_sec} 秒")

        total_distance += dist
        total_duration_seconds += dur_sec
        
        # 將起點之後的點加入路線
        route_for_json.append({"id": p2["spotId"], "name": p2["spotName"]})

    # 決定難度
    difficulty = classify_difficulty()

    # 根據難度計算任務時間
    if difficulty == "easy":
        task_duration = total_duration_seconds + 300  # +5 分鐘
    elif difficulty == "medium":
        task_duration = total_duration_seconds
    else:  # hard
        task_duration = total_duration_seconds - 180  # -3 分鐘
    
    # 確保任務時間至少為 60 秒
    task_duration = max(60, task_duration)

    # 建立最終的 JSON 物件
    result = {
        "taskName": "校園尋寶隨機任務",
        "taskDescription": f"從「目前位置」出發，依序前往「{selected_spots[0]['spotName']}」與「{selected_spots[1]['spotName']}」，探索校園風光。",
        "taskDifficulty": difficulty,
        "taskTarget": f"依序前往指定地點完成打卡任務，總共 {len(selected_spots)} 個地點。",
        "taskDuration": int(task_duration),
        "route": route_for_json
    }
    
    return result