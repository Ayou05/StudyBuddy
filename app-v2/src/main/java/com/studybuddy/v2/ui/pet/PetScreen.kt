package com.studybuddy.v2.ui.pet

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studybuddy.v2.data.model.Pet
import com.studybuddy.v2.data.model.breedEnum
import com.studybuddy.v2.data.model.growthStageEnum
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.component.AppButton
import com.studybuddy.v2.ui.component.AppCard
import com.studybuddy.v2.ui.component.AppPetImage
import com.studybuddy.v2.ui.component.LayeredPetSprite
import com.studybuddy.v2.ui.pet.saddle.ClaudeIdleLogPool
import com.studybuddy.v2.ui.pet.saddle.Pose
import com.studybuddy.v2.ui.pet.saddle.SaddleCatState
import com.studybuddy.v2.ui.pet.saddle.SaddleCatTerminal

@Composable
fun PetScreen(
    viewModel: PetViewModel = hiltViewModel(),
    onOpenCodex: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.appColors
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    // onResume 时刷新 —— 让从图鉴回来后能拉到新品种
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        when {
            state.loading -> { /* 默认空 */ }
            !state.hasPartner -> {
                // 没绑搭档 → 鞍部猫彩蛋只在解锁后显示，否则提示去绑定
                if (state.unlockedSaddleCat) SaddleCatStandalone() else PetEmpty()
            }
            state.pet == null -> {
                // 已绑搭档但 PB 还没回来 / 创建失败 → 显示蛋孵化中占位
                // 不再回退到 SaddleCatStandalone：那会让普通用户以为没有橘猫/暹罗
                PetEmpty(hint = "蛋还在路上，稍等…")
            }
            else -> PetContent(
                pet = state.pet!!,
                currentEmote = state.currentEmote,
                feedingTrigger = state.feedingTrigger,
                strokeTrigger = state.strokeTrigger,
                cleanTrigger = state.cleanTrigger,
                onFeed = viewModel::feed,
                onPlay = viewModel::play,
                onClean = viewModel::clean,
                onStroke = viewModel::stroke,
                onOpenCodex = onOpenCodex,
                onRename = viewModel::renamePet
            )
        }
    }
}

@Composable
private fun SaddleCatStandalone() {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ClaudeSpacing.pageHorizontal)
            .padding(top = ClaudeSpacing.xxl, bottom = ClaudeSpacing.xxl)
    ) {
        Text("PET", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.sm))
        Text("鞍部猫", style = ClaudeType.DisplayLg, color = colors.ink)
        Spacer(Modifier.height(ClaudeSpacing.xs))
        Text(
            "彩蛋形象 · 不依赖搭档",
            style = ClaudeType.BodyMd,
            color = colors.muted
        )
        Spacer(Modifier.height(ClaudeSpacing.xl))
        SaddleCatTerminal(
            state = SaddleCatState(pose = Pose.IDLE),
            log = ClaudeIdleLogPool.take(5),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PetEmpty(hint: String = "绑定搭档后，蛋会出现。") {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ClaudeSpacing.pageHorizontal)
            .padding(top = ClaudeSpacing.xxl)
    ) {
        Text("PET", style = ClaudeType.CaptionUppercase, color = colors.muted)
        Spacer(Modifier.height(ClaudeSpacing.sm))
        Text("还没有宠物。", style = ClaudeType.DisplayMd, color = colors.ink)
        Spacer(Modifier.height(ClaudeSpacing.sm))
        Text(hint, style = ClaudeType.BodyMd, color = colors.body)
    }
}

