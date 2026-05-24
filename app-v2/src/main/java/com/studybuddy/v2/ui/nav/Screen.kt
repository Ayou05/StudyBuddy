package com.studybuddy.v2.ui.nav

sealed class Screen(val route: String) {
    object Auth     : Screen("auth")
    object Register : Screen("register")
    object Home     : Screen("home")
    object Letter   : Screen("letter")    // 信件 + 飞机 tab
    object CoRaise  : Screen("coraise")   // 共养 tab（Pet / 账本 / 基金子页）
    object Explore  : Screen("explore")   // 探索 tab（按姿态自适应）

    // 旧 tab：Map / Pet / Me 不再上 BottomBar，但路由保留兼容（CoRaise 内部分跳）
    object Map      : Screen("map")
    object Pet      : Screen("pet")
    object Me       : Screen("me")        // deprecated，渐进迁移到 Settings

    object Focus    : Screen("focus?mode={mode}") {
        const val baseRoute = "focus"
        fun build(mode: String = "SINGLE") = "$baseRoute?mode=$mode"
    }
    object Fund     : Screen("fund")
    object Stats    : Screen("stats")
    object Settings : Screen("settings")
    object Quote    : Screen("quote")
    object Ledger   : Screen("ledger")
    object Mascot   : Screen("mascot")
    object PetCodex : Screen("pet/codex")
    object Unbind   : Screen("unbind")
    object StatsDay : Screen("stats/day/{ymd}") {
        const val baseRoute = "stats/day"
        fun build(ymd: String) = "$baseRoute/$ymd"
    }
}

data class TabSpec(
    val screen: Screen,
    val label: String,
    val iconName: String   // drawable name without prefix
)

val bottomTabs = listOf(
    TabSpec(Screen.Home,     "今天", "home"),
    TabSpec(Screen.Letter,   "写信", "mail"),
    TabSpec(Screen.CoRaise,  "陪伴", "pet"),
    TabSpec(Screen.Map,      "在哪", "map"),
    TabSpec(Screen.Settings, "我的", "settings")
)

