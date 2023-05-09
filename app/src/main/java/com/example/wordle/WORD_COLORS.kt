package com.example.wordle

import android.graphics.Color

enum class WORD_COLORS(private val rgb: Int) {
    GREEN(Color.rgb(3, 139, 89)),
    YELLOW(Color.rgb(242, 194, 48)),
    BLACK(Color.rgb(14, 14, 14));

    fun getRGB(): Int {
        return rgb
    }
}