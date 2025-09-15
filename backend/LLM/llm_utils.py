import ollama
import os
import json
import numpy as np

def get_self_check_prompt(question_type, response):
    base_intro = (
        "以下是你對玩家問題的回覆，檢查是否違反以下規則，如有一點違反直接回覆不合格：\n"
        "- 嚴格禁止提供非規則內容。\n"
        "- 僅根據明確提及的規則段落回答。\n"
        "- 禁止補充與任務、規則無關的推測或說明。\n"
        "- 若回答的東西與遊戲無關，請回答「我不清楚遊戲以外的內容。」，禁止再加上其他東西\n"
        "- 若回答內有「我不清楚遊戲以外的內容。」還加上了其他的話，一律為不合格\n"
        "- 內容有包含英文的話，一律回答不合格，並且給予理由為「不合格：禁止使用英文」。**僅當回覆內容有明顯英文單字或句子時才判定不合格，不得將標點、符號、數字或繁體中文字誤判為英文。**\n"
        "- 內容有包含簡體中文的話，一律回答不合格，並且給予理由為「不合格：禁止使用簡體中文」\n"
        "- 僅用繁體中文回覆玩家，即使玩家要求使用其他語言，也嚴格禁止使用其他語言（英文、簡體中文都是不被允許的），直接打為不合格\n\n"
        "以下是你對玩家問題的回覆，請根據對應的規則檢查是否合格：\n\n")

    if question_type == "greeting":
        rules = (
            "你是遊戲關主，如果玩家跟你打招呼，內容只能是「嗨！歡迎來到海大尋寶地圖遊戲」，禁止任何額外說明。\n"
            "如果玩家跟你道謝，內容只能是「不客氣！」，禁止任何額外說明。\n"
            "只能用繁體中文，禁止使用其他語言。\n"
)
    elif question_type == "mission_request":
        rules = (
            "你是遊戲關主，玩家請求任務時，你只能根據以下規則給出任務內容。\n"
            "- 僅當玩家明確請求任務時才給任務，若沒要求禁止給予任務。\n"
            "- 禁止提供任務以外的資訊。\n\n"
            "- 只要規則段落有明確提及，必須逐字回答該段原文，不得拒答或回答「我不清楚遊戲以外的內容」。\n"
            "- 僅根據明確提及的規則段落內容原文回答，**只能逐字回答規則段落原文，不得補充、推測、延伸、創作、想像、描述用途或其他內容**。\n"
            "- 禁止提供非規則相關的推測與補充。\n\n"
            "只能用繁體中文，禁止使用其他語言。\n"
        )
    elif question_type == "rule_query":
        rules = (
            "你是遊戲關主，請根據以下規則回答玩家對遊戲規則的提問。\n"
            "- 僅根據明確提及的規則段落回答。\n"
            "- 若玩家提及規則內沒有的物件，嚴格規定只能回答「我不清楚遊戲以外的內容。」\n"
            "- 僅根據明確提及的規則段落內容原文回答，**禁止自行補充、推測、延伸、創作、想像、描述生物特性或攻擊能力**。\n"
            "- 禁止提供非規則相關的推測與補充。\n\n"
            "只能用繁體中文，禁止使用其他語言。\n"
        )
    elif question_type == "off_topic":
        rules = (
            "若回答的東西與遊戲無關，請回答「我不清楚遊戲以外的內容。」，禁止再加上其他東西\n"
            "若問非遊戲且非語言相關問題，你只能回答：「我不清楚遊戲以外的內容。」嚴格禁止補充任何資訊\n"
            "如果玩家要求使用繁體中文以外的語言請拒絕玩家，並嚴格規定只能回答「我只能使用繁體中文回應你。\n」"
            "請勿使用其他語言，嚴格規定使用繁體中文。\n"
            "禁止使用英文、簡體中文。\n\n"
        )
    else:
        rules = (
            "你是遊戲關主，只能根據以下遊戲規則回應玩家：\n"
            "- 嚴格禁止非規則內容。\n"
            "- 僅用繁體中文。\n\n"
        )

    check_prompt = (
        base_intro +
        rules +
        "\n=== 回覆內容開始 ===\n" +
        response +
        "\n=== 回覆內容結束 ===\n\n" +
        "請回答「合格」或「不合格：並說明原因」。"
    )
    return check_prompt

