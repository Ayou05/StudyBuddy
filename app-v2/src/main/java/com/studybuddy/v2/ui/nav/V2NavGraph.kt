package com.studybuddy.v2.ui.nav

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studybuddy.v2.theme.ClaudeColors
import com.studybuddy.v2.theme.ClaudeRadius
import com.studybuddy.v2.theme.ClaudeSpacing
import com.studybuddy.v2.theme.ClaudeType
import com.studybuddy.v2.theme.appColors
import com.studybuddy.v2.ui.auth.AuthScreen
import com.studybuddy.v2.ui.component.AppIcon
import com.studybuddy.v2.ui.component.GlassPresets
import com.studybuddy.v2.ui.component.LocalSnackbarController
import com.studybuddy.v2.ui.component.MascotDock
import com.studybuddy.v2.ui.component.SnackbarController
import com.studybuddy.v2.ui.component.SnackbarHost
import com.studybuddy.v2.ui.component.mascotAvoid
import com.studybuddy.v2.ui.focus.FocusScreen
import com.studybuddy.v2.ui.fund.FundScreen
import com.studybuddy.v2.ui.home.HomeScreen
import com.studybuddy.v2.ui.map.MapScreen
import com.studybuddy.v2.ui.pet.PetScreen
import com.studybuddy.v2.ui.placeholder.PlaceholderScreen
import com.studybuddy.v2.ui.settings.SettingsScreen
import com.studybuddy.v2.ui.social.MeScreen
import com.studybuddy.v2.ui.stats.StatsScreen

@Composable
fun V2NavGraph(
    isLoggedIn: Boolean,
    navController: NavHostController = rememberNavController()
) {
    val snackbarController = remember { SnackbarController() }
    androidx.compose.runtime.CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
        SnackbarHost(controller = snackbarController) {
            V2NavGraphInner(isLoggedIn = isLoggedIn, navController = navController)
        }
    }
}

@Composable
private fun V2NavGraphInner(
    isLoggedIn: Boolean,
    navController: NavHostController
) {
    if (!isLoggedIn) {
        // 未登录：单 NavHost 只跑 Auth
        NavHost(navController = navController, startDestination = Screen.Auth.route) {
            composable(Screen.Auth.route) {
                AuthScreen(onAuthenticated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Home.route) {
                MainScaffold(navController = navController)
            }
        }
        return
    }
    MainScaffold(navController = navController)
}

@Composable
private fun MainScaffold(navController: NavHostController) {
    val colors = MaterialTheme.appColors
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Screen.Home.route

    val showBottomBar = bottomTabs.any { it.screen.route == currentRoute }
    // 鞍部猫在主页 / 信件 / 地图出现；共养是它的窝（PetScreen 内部接管）
    val showMascot = currentRoute in listOf(
        Screen.Home.route,
        Screen.Letter.route,
        Screen.Map.route
    )

    // 启动时自动检查更新（一次，静默；有新版才弹）
    val updateVm: com.studybuddy.v2.ui.update.UpdateViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)  // 给 SSE/启动让路
        updateVm.checkUpdate(manual = false)
    }

    ScaffoldBody(
        navController = navController,
        colors = colors,
        showBottomBar = showBottomBar,
        currentRoute = currentRoute,
        showMascot = showMascot
    )

    com.studybuddy.v2.ui.update.UpdateDialog(viewModel = updateVm)
}

