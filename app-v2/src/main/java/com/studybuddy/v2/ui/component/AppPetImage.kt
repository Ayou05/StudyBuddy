package com.studybuddy.v2.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studybuddy.v2.data.model.GrowthStage
import com.studybuddy.v2.data.model.PetBreed
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors

/**
 * 宠物贴图加载器。
 *
 * 命名规范遵循现有 102 张 PNG：${breed}_${stageSuffix}.png
 *   orange_stage1_baby / siamese_stage3_adult / orange_stage0_egg ...
 *
 * 找不到资源时显示 cream-strong 圆形占位 + 文字 "🥚 → 🐱" 的中文回退。
 */
@Composable
fun AppPetImage(
    breed: PetBreed,
    growthStage: GrowthStage,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    dirty: Boolean = false
) {
    val ctx = LocalContext.current
    val name = buildName(breed, growthStage, dirty)
    val resId = remember(name) {
        ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
    }
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = "Pet",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(colors.surfaceCreamStrong),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stageHintCn(growthStage),
                    style = ClaudeType.Caption,
                    color = colors.mutedSoft
                )
            }
        }
    }
}

private fun buildName(breed: PetBreed, stage: GrowthStage, dirty: Boolean): String {
    val prefix = when (breed) {
        PetBreed.ORANGE_CAT -> "orange"
        PetBreed.SIAMESE_CAT -> "siamese"
        PetBreed.SADDLE_CAT -> "orange"
        PetBreed.FOX, PetBreed.RABBIT, PetBreed.COW,
        PetBreed.SHIBA, PetBreed.CAPYBARA, PetBreed.HEDGEHOG -> "orange"  // 鞍部风品种走 PixelCreatureSprite
    }
    val suffix = when (stage) {
        GrowthStage.EGG -> "stage0_egg"
        GrowthStage.BABY -> if (dirty) "stage1_dirty" else "stage1_baby"
        GrowthStage.YOUNG -> if (dirty) "stage2_dirty" else "stage2_young"
        GrowthStage.ADULT -> if (dirty) "stage3_dirty" else "stage3_adult"
        GrowthStage.ULTIMATE -> if (dirty) "stage4_dirty" else "stage4_ultimate"
    }
    return "${prefix}_${suffix}"
}

private fun stageHintCn(stage: GrowthStage) = when (stage) {
    GrowthStage.EGG -> "蛋"
    GrowthStage.BABY -> "幼体"
    GrowthStage.YOUNG -> "成长"
    GrowthStage.ADULT -> "成熟"
    GrowthStage.ULTIMATE -> "完全体"
}
