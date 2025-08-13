package com.ntou01157.hunter.utils

import android.content.Context
import android.graphics.Bitmap
import org.pytorch.Module
import org.pytorch.IValue
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils

object Vectorizer {
    @Volatile private var module: Module? = null

    private fun ensureLoaded(ctx: Context) {
        if (module == null) {
            synchronized(this) {
                if (module == null) {
                    module = Module.load(assetFilePath(ctx, "simclr_mobilenetv3.pt"))
                }
            }
        }
    }

    fun imageToVector(ctx: Context, bitmap: Bitmap): List<Float> {
        ensureLoaded(ctx)

        // ↓ 與訓練一致的前處理（如你用的是 ImageNet 標準）
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std  = floatArrayOf(0.229f, 0.224f, 0.225f)

        // MobileNetV3 常見輸入 224x224，依你訓練值調整
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        val input: Tensor = TensorImageUtils.bitmapToFloat32Tensor(resized, mean, std)
        val out: Tensor = module!!.forward(IValue.from(input)).toTensor()
        val arr = out.dataAsFloatArray   // e.g. [d]
        return arr.toList()
    }

    // 將 assets 檔案複製到可讀路徑再載入
    private fun assetFilePath(ctx: Context, assetName: String): String {
        val file = java.io.File(ctx.filesDir, assetName)
        if (file.exists() && file.length() > 0) return file.absolutePath
        ctx.assets.open(assetName).use { inp ->
            java.io.FileOutputStream(file).use { out -> inp.copyTo(out) }
        }
        return file.absolutePath
    }
}
