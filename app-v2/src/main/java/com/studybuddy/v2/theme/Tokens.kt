package com.studybuddy.v2.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ═════════════════════════════════════════════════════════════════════════════
// Claude Design Language — Color Tokens
// 镜像 awesome-design-md/design-md/claude/DESIGN.md
// 三色 Trinity：cream canvas + warm coral + dark navy
// ═════════════════════════════════════════════════════════════════════════════
object ClaudeColors {
    // Brand & Accent
    val Primary        = Color(0xFFCC785C)  // 暖珊瑚 — 唯一 CTA 主色
    val PrimaryActive  = Color(0xFFA9583E)  // 按压色
    val PrimaryDisabled = Color(0xFFE6DFD8) // disabled cream-tinted

    val AccentTeal     = Color(0xFF5DB8A6)  // 次级强调（status / pet 健康）
    val AccentAmber    = Color(0xFFE8A55A)  // 暖色辅助 — 用于 streak / partnerB

    // Text
    val Ink            = Color(0xFF141413)  // 主要文字
    val BodyStrong     = Color(0xFF252523)
    val Body           = Color(0xFF3D3D3A)
    val Muted          = Color(0xFF6C6A64)
    val MutedSoft      = Color(0xFF8E8B82)
    val OnPrimary      = Color(0xFFFFFFFF)
    val OnDark         = Color(0xFFFAF9F5)  // cream-tinted white
    val OnDarkSoft     = Color(0xFFA09D96)

    // Surfaces — 三层节奏
    val Canvas              = Color(0xFFFAF9F5)  // L0 页面底
    val SurfaceSoft         = Color(0xFFF5F0E8)  // L0.5 浅分隔带
    val SurfaceCard         = Color(0xFFEFE9DE)  // L1 feature 卡片
    val SurfaceCreamStrong  = Color(0xFFE8E0D2)  // L1.5 强调表面
    val SurfaceDark         = Color(0xFF181715)  // L2 dark mockup 卡
    val SurfaceDarkElevated = Color(0xFF252320)  // L2 上的浮层
    val SurfaceDarkSoft     = Color(0xFF1F1E1B)  // L2 内的次级

    // Lines
    val Hairline       = Color(0xFFE6DFD8)
    val HairlineSoft   = Color(0xFFEBE6DF)

    // Semantic
    val Success        = Color(0xFF5DB872)  // 入金 / online dot / 健康进度
    val Warning        = Color(0xFFD4A017)  // 出金 / 警示
    val Error          = Color(0xFFC64545)  // 校验错误

    // ─── Extended for StudyBuddy（不在 Claude 原 DESIGN.md，但保留为扩展）─────
    // 搭档色 —— 用于 partner 头像描边、partner 状态标识。
    // 故意让 partnerA 取 coral 系派生（avoid 单纯红粉），partnerB 用 accent-teal 派生
    val PartnerA       = Color(0xFFD17A6E)  // 搭档 A（你）— 微红的暖
    val PartnerB       = Color(0xFF6AAE9C)  // 搭档 B（TA）— accent-teal 同族

    // 鞍部猫专属色：石板冷灰蓝（电子感）+ 飞船独立暖橙（区分主体）
    val MascotInk      = Color(0xFF4F5970)
    val MascotShip     = Color(0xFFD4845A)  // 暖橙铜，飞船独立色，明显对比
}

// ═════════════════════════════════════════════════════════════════════════════
// 暗色模式 —— Claude 设计语言 dark 走 surface-dark 基底 + cream-tinted on-dark
// ═════════════════════════════════════════════════════════════════════════════
object ClaudeColorsDark {
    val Primary        = Color(0xFFE89378)  // dark 下提亮 coral 保对比度
    val PrimaryActive  = Color(0xFFCC785C)
    val PrimaryDisabled = Color(0xFF3A322B)

    val AccentTeal     = Color(0xFF7ECFBC)
    val AccentAmber    = Color(0xFFF2BA75)

    val Ink            = Color(0xFFFAF9F5)
    val BodyStrong     = Color(0xFFE8E2D5)
    val Body           = Color(0xFFC9C3B6)
    val Muted          = Color(0xFF9A958A)
    val MutedSoft      = Color(0xFF6F6B62)
    val OnPrimary      = Color(0xFF1A1310)
    val OnDark         = Color(0xFFFAF9F5)
    val OnDarkSoft     = Color(0xFFA09D96)

    val Canvas              = Color(0xFF0F0E0C)  // 比 surface-dark 还低一档
    val SurfaceSoft         = Color(0xFF161412)
    val SurfaceCard         = Color(0xFF1F1E1B)
    val SurfaceCreamStrong  = Color(0xFF2A2724)
    val SurfaceDark         = Color(0xFF181715)
    val SurfaceDarkElevated = Color(0xFF252320)
    val SurfaceDarkSoft     = Color(0xFF1F1E1B)

    val Hairline       = Color(0xFF35312C)
    val HairlineSoft   = Color(0xFF2B2823)

    val Success        = Color(0xFF7DD68F)
    val Warning        = Color(0xFFE8BA45)
    val Error          = Color(0xFFE07474)

    val PartnerA       = Color(0xFFE0958A)
    val PartnerB       = Color(0xFF8BC5B5)

    // 鞍部猫专属色（深色模式版）—— 提亮一档；飞船保持暖橙
    val MascotInk      = Color(0xFFB0BAD0)
    val MascotShip     = Color(0xFFE8A071)
}

// ═════════════════════════════════════════════════════════════════════════════
// Spacing — 4dp base unit
// ═════════════════════════════════════════════════════════════════════════════
object ClaudeSpacing {
    val xxs = 4.dp
    val xs  = 8.dp
    val sm  = 12.dp
    val md  = 16.dp
    val lg  = 24.dp
    val xl  = 32.dp
    val xxl = 48.dp
    // Section padding（桌面 96dp，移动端降到 48dp 保 visual rhythm）
    val section = 48.dp
    // Page horizontal padding
    val pageHorizontal = 20.dp
    // 最小触控目标
    val minTouchTarget = 48.dp
}

// ═════════════════════════════════════════════════════════════════════════════
// Border Radius — Claude scale
// ═════════════════════════════════════════════════════════════════════════════
object ClaudeRadius {
    val xs   = 4.dp     // 细微标签
    val sm   = 6.dp     // 小按钮
    val md   = 8.dp     // 标准按钮 / 输入框 / category tab
    val lg   = 12.dp    // 内容卡片 / pricing / model-comparison
    val xl   = 16.dp    // hero illustration container
    val pill = 9999.dp  // 胶囊徽章
}
