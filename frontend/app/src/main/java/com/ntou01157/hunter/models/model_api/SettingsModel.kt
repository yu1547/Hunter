package com.ntou01157.hunter.model.model_api

import com.google.gson.annotations.SerializedName

data class Settings(
    @SerializedName("music") val music: Boolean,
    @SerializedName("notification") val notification: Boolean,
    @SerializedName("language") val language: String
)
