package com.ntou01157.hunter

suspend fun craftItem(userId: String, recipeId: String): InventoryResponse {
    return client.post("$BASE_URL/inventory/$userId/craft") {
        contentType(ContentType.Application.Json)
        setBody(mapOf("recipeId" to recipeId))
    }.body()
}