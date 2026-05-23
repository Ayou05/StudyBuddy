package com.studybuddy.v2.ui.pet.saddle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * 通用像素矩阵渲染器。
 *
 * 用 Canvas drawRect 把 [matrix]（rows × cols 的 IntArray，1=填充）一格一格画成方块。
 * [pixelSize] 是单个方块的边长，整张图固定 cols * pixelSize × rows * pixelSize。
 *
 * 行距列距完全相等 —— 这是放弃字符画走矩阵的核心理由。
 */
@Composable
fun PixelMatrix(
    matrix: IntArray,
    rows: Int,
    cols: Int,
    pixelSize: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(width = pixelSize * cols, height = pixelSize * rows)
    ) {
        val px = pixelSize.toPx()
        // 相邻方块在浮点对齐 + anti-alias 下边缘会变成半透明灰，看起来像 1px 灰缝。
        // 解法：每个 drawRect 外扩 0.5px（topLeft 偏移 -0.25, size +0.5），
        // 让相邻块物理 overlap 半像素，缝就被覆盖掉。大尺寸下肉眼看不出来。
        val overlap = 0.5f
        val half = overlap / 2f
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                if (matrix[y * cols + x] == 1) {
                    drawRect(
                        color = color,
                        topLeft = Offset(x * px - half, y * px - half),
                        size = Size(px + overlap, px + overlap)
                    )
                }
            }
        }
    }
}
