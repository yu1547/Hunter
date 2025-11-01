# compare_api.py  (第一份策略 × 第二份介面)
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

    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()
    cursor.execute("SELECT label, feature FROM features")
    rows = cursor.fetchall()
    conn.close()

    if not rows:
        raise RuntimeError("數據庫中沒有特徵記錄")

    labels = []
    features = []
    for label, feature_bytes in rows:
        feature = np.frombuffer(feature_bytes, dtype=np.float32)
        labels.append(label)
        features.append(feature)

    return features, labels

# 快取
_DB_CACHE = {"features": None, "labels": None, "features_norm": None}

def _l2_normalize(v: np.ndarray, axis: int = -1, eps: float = 1e-8) -> np.ndarray:
    n = np.linalg.norm(v, axis=axis, keepdims=True)
    n = np.maximum(n, eps)
    return v / n

def ensure_cache():
    if _DB_CACHE["features"] is None:
        feats, labels = load_features_from_database(FEATURE_DB_PATH)
        _DB_CACHE["features"] = feats
        _DB_CACHE["labels"] = labels
        # 一次性正規化訓練特徵（符合第一份策略：先正規化，再用內積作餘弦）
        try:
            F = np.stack(feats, axis=0)  # (N, D)
        except Exception:
            # 維度不一致時退回逐筆處理
            F = None
        if F is not None:
            _DB_CACHE["features_norm"] = _l2_normalize(F, axis=1)  # (N, D)
        else:
            # 逐筆正規化
            _DB_CACHE["features_norm"] = np.stack([_l2_normalize(f[np.newaxis, :], axis=1).squeeze(0) for f in feats], axis=0)

def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    # 保留介面，但實際決策改用「先正規化後內積」以對齊第一份策略
    denom = float(np.linalg.norm(a) * np.linalg.norm(b))
    if denom == 0.0:
        return 0.0
    return float(np.dot(a, b) / denom)

def compare_vector(spotName: str, vector: List[float]):
    """
    介面與回傳格式維持第二份。
    策略採用第一份：類別最高相似度的比例門檻 1.01 + 絕對門檻 0.72。
    """
    ensure_cache()
    train_features = _DB_CACHE["features"]
    train_labels = _DB_CACHE["labels"]
    train_features_norm = _DB_CACHE["features_norm"]  # (N, D)

    q = np.asarray(vector, dtype=np.float32).reshape(1, -1)  # (1, D)
    q_norm = _l2_normalize(q, axis=1)  # (1, D)

    # 與全部訓練特徵的相似度：內積（等同餘弦）
    sims = (q_norm @ train_features_norm.T).astype(np.float32).ravel()  # (N,)

    # 依樣本排序
    idx_sorted = np.argsort(-sims)
    similarities: List[Tuple[str, float]] = [(train_labels[i], float(sims[i])) for i in idx_sorted]

    # 若完全無可用樣本
    if len(similarities) == 0:
        return {
            "predicted": "未知類別",
            "score": 0.0,
            "matched": False,
            "reason": "訓練集特徵為空"
        }

    # 樣本層級 Top-1（供 score 與資訊用）
    most_similar_class, highest_similarity = similarities[0]

    # 依「類別」聚合，取各類別最高相似度
    class_best = {}
    for label, sim in similarities:
        # 保留此類別的最高相似度
        if label not in class_best or sim > class_best[label]:
            class_best[label] = sim

    # 依「類別最高相似度」排序
    sorted_classes = sorted(class_best.items(), key=lambda x: x[1], reverse=True)

    # 取得最佳與次佳類別的最高相似度
    best_class, best_sim = sorted_classes[0]
    if len(sorted_classes) > 1:
        second_best_class, second_best_sim = sorted_classes[1]
    else:
        second_best_class, second_best_sim = "N/A", 0.0

    # 第一份策略參數
    similarity_ratio_threshold = 1.01
    absolute_similarity_threshold = 0.72

    # 比例門檻判斷
    if second_best_sim > 1e-8:
        similarity_ratio = best_sim / second_best_sim
        passed_ratio = similarity_ratio > similarity_ratio_threshold
        ratio_reason = f"最高類別/次高類別 = {best_sim:.4f}/{second_best_sim:.4f} = {similarity_ratio:.4f} {'>' if passed_ratio else '<='} {similarity_ratio_threshold}"
    else:
        passed_ratio = True  # 僅有一個類別或次高極低，視為通過
        ratio_reason = f"僅一類別或次高極低({second_best_sim:.4f})，視為通過比例門檻"

    # 絕對門檻判斷
    passed_abs = best_sim > absolute_similarity_threshold
    abs_reason = f"最高相似度 {best_sim:.4f} {'>' if passed_abs else '<='} {absolute_similarity_threshold}"

    # 綜合門檻
    if passed_ratio and passed_abs:
        predicted = best_class
        reason = f"{ratio_reason}，且 {abs_reason}"
    else:
        predicted = "未知類別"
        reason = f"{ratio_reason}，但 {abs_reason}，故拒答"

    return {
        "predicted": predicted,
        "score": float(highest_similarity),  # 與原系統一致，回報樣本層級的最高相似度
        "matched": bool(predicted == spotName),
        "reason": reason
    }
