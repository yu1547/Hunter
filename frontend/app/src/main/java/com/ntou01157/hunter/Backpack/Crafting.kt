// package com.ntou01157.hunter

// object CraftingSystem {
//     // 檢查玩家背包是否有足夠的材料合成指定物品
//     fun canCraft(inventory: List<Item>, targetItemId: String): Boolean {
//         // 找出所有指向目標物品的材料
//         val requiredMaterials = inventory.filter { 
//             it.itemType == 0 && it.resultId == targetItemId 
//         }
        
//         // 若沒有任何材料指向該物品，則無法合成
//         if (requiredMaterials.isEmpty()) return false
        
//         // 檢查每種材料是否都至少有1個
//         return requiredMaterials.all { it.count.value > 0 }
//     }

//     // 執行合成動作（會直接修改 inventory）
//     fun craftItem(inventory: MutableList<Item>, targetItemId: String): Boolean {
//         if (!canCraft(inventory, targetItemId)) return false

//         // 獲取所需的材料
//         val requiredMaterials = inventory.filter { 
//             it.itemType == 0
//         }
        
//         // 減少每種材料的數量
//         requiredMaterials.forEach { material ->
//             material.count.value -= 1
//         }

//         // // 增加合成結果
//         // val resultItem = it.resultId
//         // if (resultItem != null) {
//         //     resultItem.count.value += 1
//         // }

//         return true
//     }
// }