@Composable
private fun ScaffoldBody(
    navController: NavHostController,
    colors: com.studybuddy.v2.theme.AppColors,
    showBottomBar: Boolean,
    currentRoute: String,
    showMascot: Boolean
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        // 内容区 + 鞍部猫漫游层（共享 Box，bounds 自动等于内容区，不用估 bottomInset）
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            MascotDock(
                visible = showMascot,
                allowSleep = currentRoute == Screen.Home.route,
                onOpenMascotPage = {
                    navController.navigate(Screen.Mascot.route) {
                        launchSingleTop = true
                    }
                }
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    enterTransition = {
                        androidx.compose.animation.fadeIn(
                            androidx.compose.animation.core.tween(220)
                        ) + androidx.compose.animation.slideInVertically(
                            animationSpec = androidx.compose.animation.core.tween(220),
                            initialOffsetY = { 24 }
                        )
                    },
                    exitTransition = {
                        androidx.compose.animation.fadeOut(
                            androidx.compose.animation.core.tween(160)
                        )
                    },
                    popEnterTransition = {
                        androidx.compose.animation.fadeIn(
                            androidx.compose.animation.core.tween(220)
                        )
                    },
                    popExitTransition = {
                        androidx.compose.animation.fadeOut(
                            androidx.compose.animation.core.tween(160)
                        )
                    }
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            onStartFocus = { navController.navigate(Screen.Focus.build("SINGLE")) },
                            onStartSyncFocus = { navController.navigate(Screen.Focus.build("SYNC")) },
                            onOpenFund = { navController.navigate(Screen.Ledger.route) },
                            onOpenStats = { navController.navigate(Screen.Stats.route) },
                            onOpenHistory = { navController.navigate(Screen.Stats.route) },
                            onOpenNoteWall = { navController.navigate(Screen.CoRaise.route) }
                        )
                    }
                    composable(Screen.Map.route) {
                        MapScreen()
                    }
                    composable(Screen.Pet.route) {
                        PetScreen()
                    }
                    composable(Screen.Letter.route) {
                        com.studybuddy.v2.ui.letter.LetterScreen()
                    }
                    composable(Screen.CoRaise.route) {
                        com.studybuddy.v2.ui.coraise.CoRaiseScreen(
                            onOpenSettings = { navController.navigate(Screen.Settings.route) },
                            onOpenCodex = { navController.navigate(Screen.PetCodex.route) }
                        )
                    }
                    composable(Screen.Explore.route) {
                        com.studybuddy.v2.ui.explore.ExploreScreen()
                    }
                    composable(Screen.Me.route) {
                        MeScreen(
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                            onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                            onNavigateToLedger = { navController.navigate(Screen.Ledger.route) },
                            onNavigateToQuote = { navController.navigate(Screen.Quote.route) },
                            onNavigateToFund = { navController.navigate(Screen.Fund.route) },
                            onLoggedOut = {
                                navController.navigate(Screen.Auth.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        route = Screen.Focus.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("mode") {
                                type = androidx.navigation.NavType.StringType
                                defaultValue = "SINGLE"
                            }
                        )
                    ) { entry ->
                        val mode = entry.arguments?.getString("mode") ?: "SINGLE"
                        FocusScreen(
                            focusType = mode,
                            onDismiss = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Fund.route) {
                        FundScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Stats.route) {
                        StatsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenDay = { ymd -> navController.navigate(Screen.StatsDay.build(ymd)) }
                        )
                    }
                    composable(Screen.Settings.route) {
                        val isBottomTab = bottomTabs.any { it.screen.route == Screen.Settings.route }
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenUnbind = { navController.navigate(Screen.Unbind.route) },
                            showBackButton = !isBottomTab
                        )
                    }
                    composable(Screen.Quote.route) {
                        com.studybuddy.v2.ui.quote.QuoteScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Ledger.route) {
                        com.studybuddy.v2.ui.ledger.LedgerScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Mascot.route) {
                        com.studybuddy.v2.ui.mascot.MascotPage(onBack = { navController.popBackStack() })
                    }
                    composable(
                        route = Screen.PetCodex.route,
                        enterTransition = {
                            androidx.compose.animation.slideInVertically(
                                animationSpec = androidx.compose.animation.core.tween(280),
                                initialOffsetY = { full -> full / 3 }
                            ) + androidx.compose.animation.fadeIn(
                                androidx.compose.animation.core.tween(220)
                            )
                        },
                        exitTransition = {
                            androidx.compose.animation.fadeOut(
                                androidx.compose.animation.core.tween(160)
                            )
                        },
                        popExitTransition = {
                            androidx.compose.animation.slideOutVertically(
                                animationSpec = androidx.compose.animation.core.tween(220),
                                targetOffsetY = { full -> full / 3 }
                            ) + androidx.compose.animation.fadeOut(
                                androidx.compose.animation.core.tween(180)
                            )
                        }
                    ) {
                        com.studybuddy.v2.ui.pet.CodexScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Unbind.route) {
                        com.studybuddy.v2.ui.unbind.UnbindScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = Screen.StatsDay.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("ymd") {
                                type = androidx.navigation.NavType.StringType
                            }
                        )
                    ) { entry ->
                        val ymd = entry.arguments?.getString("ymd") ?: ""
                        com.studybuddy.v2.ui.stats.StatsDayDetailScreen(
                            ymd = ymd,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Auth.route) {
                        com.studybuddy.v2.ui.auth.AuthScreen(onAuthenticated = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        })
                    }
                }
            }
        }
        // 底部 4 tab 导航条
        if (showBottomBar) {
            BottomBar(
                currentRoute = currentRoute,
                onTabClick = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
private fun BottomBar(currentRoute: String, onTabClick: (Screen) -> Unit) {
    val colors = MaterialTheme.appColors
    GlassPresets.Cream(
        modifier = Modifier
            .fillMaxWidth()
            .mascotAvoid(),  // BottomBar 整体注册避让
        shape = RoundedCornerShape(0.dp),
        hairline = false,  // 顶部 hairline 由 border 替代
        highlight = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = colors.hairlineSoft,
                    shape = RoundedCornerShape(0.dp)
                )
                .navigationBarsPadding()
                .padding(horizontal = ClaudeSpacing.sm, vertical = ClaudeSpacing.xs),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomTabs.forEach { spec ->
                val selected = currentRoute == spec.screen.route
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(ClaudeRadius.md))
                        .clickable { onTabClick(spec.screen) }
                        .padding(horizontal = ClaudeSpacing.md, vertical = ClaudeSpacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppIcon(
                        name = spec.iconName,
                        size = 22.dp,
                        tint = if (selected) ClaudeColors.Primary else colors.muted
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        spec.label,
                        style = ClaudeType.CaptionUppercase,
                        color = if (selected) ClaudeColors.Primary else colors.muted
                    )
                }
            }
        }
    }
}
