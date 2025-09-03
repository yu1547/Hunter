import ollama
import json
import os
import logging
from dotenv import load_dotenv
from llm_utils import (
    get_self_check_prompt,
    parse_paragraph,
    cache_embeddings,
    calc_similar_vectors,
    classify_question_type,
    build_instance_adaptive_prompt,
    summarize_conversation,
)
from route_utils import detect_route_intent, generate_route_from_start

load_dotenv()

# 設定 logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 初始化 RAG 所需的資源
doc = "rule.txt"
paragraphs = None
embeddings = None

def get_embeddings():
    global paragraphs, embeddings
    if paragraphs is None or embeddings is None:
        paragraphs = parse_paragraph(doc)
        embeddings = cache_embeddings(doc, paragraphs)
    return paragraphs, embeddings

api_key = os.getenv("GOOGLE_API_KEY")

def handle_chat_request(prompt, conversation_history, enable_self_check=True):
    """處理聊天請求，包含 RAG 和自我檢查邏輯"""
    try:
        paragraphs, embeddings = get_embeddings()
        question_type = classify_question_type(prompt)
        logger.info(f"🧠 問題分類結果：{question_type}")

        try:
            prompt_embedding = ollama.embeddings(model="ycchen/breeze-7b-instruct-v1_0", prompt=prompt)["embedding"]
        except Exception as e:
            logger.error(f"❌ LLM embeddings 發生錯誤: {e}")
            return f"LLM embeddings 發生錯誤: {e}"

        try:
            similar_vectors = calc_similar_vectors(prompt_embedding, embeddings)[:3]
            valid_vectors = [v for v in similar_vectors if v[0] < len(paragraphs)]

            memory_summary = None
            if len(conversation_history) >= 2:
                memory_summary = summarize_conversation(conversation_history[-4:])

            system_prompt = build_instance_adaptive_prompt(paragraphs, valid_vectors, question_type, memory_summary)
            messages = [{"role": "system", "content": system_prompt}] + conversation_history

            if enable_self_check:
                max_attempts = 3
                attempt = 1
                final_response = None
                error_feedback = None
                while attempt <= max_attempts:
                    if error_feedback:
                        retry_hint = f"上次回覆被判定為不合格，原因是：{error_feedback.strip()}。\n請遵守規則，請勿再犯下同樣的錯誤。\n\n"
                    else:
                        retry_hint = ""
                    modified_system_prompt = retry_hint + system_prompt
                    current_messages = [{"role": "system", "content": modified_system_prompt}] + conversation_history
                    try:
                        response = ollama.chat(model="ycchen/breeze-7b-instruct-v1_0", messages=current_messages)["message"]["content"]
                    except Exception as e:
                        logger.error(f"❌ LLM chat 發生錯誤: {e}")
                        return f"LLM chat 發生錯誤: {e}"

                    logger.info(f"\n🗨️ 回覆內容（第 {attempt} 次嘗試）:\n{response}\n")

                    # 呼叫 get_self_check_prompt
                    check_response_prompt = get_self_check_prompt(question_type, response)
                    try:
                        audit_result = ollama.chat(
                            model="ycchen/breeze-7b-instruct-v1_0",
                            messages=[{"role": "user", "content": check_response_prompt}]
                        )["message"]["content"]
                    except Exception as e:
                        logger.error(f"❌ LLM 自我檢查 chat 發生錯誤: {e}")
                        return f"LLM 自我檢查 chat 發生錯誤: {e}"

                    logger.info(f"🧪 自我檢查結果：{audit_result.strip()}\n")

                    if "不合格" in audit_result:
                        logger.warning("❌ 不合格，重新生成新的回答...")
                        error_feedback = audit_result.replace("不合格：", "").strip()
                        attempt += 1
                    else:
                        final_response = response
                        break

                if final_response is None:
                    logger.warning("[⚠️ 最多重試次數已達，回答我不清楚遊戲以外的內容]")
                    final_response = "我不清楚遊戲以外的內容"
                return final_response
            else:
                try:
                    response = ollama.chat(model="ycchen/breeze-7b-instruct-v1_0", messages=messages)["message"]["content"]
                except Exception as e:
                    logger.error(f"❌ LLM chat 發生錯誤: {e}")
                    return f"LLM chat 發生錯誤: {e}"
                logger.info(f"\n🗨️ 回覆內容：\n{response}\n")
                return response
        except Exception as e:
            logger.error(f"❌ handle_chat_request 內部流程錯誤: {e}")
            return f"handle_chat_request 內部流程錯誤: {e}"
    except Exception as e:
        logger.error(f"❌ handle_chat_request 發生未預期錯誤: {e}")
        return f"handle_chat_request 發生未預期錯誤: {e}"

def handle_route_request(user_location, candidate_landmarks, enable_self_check=True,api_key=None):
    """處理路線規劃請求"""
    try:
        start_lat = user_location.get("latitude")
        start_lon = user_location.get("longitude")

        if not all([start_lat, start_lon, api_key, candidate_landmarks]):
            raise ValueError("缺少生成路線所需的參數（經緯度、API金鑰或候選地標）。")

        try:
            response_data = generate_route_from_start(start_lat, start_lon, candidate_landmarks, api_key=api_key)
            logger.info(f"\n🗺️ 路線規劃結果：\n{json.dumps(response_data, ensure_ascii=False, indent=2)}\n")
            return response_data
        except Exception as e:
            logger.error(f"\n❌ 路線規劃時發生錯誤：{e}\n")
            return {"error": f"路線規劃時發生錯誤: {e}"}
    except Exception as e:
        logger.error(f"❌ handle_route_request 發生未預期錯誤: {e}")
        return {"error": f"handle_route_request 發生未預期錯誤: {e}"}
