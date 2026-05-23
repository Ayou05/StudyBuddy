package com.studybuddy.v2.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.data.model.GrowthStage
import com.studybuddy.v2.data.model.Pet
import com.studybuddy.v2.data.model.PetBreed
import com.studybuddy.v2.data.model.breedEnum
import com.studybuddy.v2.data.model.growthStageEnum
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppButton
import com.studybuddy.v2.ui.component.AppCard
import com.studybuddy.v2.ui.component.AppIcon
import com.studybuddy.v2.ui.pet.saddle.PixelCreatureSprite
import com.studybuddy.v2.ui.pet.saddle.Pose
import com.studybuddy.v2.ui.pet.saddle.SaddleFriendsFrames

private data class BreedEntry(val breed: PetBreed, val name: String, val tagline: String)

private val codexEntries = listOf(
    BreedEntry(PetBreed.ORANGE_CAT, "鞍部猫", "你已经认识 TA"),
    BreedEntry(PetBreed.SIAMESE_CAT, "暹罗", "蓝眼，安静"),
    BreedEntry(PetBreed.FOX, "狐狸", "尖耳长尾"),
    BreedEntry(PetBreed.RABBIT, "兔子", "长耳圆胖"),
    BreedEntry(PetBreed.COW, "奶牛", "黑白小角"),
    BreedEntry(PetBreed.SHIBA, "柴犬", "三角立耳"),
    BreedEntry(PetBreed.CAPYBARA, "水豚", "横眯眼"),
    BreedEntry(PetBreed.HEDGEHOG, "刺猬", "一排刺尖鼻")
)

/**
 * 图鉴：8 种伙伴的全屏选择页。
 *
 * 不在 PetScreen 平铺切换 chip —— 那是流水账。
 * 这里要让"换一个伙伴"成为有仪式感的事：进图鉴 → 选 → 二次确认 → 成长进度保留。
 */
@Composable
fun CodexScreen(
    onBack: () -> Unit = {},
    viewModel: CodexViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors
    val pet = state.pet

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ClaudeSpacing.pageHorizontal)
                .padding(top = ClaudeSpacing.xxl, bottom = ClaudeSpacing.xxl)
        ) {
            // 顶部：返回 + 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(name = "arrow_left", size = 20.dp, tint = colors.muted)
                }
                Spacer(Modifier.size(ClaudeSpacing.xs))
                Text("CODEX", style = ClaudeType.CaptionUppercase, color = colors.muted)
            }
            Spacer(Modifier.height(ClaudeSpacing.sm))
            Text("图鉴", style = ClaudeType.DisplayLg, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(
                "和你一起长大的伙伴。换 TA 不会丢失成长进度。",
                style = ClaudeType.BodyMd,
                color = colors.muted
            )

            Spacer(Modifier.height(ClaudeSpacing.xl))

            // 8 个品种 4x2 grid
            val rows = codexEntries.chunked(2)
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.sm)
                ) {
                    row.forEach { entry ->
                        BreedTile(
                            entry = entry,
                            isCurrent = pet?.breedEnum() == entry.breed,
                            growthStage = pet?.growthStageEnum() ?: GrowthStage.ADULT,
                            onPick = { viewModel.pickBreed(entry.breed) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(ClaudeSpacing.sm))
            }
        }

        // 二次确认 sheet
        val pending = state.pendingBreed
        if (pending != null && pet != null) {
            ConfirmSwitchSheet(
                pet = pet,
                target = pending,
                onCancel = { viewModel.cancelPick() },
                onConfirm = {
                    viewModel.confirmPick(onDone = onBack)
                }
            )
        }
    }
}

@Composable
private fun BreedTile(
    entry: BreedEntry,
    isCurrent: Boolean,
    growthStage: GrowthStage,
    onPick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ClaudeRadius.lg))
            .background(colors.surfaceCard)
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) ClaudeColors.Primary else colors.hairline,
                shape = RoundedCornerShape(ClaudeRadius.lg)
            )
            .clickable { onPick() }
            .padding(ClaudeSpacing.md)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                PixelCreatureSprite(
                    breed = breedToFrames(entry.breed),
                    growthStage = growthStage,
                    pose = Pose.IDLE,
                    pixelSize = 6.dp
                )
            }
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(entry.name, style = ClaudeType.TitleSm, color = colors.ink)
            Spacer(Modifier.height(2.dp))
            Text(entry.tagline, style = ClaudeType.Caption, color = colors.muted)
            if (isCurrent) {
                Spacer(Modifier.height(ClaudeSpacing.xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ClaudeColors.Primary)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text("在陪你", style = ClaudeType.Caption, color = ClaudeColors.Primary)
                }
            }
        }
    }
}

@Composable
private fun ConfirmSwitchSheet(
    pet: Pet,
    target: PetBreed,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val targetName = codexEntries.firstOrNull { it.breed == target }?.name ?: target.name
    val isSame = pet.breedEnum() == target

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        containerColor = colors.surfaceCard,
        title = {
            Text(
                if (isSame) "TA 已经在陪你了" else "要把 TA 换成 $targetName 吗?",
                style = ClaudeType.TitleLg,
                color = colors.ink
            )
        },
        text = {
            Text(
                if (isSame) "不需要切换。"
                else "TA 的成长进度、亲密度、名字 “${pet.name}” 都会保留。只是换了个外形。",
                style = ClaudeType.BodyMd,
                color = colors.body
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text(
                    if (isSame) "好" else "换 TA",
                    color = ClaudeColors.Primary
                )
            }
        },
        dismissButton = {
            if (!isSame) {
                androidx.compose.material3.TextButton(onClick = onCancel) {
                    Text("再想想", color = colors.muted)
                }
            }
        }
    )
}

private fun breedToFrames(breed: PetBreed): SaddleFriendsFrames.Breed = when (breed) {
    PetBreed.ORANGE_CAT -> SaddleFriendsFrames.Breed.ORANGE
    PetBreed.SIAMESE_CAT -> SaddleFriendsFrames.Breed.SIAMESE
    PetBreed.SADDLE_CAT -> SaddleFriendsFrames.Breed.ORANGE
    PetBreed.FOX -> SaddleFriendsFrames.Breed.FOX
    PetBreed.RABBIT -> SaddleFriendsFrames.Breed.RABBIT
    PetBreed.COW -> SaddleFriendsFrames.Breed.COW
    PetBreed.SHIBA -> SaddleFriendsFrames.Breed.SHIBA
    PetBreed.CAPYBARA -> SaddleFriendsFrames.Breed.CAPYBARA
    PetBreed.HEDGEHOG -> SaddleFriendsFrames.Breed.HEDGEHOG
}
