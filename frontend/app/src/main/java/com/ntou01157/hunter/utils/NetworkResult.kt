// frontend/app/src/main/java/com/ntou01157/hunter/utils/NetworkResult.kt
package com.ntou01157.hunter.utils

/**
 * 用於封裝網路請求結果的密封類。
 *
 * @param T 數據類型
 * @param data 成功時的數據
 * @param message 錯誤時的訊息
 */
sealed class NetworkResult<T>(
    val data: T? = null,
    val message: String? = null
) {
    /**
     * 表示網路請求成功。
     * @param data 請求成功時返回的數據。
     */
    class Success<T>(data: T) : NetworkResult<T>(data)

    /**
     * 表示網路請求失敗。
     * @param message 錯誤訊息。
     * @param data 即使在錯誤時也可能存在的數據（例如，部分數據或快取數據）。
     */
    class Error<T>(message: String, data: T? = null) : NetworkResult<T>(data, message)

    /**
     * 表示網路請求正在載入中。
     */
    class Loading<T> : NetworkResult<T>()
}