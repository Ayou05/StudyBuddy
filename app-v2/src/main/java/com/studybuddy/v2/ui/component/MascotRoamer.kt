package com.studybuddy.v2.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.studybuddy.v2.ui.pet.saddle.Pose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 鞍部猫漫游器 —— EDGE 三边游走 + 偶尔 INTERIOR 飞船潜入。
 *
 * # 两态
 * - [Mode.EDGE]：沿屏幕左/底/右三条边走，θ ∈ [0, 1)。
 * - [Mode.INTERIOR]：骑飞船在屏中心区域游荡，路径避开 [MascotAvoidRegistry] 注册的文字 Rect。
 *
 * INTERIOR 触发节奏：每条 EDGE 停留若干轮后，按 ~30% 概率切到 INTERIOR 飞一圈再回 EDGE。
 * INTERIOR 期间 sprite 不旋转（永远脸朝上 + 脚踩飞船），方向感由飞船平移轨迹自然给出。
 */
@Stable
class MascotRoamer(private val scope: CoroutineScope) {

    enum class Mode { EDGE, INTERIOR }

    private val _theta = Animatable(0.5f)         // EDGE 模式下的位置
    private val _interiorX = Animatable(0f)        // INTERIOR 模式下的 sprite 中心 x
    private val _interiorY = Animatable(0f)
    private val _mode = MutableStateFlow(Mode.EDGE)
    private val _pose = MutableStateFlow(Pose.IDLE)

    val mode: StateFlow<Mode> = _mode.asStateFlow()
    val pose: StateFlow<Pose> = _pose.asStateFlow()
    val theta: Float get() = _theta.value
    val interiorPos: Offset get() = Offset(_interiorX.value, _interiorY.value)

    /** 从持久状态恢复。在 start 之前调用。 */
    suspend fun hydrate(theta: Float, pose: Pose) {
        _theta.snapTo(theta.coerceIn(0f, 0.9999f))
        _pose.value = pose
    }

    /** 给持久状态用的快照值。 */
    fun snapshotTheta(): Float = _theta.value
    fun snapshotPose(): Pose = _pose.value

    private var roamJob: Job? = null
    private var lookJob: Job? = null
    private var idleSleepJob: Job? = null
    private var lastLookAtMs: Long = 0L
    private var lastInteractionMs: Long = System.currentTimeMillis()

    /** 启动主漫游循环 */
    fun start(getBounds: () -> Rect, getAvoidRegions: () -> List<Rect> = { emptyList() }) {
        if (roamJob?.isActive == true) return
        // 起手把 theta 拉到 BOTTOM 段中央，主轴在底边
        scope.launch {
            val b = getBounds()
            _theta.snapTo(thetaForBottomCenter(b))
        }
        roamJob = scope.launch {
            var edgeRoundsSinceInterior = 0
            while (true) {
                when (_mode.value) {
                    Mode.EDGE -> {
                        // 35-60s 停留 + 小步幅，散步感
                        delay(35_000L + Random.nextLong(25_000L))
                        val bounds = getBounds()
                        val avoid = getAvoidRegions()
                        val currentEdge = edgeAt(_theta.value, bounds)
                        // 步长 5-12% 之间
                        val rawDelta = (0.05f + Random.nextFloat() * 0.07f) *
                                (if (Random.nextBoolean()) 1f else -1f)

                        // 主轴偏置：BOTTOM 段是"家"，80% 概率把目标 clamp 在 BOTTOM 段内不跨角
                        val target = if (currentEdge == Edge.BOTTOM && Random.nextFloat() < 0.80f) {
                            val (lo, hi) = bottomThetaRange(bounds)
                            (_theta.value + rawDelta).coerceIn(lo, hi)
                        } else {
                            (_theta.value + rawDelta).coerceIn(0f, 0.9999f)
                        }

                        // 检查目标是否跨段；跨段则走"角点歇 + snapTo 切方向"流程
                        val targetEdge = edgeAt(target, bounds)
                        if (targetEdge != currentEdge) {
                            crossCornerStep(currentEdge, targetEdge, target, bounds)
                        } else {
                            // 同段平移；命中文字 Rect 则换一个不撞文字的目标
                            val safe = avoidEdgeOverlap(target, bounds, avoid, currentEdge)
                            _theta.animateTo(
                                targetValue = safe,
                                // stiffness 4，更软更慢
                                animationSpec = spring(dampingRatio = 1f, stiffness = 4f)
                            )
                        }
                        edgeRoundsSinceInterior++
                        // 每 5-8 轮 EDGE 后 15% 概率切 INTERIOR（更克制，不抢戏）
                        if (edgeRoundsSinceInterior >= 5 && Random.nextFloat() < 0.15f) {
                            edgeRoundsSinceInterior = 0
                            switchToInterior(getBounds(), getAvoidRegions())
                        }
                    }
                    Mode.INTERIOR -> {
                        val bounds = getBounds()
                        val avoid = getAvoidRegions()
                        val waypoints = (1..(2 + Random.nextInt(3))).map {
                            randomFreePoint(bounds, avoid)
                        }
                        for (wp in waypoints) {
                            _interiorX.animateTo(wp.x, spring(dampingRatio = 1f, stiffness = 12f))
                            _interiorY.animateTo(wp.y, spring(dampingRatio = 1f, stiffness = 12f))
                            delay(3_500L + Random.nextLong(2_000L))
                        }
                        returnToEdge(bounds)
                    }
                }
            }
        }
    }