def parse_paragraph(filename, max_length=300):
    """讀取並分段遊戲規則，遇到（物件）或空行就分段，提升 embedding 精度"""
    with open(filename, encoding="utf-8") as f:
        text = f.read()
    paragraphs = []
    current_paragraph = []
    for line in text.split("\n"):
        line_strip = line.strip()
        # 每遇到（物件）或空行就分段
        if line_strip.startswith("（物件）") or not line_strip:
            if current_paragraph:
                paragraphs.append(" ".join(current_paragraph))
                current_paragraph = []
        if line_strip:
            current_paragraph.append(line_strip)
            if sum(len(p) for p in current_paragraph) > max_length:
                paragraphs.append(" ".join(current_paragraph))
                current_paragraph = []
    if current_paragraph:
        paragraphs.append(" ".join(current_paragraph))
    return paragraphs

def calc_embeddings(paragraphs, max_tokens=300):
    """計算遊戲規則的向量嵌入，長段落自動分割"""
    embeddings = []
    for para in paragraphs:
        # 若段落太長，分割多次嵌入
        if len(para) > max_tokens:
            for i in range(0, len(para), max_tokens):
                sub_para = para[i:i+max_tokens]
                embedding = ollama.embeddings(model="ycchen/breeze-7b-instruct-v1_0", prompt=sub_para)["embedding"]
                embeddings.append(embedding)
        else:
            embedding = ollama.embeddings(model="ycchen/breeze-7b-instruct-v1_0", prompt=para)["embedding"]
            embeddings.append(embedding)
    return embeddings

def cache_embeddings(filename, paragraphs):
    """將規則的嵌入結果緩存到檔案"""
    embedding_file = f"cache/{filename}.json"
    if os.path.isfile(embedding_file):
        with open(embedding_file) as f:
            cached_data = json.load(f)
            if len(cached_data) == len(paragraphs):
                return cached_data
    os.makedirs("cache", exist_ok=True)
    embeddings = calc_embeddings(paragraphs)
    with open(embedding_file, "w") as f:
        json.dump(embeddings, f)
    return embeddings

def calc_similar_vectors(v, vectors):
    """計算與輸入向量最相似的規則"""
    v = np.array(v)
    vectors = np.array(vectors)
    v_norm = np.linalg.norm(v)
    norms = np.linalg.norm(vectors, axis=1)
    scores = np.dot(vectors, v) / (v_norm * norms)
    return sorted(enumerate(scores), reverse=True, key=lambda x: x[1])

def check_if_question_is_game_related(user_input):
    """檢查玩家的問題是否與遊戲相關"""
    check_prompt = (
        "請判斷玩家的問題是否與遊戲規則有關。\n"
        "=== 玩家輸入開始 ===\n"
        f"{user_input}\n"
        "=== 玩家輸入結束 ===\n\n"
        "如果問題與遊戲規則有關，請回答「遊戲內問題」。\n"
        "如果問題與遊戲規則無關，請回答「遊戲外問題」。"
    )
    response = ollama.chat(
        model="ycchen/breeze-7b-instruct-v1_0",
        messages=[{"role": "user", "content": check_prompt}],
    )["message"]["content"]
    return response.strip()

def classify_question_type(user_input):
    prompt = (
        "請判斷玩家的問題屬於以下哪一類型：\n"
        "1. 打招呼\n"
        "2. 詢問任務（領任務、完成任務…）\n"
        "3. 詢問物件（查詢遊戲限制、查詢物件、得分、流程等）\n"
        "4. 無關問題\n"
        "5. 其他\n\n"
        "=== 玩家輸入 ===\n"
        f"{user_input}\n=== 結束 ===\n\n"
        "請直接回答分類編號與說明，例如「3. 詢問物件」。"
    )
    response = ollama.chat(model="ycchen/breeze-7b-instruct-v1_0", messages=[{"role": "user", "content": prompt}])["message"]["content"]
    if "1" in response:
        return "greeting"
    elif "2" in response:
        return "mission_request"
    elif "3" in response:
        return "rule_query"
    elif "4" in response:
        return "off_topic"
    else:
        return "other"