@Composable
private fun PetContent(
    pet: Pet,
    currentEmote: String,
    feedingTrigger: Long,
    strokeTrigger: Long,
    cleanTrigger: Long,
    onFeed: () -> Unit,
    onPlay: () -> Unit,
    onClean: () -> Unit,
    onStroke: () -> Unit,
    onOpenCodex: () -> Unit,
    onRename: (String) -> Unit
) {
    val colors = MaterialTheme.appColors
    val showRenameDialog = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showRenameDialog.value) {
        RenamePetDialog(
            currentName = pet.name,
            onDismiss = { showRenameDialog.value = false },
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog.value = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ClaudeSpacing.pageHorizontal)
            .padding(top = ClaudeSpacing.xxl, bottom = ClaudeSpacing.xxl)
    ) {
        // 顶部标题 — 衬线大字 + caption + 右侧"图鉴"入口（书本图标）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PET", style = ClaudeType.CaptionUppercase, color = colors.muted)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onOpenCodex() },
                contentAlignment = Alignment.Center
            ) {
                com.studybuddy.v2.ui.component.AppIcon(
                    name = "book",
                    size = 20.dp,
                    tint = colors.muted
                )
            }
        }
        Spacer(Modifier.height(ClaudeSpacing.sm))
        // 长按 name 改名 —— 配合默认名 "未命名" 引导用户
        Text(
            pet.name,
            style = ClaudeType.DisplayLg,
            color = colors.ink,
            modifier = Modifier.pointerInput(pet.id) {
                detectTapGestures(
                    onLongPress = { showRenameDialog.value = true }
                )
            }
        )
        Spacer(Modifier.height(ClaudeSpacing.xs))
        if (pet.name == "未命名") {
            Text("长按起个名字", style = ClaudeType.Caption, color = ClaudeColors.Primary)
            Spacer(Modifier.height(ClaudeSpacing.xs))
        }
        Text(
            "成长阶段 · ${stageLabel(pet.growthStageEnum().name)} · ${pet.ageDays} 天",
            style = ClaudeType.BodyMd,
            color = colors.muted
        )
        // 急需照顾提示 —— 任意一维 < 30 时显示一行 coral 文字（不是弹窗、不打扰）
        urgentNeedHint(pet)?.let { hint ->
            Spacer(Modifier.height(ClaudeSpacing.xs))
            Text(hint, style = ClaudeType.BodySm, color = ClaudeColors.Primary)
        }

        Spacer(Modifier.height(ClaudeSpacing.xl))

        if (pet.breedEnum() == com.studybuddy.v2.data.model.PetBreed.SADDLE_CAT) {
            // 鞍部猫：替换 cream-card + PNG 为终端窗口
            SaddleCatTerminal(
                state = SaddleCatState(
                    pose = Pose.IDLE,
                    hunger = pet.hunger,
                    mood = pet.mood,
                    cleanliness = pet.cleanliness,
                    intimacy = pet.intimacy
                ),
                log = ClaudeIdleLogPool.take(5),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // 鞍部风像素生物（替代 PNG 切割方案）
            AppCard.Feature(
                modifier = Modifier
                    .fillMaxWidth(),
                padding = ClaudeSpacing.lg
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = pet.breedEnum(),
                        transitionSpec = {
                            (androidx.compose.animation.fadeIn(
                                androidx.compose.animation.core.tween(320)
                            ) + androidx.compose.animation.slideInVertically(
                                animationSpec = androidx.compose.animation.core.tween(320),
                                initialOffsetY = { full -> full / 4 }
                            )) togetherWith androidx.compose.animation.fadeOut(
                                androidx.compose.animation.core.tween(160)
                            )
                        },
                        label = "breedSwap"
                    ) { breed ->
                        com.studybuddy.v2.ui.pet.saddle.PixelCreatureSprite(
                            breed = breedToFriendsBreed(breed),
                            growthStage = pet.growthStageEnum(),
                            pose = if (currentEmote == "sleeping") com.studybuddy.v2.ui.pet.saddle.Pose.SLEEPING
                                   else if (currentEmote == "happy") com.studybuddy.v2.ui.pet.saddle.Pose.HAPPY
                                   else com.studybuddy.v2.ui.pet.saddle.Pose.IDLE,
                            feedingTrigger = feedingTrigger,
                            strokeTrigger = strokeTrigger,
                            cleanTrigger = cleanTrigger,
                            sleeping = currentEmote == "sleeping",
                            mood = pet.mood,
                            pixelSize = 12.dp
                        )
                    }
                    Spacer(Modifier.height(ClaudeSpacing.sm))
                    // 极浅圆形阴影 — 用 surface-cream-strong 画一条窄椭圆
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceCreamStrong)
                    )
                }
            }
        }

        Spacer(Modifier.height(ClaudeSpacing.lg))

        // 四维 2x2 progress 卡
        Row(horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            StatCard("饱腹", pet.hunger, Modifier.weight(1f))
            StatCard("心情", pet.mood, Modifier.weight(1f))
        }
        Spacer(Modifier.height(ClaudeSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            StatCard("清洁", pet.cleanliness, Modifier.weight(1f))
            StatCard("亲密", pet.intimacy, Modifier.weight(1f))
        }

        Spacer(Modifier.height(ClaudeSpacing.lg))

        // 底部 4 个互动 — 用 button-secondary（canvas + hairline）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.xs)
        ) {
            AppButton.Secondary(text = "喂食", onClick = onFeed, modifier = Modifier.weight(1f))
            AppButton.Secondary(text = "陪玩", onClick = onPlay, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(ClaudeSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ClaudeSpacing.xs)
        ) {
            AppButton.Secondary(text = "清洁", onClick = onClean, modifier = Modifier.weight(1f))
            AppButton.Secondary(text = "抚摸", onClick = onStroke, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: Float, modifier: Modifier) {
    val colors = MaterialTheme.appColors
    // 阈值色：> 60 健康（绿）/ 30-60 一般（amber）/ < 30 警示（coral）
    val tint = when {
        value > 60f -> colors.success
        value > 30f -> ClaudeColors.AccentAmber
        else -> ClaudeColors.Primary
    }
    AppCard.Feature(
        modifier = modifier,
        padding = ClaudeSpacing.md,
        radius = ClaudeRadius.md
    ) {
        Column {
            Text(label, style = ClaudeType.Caption, color = colors.muted)
            Spacer(Modifier.height(ClaudeSpacing.xxs))
            Text("${value.toInt()}", style = ClaudeType.TitleLg, color = colors.ink)
            Spacer(Modifier.height(ClaudeSpacing.xs))
            ProgressBar(progress = (value / 100f).coerceIn(0f, 1f), tint = tint)
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, tint: Color) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.hairlineSoft)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(tint)
        )
    }
}

