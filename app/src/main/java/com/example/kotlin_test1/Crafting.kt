package com.example.kotlin_test1

// 第三步：檢查玩家背包是否符合配方需求

object CraftingSystem {

    // 檢查背包裡的道具是否符合配方需求
    fun canCraft(inventory: List<Item>, recipe: Recipe): Boolean {
        return recipe.requiredItems.all { (requiredId, requiredCount) ->
            inventory.any { it.itemid == requiredId && it.count.value >= requiredCount }
        }
    }

    // 執行合成動作（會直接修改 inventory）
    fun craftItem(inventory: MutableList<Item>, recipe: Recipe): Boolean {
        if (!canCraft(inventory, recipe)) return false

        // 扣掉素材
        recipe.requiredItems.forEach { (requiredId, requiredCount) ->
            inventory.find { it.itemid == requiredId }?.let {
                it.count.value -= requiredCount
            }
        }

        // 增加合成結果
        val resultItem = inventory.find { it.itemid == recipe.resultItemId }
        if (resultItem != null) {
            resultItem.count.value += 1
        }

        return true
    }
}