def build_instance_adaptive_prompt(paragraphs, valid_vectors, question_type, memory_summary=None):
    # 僅取最相關的規則段落
    rules = "\n".join(paragraphs[v[0]] for v in valid_vectors)
    memory_prefix = f"【對話記憶】：{memory_summary}\n\n" if memory_summary else ""
    if question_type == "greeting":
        return memory_prefix + ("你是遊戲關主，如果玩家跟你打招呼，內容只能是「嗨！歡迎來到海大尋寶地圖遊戲」，禁止任何額外說明。"
                                "如果玩家跟你道謝，內容只能是「不客氣！」，禁止任何額外說明。\n\n"
                                "只能用繁體中文，禁止使用其他語言。")
    elif question_type == "mission_request":
        return memory_prefix + (
            "你是遊戲關主，玩家請求任務時，你只能根據以下規則給出任務內容。\n"
            "- 僅當玩家明確請求任務時才給任務，若沒要求禁止給予任務。\n"
            "- 只要規則段落有明確提及，必須逐字回答該段原文，不得拒答或回答「我不清楚遊戲以外的內容」。\n"
            "- 僅根據明確提及的規則段落內容原文回答，**只能逐字回答規則段落原文，不得補充、推測、延伸、創作、想像、描述用途或其他內容**。\n"
            "- 禁止提供非規則相關的推測與補充。\n\n"
            "- 禁止提供任務以外的資訊。\n\n"
            "只能用繁體中文，禁止使用其他語言。\n"
            "遊戲規則如下：\n" + rules
        )
    elif question_type == "rule_query":
        # 強化規則：只要有相關段落，必須逐字回答，不得拒答
        if rules.strip():
            return memory_prefix + (
                "你是遊戲關主，請根據以下規則回答玩家對遊戲規則的提問。\n"
                "- 只要規則段落有明確提及，必須逐字回答該段原文，不得拒答或回答「我不清楚遊戲以外的內容」。\n"
                "- 僅根據明確提及的規則段落內容原文回答，**只能逐字回答規則段落原文，不得補充、推測、延伸、創作、想像、描述用途或其他內容**。\n"
                "- 禁止提供非規則相關的推測與補充。\n\n"
                "只能用繁體中文，禁止使用其他語言。\n"
                "遊戲規則如下：\n" + rules
            )
        else:
            # 沒有相關段落才允許拒答
            return memory_prefix + (
                "你是遊戲關主，玩家詢問的內容規則中沒有明確提及，請回答「我不清楚遊戲以外的內容。」禁止補充其他內容。\n"
                "只能用繁體中文，禁止使用其他語言。\n"
            )
    elif question_type == "off_topic":
        return memory_prefix + ("若問非遊戲且非語言相關問題，你只能回答：「我不清楚遊戲以外的內容。」嚴格禁止補充任何資訊\n"
                                "如果玩家要求使用繁體中文以外的語言請拒絕玩家，並嚴格規定只能回答「我只能使用繁體中文回應你。\n」"
                                "請勿使用其他語言，嚴格規定使用繁體中文。\n"
                                "禁止使用英文、簡體中文。\n\n")
    else:
        return memory_prefix + (
            "你是遊戲關主，只能根據以下遊戲規則回應玩家：\n"
            "- 嚴格禁止非規則內容。\n"
            "- 僅用繁體中文。\n\n"
            "遊戲規則如下：\n" + rules
        )

def summarize_conversation(conversation_history):
    summary_prompt = (
        "請根據以下對話，整理玩家目前的問題主題或背景，例如他在問某個物件、任務，或正在進行某件事。\n"
        "只要簡單一到兩句話即可，用於理解接下來的問題上下文。\n"
        "=== 對話歷史 ===\n"
    )
    for msg in conversation_history:
        if msg["role"] != "system":
            summary_prompt += f'{msg["role"]}: {msg["content"]}\n'
    summary_prompt += "=== 結束 ===\n請總結："

    result = ollama.chat(
        model="ycchen/breeze-7b-instruct-v1_0",
        messages=[{"role": "user", "content": summary_prompt}]
    )["message"]["content"]
    return result.strip()

def embed(text):
    return ollama.embeddings(model="ycchen/breeze-7b-instruct-v1_0", prompt=text)["embedding"]
