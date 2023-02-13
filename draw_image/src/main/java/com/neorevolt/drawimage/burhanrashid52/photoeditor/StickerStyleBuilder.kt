package com.neorevolt.drawimage.burhanrashid52.photoeditor

import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.widget.TextView
import androidx.core.graphics.scale

/**
 *
 *
 * This class is used to wrap the styles to apply on the TextView on [PhotoEditor.addText] and [PhotoEditor.editText]
 *
 *
 * @author [NeoRevolt]
 * @since 2022
 */
open class StickerStyleBuilder {
    val values = mutableMapOf<StickerStyle, Any>()


    fun withStickerSize(size: Float) {
        values[StickerStyle.SIZE] = size
    }

    fun applyStyle(bitmap: Bitmap, size: Float) {

        applyStickerSize(bitmap, size)
    }

    protected open fun applyStickerSize(bitmap: Bitmap, size: Float) {
        val resized = Bitmap.createScaledBitmap(bitmap, size.toInt(), size.toInt(),true)
        bitmap.scale(size.toInt(), size.toInt(),true)
    }

    enum class StickerStyle(val property: String) {
        SIZE("StickerSize")
    }
}