private fun stageLabel(stage: String): String = when (stage) {
    "EGG" -> "蛋"
    "BABY" -> "幼体"
    "YOUNG" -> "成长期"
    "ADULT" -> "成熟期"
    "ULTIMATE" -> "完全体"
    else -> stage
}

/**
 * 急需照顾提示 —— 任意一维 < 30 时返回一句温柔的 coral 副标。
 * 优先级 hunger > cleanliness > mood > intimacy（饿和脏更紧迫）。
 * 都正常 → null，UI 不显示这行。
 */
private fun urgentNeedHint(pet: Pet): String? {
    if (pet.growthStageEnum().name == "EGG") return null  // 蛋不需要
    return when {
        pet.hunger < 10f -> "它饿坏了，喂点东西吧"
        pet.hunger < 30f -> "它有点饿了"
        pet.cleanliness < 10f -> "它需要洗一洗了"
        pet.cleanliness < 30f -> "它有点脏了"
        pet.mood < 30f -> "它有点闷闷的，陪陪它吧"
        pet.intimacy < 20f -> "好久没和它玩了，它在等你"
        else -> null
    }
}

@Composable
private fun RenamePetDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = MaterialTheme.appColors
    val input = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(if (currentName == "未命名") "" else currentName)
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceCard,
        title = {
            Text("起个名字", style = ClaudeType.TitleLg, color = colors.ink)
        },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = input.value,
                    onValueChange = { input.value = it.take(12) },
                    placeholder = { Text("最多 12 个字") },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClaudeColors.Primary,
                        unfocusedBorderColor = colors.hairline
                    )
                )
                Spacer(Modifier.height(ClaudeSpacing.xs))
                Text("一旦起了名字，会跟着它从蛋一直长大", style = ClaudeType.Caption, color = colors.muted)
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(input.value.trim()) },
                enabled = input.value.trim().isNotEmpty()
            ) {
                Text("确定", color = ClaudeColors.Primary)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("取消", color = colors.muted)
            }
        }
    )
}

// ─── 像素生物品种映射 ────────────────────────────────────────────────────────
private fun breedToFriendsBreed(
    breed: com.studybuddy.v2.data.model.PetBreed
): com.studybuddy.v2.ui.pet.saddle.SaddleFriendsFrames.Breed = when (breed) {
    com.studybuddy.v2.data.model.PetBreed.ORANGE_CAT -> com.studybuddy.v2.ui.pet.saddle.SaddleFriendsFrames.Breed.ORANGE
    com.studybuddy.v2.data.model.PetBreed.SIAMESE_CAT -> com.studybuddy.v2.ui.pet.saddle.SaddleFriendsFrames.Breed.SIAMESE
    com.studybuddy.v2.data.model.PetBreed.SADDLE_CAT -> com.studybuddy.v2.ui.pet.saddle.SaddleFriendsFrames.Breed.ORANGE
    com.studybuddy.v2.data.model.PetBreed.FOX -> com.studybuddy.v2.ui.pet.saddle.SaddleFriendsFrames.Breed.FOX
    com.studybuddy.v2.data.model.PetBreed.RABBIT -> com.studybuddy.v2.ui.pet.saddle.SaddleFriendsFrames.Breed.RABBIT
    com.studybuddy.v2.data.model.PetBreed.COW -> com.studybuddy.v2.ui.pet.saddle.SaddleFriendsFrames.Breed.COW
    com.studybuddy.v2.data.model.PetBreed.SHIBA -> com.studybuddy.v2.ui.pet.saddle.SaddleFriendsFrames.Breed.SHIBA
    com.studybuddy.v2.data.model.PetBreed.CAPYBARA -> com.studybuddy.v2.ui.pet.saddle.SaddleFriendsFrames.Breed.CAPYBARA
    com.studybuddy.v2.data.model.PetBreed.HEDGEHOG -> com.studybuddy.v2.ui.pet.saddle.SaddleFriendsFrames.Breed.HEDGEHOG
}
