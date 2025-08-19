# compare_api.py
import os
import sqlite3
import numpy as np
from typing import List, Tuple

FEATURE_DB_PATH = os.getenv("FEATURE_DB_PATH", "/app/data/train_features.db")

# 從SQLite數據庫讀取特徵數據
def load_features_from_database(db_file):
    """從SQLite數據庫讀取特徵數據"""
    if not os.path.exists(db_file):
        raise FileNotFoundError(f"錯誤：特徵數據庫 {db_file} 不存在")

    # 連接到SQLite數據庫
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()

    # 讀取所有數據
    cursor.execute("SELECT label, feature FROM features")
    rows = cursor.fetchall()
    conn.close()

    if not rows:
        raise RuntimeError("數據庫中沒有特徵記錄")

    labels = []
    features = []
    for label, feature_bytes in rows:
        # 將二進制數據轉換回numpy數組
        feature = np.frombuffer(feature_bytes, dtype=np.float32)
        labels.append(label)
        features.append(feature)

    return features, labels

# 啟動時載入到記憶體，避免每次 I/O
_DB_CACHE = {"features": None, "labels": None}

def ensure_cache():
    if _DB_CACHE["features"] is None:
        feats, labels = load_features_from_database(FEATURE_DB_PATH)
        _DB_CACHE["features"] = feats
        _DB_CACHE["labels"] = labels

def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    # 余弦相似度計算: cos(θ) = A·B / (||A||·||B||)
    denom = float(np.linalg.norm(a) * np.linalg.norm(b))
    if denom == 0.0:
        return 0.0
    return float(np.dot(a, b) / denom)

def compare_vector(spotName: str, vector: List[float]):
    """
    預測並回傳結果（HTTP API 版本，不做影像讀取）
    - 實現雙重閾值判斷邏輯
    - 中等可信度：按類別分組，比較不同類別間的相似度差距
    """
    ensure_cache()
    train_features = _DB_CACHE["features"]
    train_labels = _DB_CACHE["labels"]

    q = np.asarray(vector, dtype=np.float32)

    # 計算與訓練集中所有特徵的余弦相似度
    similarities: List[Tuple[str, float]] = []
    for idx, train_feature in enumerate(train_features):
        # 余弦相似度計算: cos(θ) = A·B / (||A||·||B||)
        sim = cosine_similarity(q, train_feature)
        similarities.append((train_labels[idx], sim))

    # 根據相似度排序
    similarities.sort(key=lambda x: x[1], reverse=True)

    # 選擇第一個最相似的結果
    similar_images = similarities[:1]
    if not similar_images:
        # 無法找到相似的向量 -> 視為未知類別
        most_similar_class = "未知類別"
        highest_similarity = 0.0
        prediction_confidence = "低可信度"
        prediction_reason = "無法找到相似的向量"
        return {
            "predicted": most_similar_class,
            "score": float(highest_similarity),
            "matched": False,
            "reason": prediction_reason
        }

    # 找出最相似的結果（第一個結果）
    most_similar_class, highest_similarity = similar_images[0]

    # 實現雙重閾值判斷邏輯
    prediction_confidence = "未知"
    prediction_reason = ""

    if highest_similarity > 0.8:
        # 高可信度 - 直接採用最相似圖片的類別
        prediction_confidence = "高可信度"
        prediction_reason = f"相似度 {highest_similarity:.4f} > 0.8"
        predicted = most_similar_class

    elif highest_similarity < 0.7:
        # 低可信度 - 判定為未知類別
        predicted = "未知類別"
        prediction_confidence = "低可信度"
        prediction_reason = f"相似度 {highest_similarity:.4f} < 0.7"

    else:
        # 中等可信度 - 按類別分組，比較不同類別間的相似度差距
        class_best = {}
        # 將相似圖像按類別分組，每個類別只保留最高相似度
        for label, sim in similarities:
            if label not in class_best or sim > class_best[label][1]:
                class_best[label] = (label, sim)

        # 按相似度排序類別
        sorted_classes = sorted([(label, sim) for label, (_, sim) in class_best.items()],
                               key=lambda x: x[1], reverse=True)

        # 如果只有一個類別，則採用該類別
        if len(sorted_classes) == 1:
            best_class, best_sim = sorted_classes[0]
            predicted = best_class
            prediction_confidence = "中可信度-採用"
            prediction_reason = "僅有一個匹配類別"
        else:
            # 獲取最高相似度的類別 (A) 和次高相似度的類別 (B)
            best_class, best_sim = sorted_classes[0]
            second_best_class, second_best_sim = sorted_classes[1]
            similarity_gap = best_sim - second_best_sim

            # 若最佳與次佳類別相似度差距大於0.1，採用最佳結果
            if similarity_gap > 0.1:
                predicted = best_class
                prediction_confidence = "中可信度-採用"
                prediction_reason = (
                    f"類別間相似度差距 {similarity_gap:.4f} > 0.1 "
                    f"(最佳:{best_class}={best_sim:.4f}, 次佳:{second_best_class}={second_best_sim:.4f})"
                )
            else:
                predicted = "未知類別"
                prediction_confidence = "中可信度-拒绝"
                prediction_reason = (
                    f"類別間相似度差距 {similarity_gap:.4f} <= 0.1 "
                    f"(最佳:{best_class}={best_sim:.4f}, 次佳:{second_best_class}={second_best_sim:.4f})"
                )

    return {
        "predicted": predicted,
        "score": float(highest_similarity),  # 與原腳本一致，輸出最高相似度
        "matched": bool(predicted == spotName),
        "reason": prediction_reason
    }
