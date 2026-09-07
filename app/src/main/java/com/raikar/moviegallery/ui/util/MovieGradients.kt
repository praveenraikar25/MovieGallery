package com.raikar.moviegallery.ui.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush

object MovieGradients {
    private val palettes: List<List<Color>> =
        listOf(
            listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF)),
            listOf(Color(0xFF834D9B), Color(0xFFD04ED6)),
            listOf(Color(0xFF1D4350), Color(0xFFA43931)),
            listOf(Color(0xFF0F2027), Color(0xFF2C5364)),
            listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B)),
            listOf(Color(0xFF141E30), Color(0xFF243B55)),
            listOf(Color(0xFF360033), Color(0xFF0B8793)),
            listOf(Color(0xFF4B134F), Color(0xFFC94B4B)),
            listOf(Color(0xFF0B486B), Color(0xFFF56217)),
            listOf(Color(0xFF1A2980), Color(0xFF26D0CE)),
            listOf(Color(0xFF333333), Color(0xFFDD1818)),
            listOf(Color(0xFF403A3E), Color(0xFFBE5869)),
            listOf(Color(0xFF1F1C2C), Color(0xFF928DAB)),
            listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E)),
            listOf(Color(0xFF2B5876), Color(0xFF4E4376)),
            listOf(Color(0xFF614385), Color(0xFF516395)),
            listOf(Color(0xFF525252), Color(0xFF3D7236)),
            listOf(Color(0xFF000428), Color(0xFF004E92)),
            listOf(Color(0xFF232526), Color(0xFF414345)),
            listOf(Color(0xFF1B1B1B), Color(0xFF7B4397)),
        )

    fun colorsFor(movieId: Int): List<Color> = palettes[(movieId - 1).mod(palettes.size)]

    /** Approximates CSS `linear-gradient(135deg, …)`: top-left to bottom-right. */
    fun brushFor(movieId: Int): Brush {
        val colors = colorsFor(movieId)
        return object : ShaderBrush() {
            override fun createShader(size: androidx.compose.ui.geometry.Size) =
                LinearGradientShader(
                    from = Offset(0f, 0f),
                    to = Offset(size.width, size.height),
                    colors = colors,
                )
        }
    }
}