    /**
     * 跨角 step：到角点 → 停 2-3s（猫坐着观察）→ snapTo 切到新段同一角点 → 出发去 finalTarget。
     *
     * 关键：snapTo 之间没有旋转动画，UI 层 EdgeMascot 看 [edgeAt] 实时变化决定 rotation，
     * 但因为 _theta 是瞬切，rotation 也是瞬切（参见 MascotDock 配套改动）。
     */
    private suspend fun crossCornerStep(
        from: Edge,
        to: Edge,
        finalTarget: Float,
        bounds: Rect
    ) {
        // 1. animate 到 from 段靠近 to 段那一端的角点
        val cornerInFrom = cornerThetaInside(from, towards = to, bounds)
        _theta.animateTo(cornerInFrom, spring(dampingRatio = 1f, stiffness = 4f))
        // 2. 角点坐 2-3s（猫观察）
        delay(2_000L + Random.nextLong(1_500L))
        // 3. 瞬切到 to 段对应角点（rotation 由此瞬切）
        val cornerInTo = cornerThetaInside(to, towards = from, bounds)
        _theta.snapTo(cornerInTo)
        // 4. 短停 ~400ms 让用户感知"咦它换边了"
        delay(400L)
        // 5. animate 到最终目标
        _theta.animateTo(finalTarget, spring(dampingRatio = 1f, stiffness = 4f))
    }

    /** BOTTOM 段中心 theta（漫游主轴） */
    private fun thetaForBottomCenter(bounds: Rect): Float {
        val total = totalLength(bounds)
        return ((bounds.height + bounds.width / 2f) / total).coerceIn(0f, 0.9999f)
    }

    /** BOTTOM 段 theta 区间 [lo, hi]，留 5% margin 不贴角 */
    private fun bottomThetaRange(bounds: Rect): Pair<Float, Float> {
        val total = totalLength(bounds)
        val h = bounds.height
        val w = bounds.width
        val margin = 0.05f * (w / total)
        val lo = (h / total) + margin
        val hi = ((h + w) / total) - margin
        return lo to hi
    }

    /** 给定段 [edge]，返回靠 [towards] 那一端的角点 theta */
    private fun cornerThetaInside(edge: Edge, towards: Edge, bounds: Rect): Float {
        val total = totalLength(bounds)
        val h = bounds.height
        val w = bounds.width
        // 段边界：LEFT [0, h/total]，BOTTOM [h/total, (h+w)/total]，RIGHT [(h+w)/total, 1)
        return when (edge) {
            Edge.LEFT -> if (towards == Edge.BOTTOM) (h / total - 0.001f) else 0.001f
            Edge.BOTTOM -> when (towards) {
                Edge.LEFT -> (h / total + 0.001f)
                Edge.RIGHT -> ((h + w) / total - 0.001f)
                else -> ((h + w / 2f) / total)
            }
            Edge.RIGHT -> if (towards == Edge.BOTTOM) ((h + w) / total + 0.001f) else 0.9999f
        }
    }

    /** 同段平移时检查目标位置是否撞到文字 Rect，撞了则向反方向回退到无文字处 */
    private fun avoidEdgeOverlap(
        target: Float,
        bounds: Rect,
        avoid: List<Rect>,
        edge: Edge
    ): Float {
        if (avoid.isEmpty()) return target
        val pos = positionFor(target, bounds)
        val padded = avoid.any { it.containsWithPad(pos, 24f) }
        if (!padded) return target
        // 反向探 30%，找一个不撞文字的位置
        val total = totalLength(bounds)
        val w = bounds.width
        val maxBackoff = 0.20f * (w / total)
        var t = target
        var step = -0.02f * (w / total)
        repeat(10) {
            t = (t + step).coerceIn(0f, 0.9999f)
            val p = positionFor(t, bounds)
            if (avoid.none { it.containsWithPad(p, 24f) }) return t
            if (kotlin.math.abs(t - target) > maxBackoff) {
                step = -step  // 换方向继续探
            }
        }
        return target  // 实在找不到就压字
    }

