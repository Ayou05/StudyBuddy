package com.studybuddy.v2.ui.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 加载本地 drawable VectorDrawable。
 * 文件命名规范：`ic_<name>.xml`（已由 P0-2 从 570+ 图标包转换）。
 *
 * @param name 例如 "home"、"map"、"play"
 */
@Composable
fun AppIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color? = null
) {
    val ctx = LocalContext.current
    val resId = remember(name) {
        ctx.resources.getIdentifier("ic_$name", "drawable", ctx.packageName)
    }
    if (resId == 0) return
    Image(
        painter = painterResource(id = resId),
        contentDescription = name,
        modifier = modifier.then(Modifier),
        colorFilter = tint?.let { ColorFilter.tint(it) }
    )
}
