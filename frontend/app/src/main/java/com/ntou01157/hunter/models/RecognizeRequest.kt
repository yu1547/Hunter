package com.ntou01157.hunter.models

data class RecognizeRequest(
    val userId: String,
    val spotName: String,
    val vector: List<Float>
)