    fun stop() {
        roamJob?.cancel()
        lookJob?.cancel()
        idleSleepJob?.cancel()
        roamJob = null
        lookJob = null
        idleSleepJob = null
    }

    /**
     * 受惊：用户点击落点距离鞍部猫当前位置 < 阈值（约 80dp）时触发。
     *
     * 行为：pose 切到 STARTLED 持续 ~3s，期间不动；用户**有意凑近**才触发，
     * 普通点击不会触发，符合"克制"原则。
     *
     * 60s 冷却避免连点反复触发。
     *
     * @return true 表示触发了受惊（让外层做配套的视觉效果，如跳起来）
     */
    fun startleIfNear(point: Offset, mascotCenter: Offset, thresholdPx: Float = 80f * 3f): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastLookAtMs < 60_000L) return false
        val dx = point.x - mascotCenter.x
        val dy = point.y - mascotCenter.y
        if (dx * dx + dy * dy > thresholdPx * thresholdPx) return false
        lastLookAtMs = now
        lookJob?.cancel()
        lookJob = scope.launch {
            _pose.value = Pose.STARTLED
            delay(3_000L)
            _pose.value = Pose.IDLE
        }
        return true
    }

    /**
     * 启动 idle 睡眠监控：超过 [thresholdMs]（默认 3 分钟）没有 bumpInteraction
     * 调用，鞍部猫切到 SLEEPING；任何 bumpInteraction 立刻醒回 IDLE。
     *
     * 仅 Home tab 启用（Map/Pet 那两个本来就是"看东西"的页面，不该睡）。
     */
    fun startIdleSleep(thresholdMs: Long = 3 * 60 * 1000L) {
        if (idleSleepJob?.isActive == true) return
        idleSleepJob = scope.launch {
            while (true) {
                delay(10_000L)  // 每 10s 检查一次
                val sinceMs = System.currentTimeMillis() - lastInteractionMs
                if (sinceMs > thresholdMs && _pose.value == Pose.IDLE) {
                    _pose.value = Pose.SLEEPING
                }
            }
        }
    }

    fun stopIdleSleep() {
        idleSleepJob?.cancel()
        idleSleepJob = null
    }

    /** 任何用户交互都调一下，重置 idle 计时器 + 醒回 IDLE。 */
    fun bumpInteraction() {
        lastInteractionMs = System.currentTimeMillis()
        if (_pose.value == Pose.SLEEPING) {
            _pose.value = Pose.IDLE
        }
    }

    /**
     * 玩家点击屏幕。
     *
     * **克制原则**：不是每次点击都响应。鞍部猫的酷在于"它就那样，但它在" ——
     * 大多数时候它不理你，偶尔（~15% 概率）滑过来探一眼，让用户偶遇式发现。
     * 60s 冷却避免连点造成"连滚带爬"的尴尬感。
     */
    fun lookAt(screenPoint: Offset, bounds: Rect, avoidRegions: List<Rect> = emptyList()) {
        val now = System.currentTimeMillis()
        if (now - lastLookAtMs < 60_000L) return        // 冷却：60s 内只响应一次
        if (Random.nextFloat() > 0.15f) return          // 概率门控：85% 不理你
        lastLookAtMs = now

        lookJob?.cancel()
        lookJob = scope.launch {
            when (_mode.value) {
                Mode.EDGE -> {
                    val targetTheta = nearestThetaOnRail(screenPoint, bounds)
                    // 慢慢溜过来，散步感而非跟踪
                    _theta.animateTo(targetTheta, spring(dampingRatio = 1f, stiffness = 25f))
                }
                Mode.INTERIOR -> {
                    val target = findFreeNear(screenPoint, bounds, avoidRegions)
                    _interiorX.animateTo(target.x, spring(dampingRatio = 1f, stiffness = 20f))
                    _interiorY.animateTo(target.y, spring(dampingRatio = 1f, stiffness = 20f))
                }
            }
        }
    }

    fun parkAt(theta: Float, pose: Pose = Pose.IDLE) {
        lookJob?.cancel()
        lookJob = scope.launch {
            _mode.value = Mode.EDGE
            _theta.animateTo(theta.coerceIn(0f, 0.9999f), spring(dampingRatio = 0.85f, stiffness = 80f))
            _pose.value = pose
        }
    }

    private suspend fun switchToInterior(bounds: Rect, avoid: List<Rect>) {
        // 从当前 EDGE 位置起飞 —— 切模式前同步 interior 坐标到当前边线点（避免瞬移）
        val railPoint = positionFor(_theta.value, bounds)
        _interiorX.snapTo(railPoint.x)
        _interiorY.snapTo(railPoint.y)
        _mode.value = Mode.INTERIOR
        // 起飞：往屏中心方向移一点点
        val firstStop = randomFreePoint(bounds, avoid)
        _interiorX.animateTo(firstStop.x, spring(dampingRatio = 1f, stiffness = 10f))
        _interiorY.animateTo(firstStop.y, spring(dampingRatio = 1f, stiffness = 10f))
    }

    private suspend fun returnToEdge(bounds: Rect) {
        val current = Offset(_interiorX.value, _interiorY.value)
        val targetTheta = nearestThetaOnRail(current, bounds)
        // 先飞到对应边线点，再切模式（避免瞬移）
        val railPoint = positionFor(targetTheta, bounds)
        _interiorX.animateTo(railPoint.x, spring(dampingRatio = 1f, stiffness = 14f))
        _interiorY.animateTo(railPoint.y, spring(dampingRatio = 1f, stiffness = 14f))
        _theta.snapTo(targetTheta)
        _mode.value = Mode.EDGE
    }

    /** 在 INTERIOR 安全区随机选个航点，绕开 avoid 区 */
    private fun randomFreePoint(bounds: Rect, avoid: List<Rect>): Offset {
        // 内部安全 margin：飞船 + 鞍部猫整体大约 50dp 宽，密度 ~3px/dp，外扩 80px
        val margin = 80f
        repeat(30) {
            val x = bounds.left + margin + Random.nextFloat() * (bounds.width - 2 * margin)
            val y = bounds.top + margin + Random.nextFloat() * (bounds.height - 2 * margin)
            val p = Offset(x, y)
            if (avoid.none { it.containsWithPad(p, 24f) }) return p
        }
        // 30 次找不到就返回屏中央，宁可压字也别死循环
        return bounds.center
    }

    private fun findFreeNear(point: Offset, bounds: Rect, avoid: List<Rect>): Offset {
        if (avoid.none { it.containsWithPad(point, 24f) }) return point
        // 螺旋搜索周围空地
        for (r in 1..10) {
            val radius = r * 30f
            for (i in 0..7) {
                val a = (i * Math.PI / 4).toFloat()
                val candidate = Offset(point.x + radius * kotlin.math.cos(a), point.y + radius * kotlin.math.sin(a))
                if (candidate.x in bounds.left..bounds.right && candidate.y in bounds.top..bounds.bottom &&
                    avoid.none { it.containsWithPad(candidate, 24f) }
                ) return candidate
            }
        }
        return point
    }

    // ─────────────────────────────────────────────────────────
    // EDGE 模式下的边判定（与之前一致）
    // ─────────────────────────────────────────────────────────

    private fun totalLength(bounds: Rect): Float = 2 * bounds.height + bounds.width

    fun edgeAt(theta: Float, bounds: Rect): Edge {
        val total = totalLength(bounds)
        val t = theta * total
        val h = bounds.height
        val w = bounds.width
        return when {
            t < h -> Edge.LEFT
            t < h + w -> Edge.BOTTOM
            else -> Edge.RIGHT
        }
    }

    fun positionFor(theta: Float, bounds: Rect): Offset {
        val total = totalLength(bounds)
        val t = theta * total
        val h = bounds.height
        val w = bounds.width
        return when {
            t < h -> Offset(bounds.left, bounds.top + t)
            t < h + w -> Offset(bounds.left + (t - h), bounds.bottom)
            else -> Offset(bounds.right, bounds.bottom - (t - h - w))
        }
    }

    private fun nearestThetaOnRail(point: Offset, bounds: Rect): Float {
        val candidates = listOf(
            Edge.LEFT to Offset(bounds.left, point.y.coerceIn(bounds.top, bounds.bottom)),
            Edge.BOTTOM to Offset(point.x.coerceIn(bounds.left, bounds.right), bounds.bottom),
            Edge.RIGHT to Offset(bounds.right, point.y.coerceIn(bounds.top, bounds.bottom))
        )
        val best = candidates.minByOrNull { (_, proj) -> (proj - point).getDistance() }!!
        return inverseThetaOf(best.first, best.second, bounds)
    }

    private fun inverseThetaOf(edge: Edge, p: Offset, bounds: Rect): Float {
        val total = totalLength(bounds)
        val h = bounds.height
        val w = bounds.width
        val t = when (edge) {
            Edge.LEFT -> p.y - bounds.top
            Edge.BOTTOM -> h + (p.x - bounds.left)
            Edge.RIGHT -> h + w + (bounds.bottom - p.y)
        }
        return (t / total).coerceIn(0f, 0.9999f)
    }
}

enum class Edge { LEFT, BOTTOM, RIGHT }

private fun Rect.containsWithPad(p: Offset, pad: Float): Boolean =
    p.x in (left - pad)..(right + pad) && p.y in (top - pad)..(bottom + pad)
