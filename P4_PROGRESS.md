# StudyBuddy v2 — P4 进度

> 主计划：`C:\Users\17142\.claude\plans\cached-sniffing-kite.md`
> 用户已确认顺序：A1 → D4 → C2 → 其他 P4 项

> ⚠️ **下一轮优化时立即提优先级：鞍部猫 L5 扩展**（受惊装死 / 首页睡眠 / Focus 陪伴位 / Pet 窝 / 跨页过渡）。
> 详见 memory `project_saddle_cat_L5_backlog.md`。在 A2/A3/A4/A5/A6/B 主轴推完后立即做，**优先于 D5/D6 其他鞍部猫小项**。

---

## 全局状态速览

| 子项 | 状态 | 备注 |
|---|---|---|
| A1 PbRealtime SSE 底层 | ✅ 完成 | `open · N ev` 已通 |
| A1 SSE 联通验证 | ✅ 完成 | Settings 自 ping 通 |
| D4 MascotDock 全局常驻 | ✅ 完成 | 三边漫游 + 旋转 + 整张贴边 + 慢悠悠步长 + 二级页不跟进 |
| L4 鞍部猫边线漫游 | ✅ 完成 | EDGE 三边 + 旋转 |
| L4 两态漫游 + 像素飞船 | ✅ 完成 | INTERIOR 模式 + Saddleship 飞船 + MascotAvoidRegistry 文字避让 + Home 试点接入 |
| C2 玻璃质感接入 4 处 | ✅ 完成 | BottomBar / Snackbar / FocusSetupSheet / Map 浮卡 全部接入；GlassSurface 改为"半透+高光+hairline"，**不用 blur**（Android self-blur 会糊掉内容） |
| A2 状态机 + Focus 双分栏 | ✅ 完成（骨架） | SyncFocusStateMachine（7态纯函数 reducer）+ PartnerPaneState（30s 心跳判离线）+ FocusViewModel 加 SYNC mode + partnerJob 订阅 PB STATUS + FocusScreen 双分栏 50/50 + 1dp coral 中线 + Countdown321Overlay。**A3 邀请发起 + sheet 待补**，当前 SYNC 入场会自动跑 IDLE→INVITING，等 A3 接 SyncInviteRepo |
| **新增** Quote Gallery 骨架 | ✅ 完成（骨架） | 话廊 = 灵光一闪句子记录，详见下方"Quote Gallery 设计笔记" |
| A3 邀请发起 + sheet | ⏸ 未开始 | |
| A4 FCM 推送 4 渠道 | ⏸ 未开始 | |
| A5 公共基金真扣款 | ⏸ 未开始 | |
| A6 解绑 7 天冷静期 | ⏸ 未开始 | |
| B1–B5 围栏 + 防切出 | ⏸ 未开始 | |
| D5 L3 交互感知 | ⏸ 未开始 | |
| D6 启动露脸 | ⏸ 未开始 | |

---

## 鞍部猫漫游层（D4 + L4 第一阶段）— 已落地的关键决策

**形象**
- `ui/pet/saddle/SaddleCatFrames.kt`：HeadOnly 7×10 + FullBody 13×15
- "呼吸"用 IDLE 整体亚像素 translationY，**矩阵不重排**（保护眼睛和上半张脸的连接处）
- 眨眼用 BLINK_1/2/3 三帧，70ms/帧，4–6s 一轮
- HEAD_ONLY 形态本身就是"探出半身"造型 —— 全身在屏内、用底边贴在屏幕边线上，不要走"中心贴边只露一半"的错误方案

**漫游**
- 只走**左/底/右** 三条边（去掉顶边）—— 避开各设备通知栏/摄像头开孔的适配差异
- θ ∈ [0, 1) U 形开放轨道：0 = 左边顶端，1 = 右边顶端
- 边判定 + 旋转：底=0°、左=90°、右=270°；脖子贴边、脸朝屏内
- 漫游节奏：25–50s 停留 + ±8%–18% 小步 + spring stiffness 6 dampingRatio 1.0（散步感）
- 跨边时 500ms tween 平滑旋转角度

**显隐**
- 仅 4 个 bottom tab（Home/Map/Pet/Me）显示；Stats/Fund/Settings/Focus/Auth 隐藏
- DataStore 控制：`unlockedSaddleCat` / `mascotDockEnabled` / `mascotDockCollapsedUntil`
- 解锁路径：Settings 长按 About 5 次后输入 `cc-pet-ahoy`

**布局修正（重要决策）**
- MascotDock 必须放在 ScaffoldBody 的"内容区 Box(weight=1f)"内层，不要包整个 NavGraph。这样 fillMaxSize 自动等于内容区，bounds.bottom 直接是 BottomBar 顶端，**不要再手填 bottomInset 估值**（之前 88dp 是瞎猜，会导致鞍部猫悬空在 BottomBar 上方留白处）。
- pointer 监听用 `awaitEachGesture + awaitFirstDown(pass = Initial)`，不消费 up 事件，避免和子节点 clickable 冲突。
- 跟手参数：lookAt EDGE 模式 stiffness 200 / dampingRatio 0.85，让"看一眼"动作明显但不弹跳。

**改动文件**
- 新：`ui/component/MascotDock.kt` / `ui/component/MascotRoamer.kt`
- 新：`ui/pet/saddle/*`（PixelMatrix / SaddleCatFrames / SaddleCatSprite / SaddleCatState）
- 改：`ui/nav/V2NavGraph.kt`（外层包 MascotDock + 只在 bottom tab 显示）
- 改：`data/repo/PreferencesStore.kt`（三个鞍部猫相关 key）

---

## L4 下一步 — INTERIOR 模式 + 像素飞船 + 文字避让

**已落地**
- `ui/pet/saddle/SaddleshipFrames.kt`：9×3 飞船 + 3 帧 thruster 循环（IDLE/THRUST_A/THRUST_B，180ms/帧）
- `ui/component/MascotAvoidRegistry.kt`：`MascotAvoidRegistry` + `LocalMascotAvoidRegistry` CompositionLocal + `Modifier.mascotAvoid()` 扩展
- `MascotRoamer.Mode`：EDGE / INTERIOR 双态；EDGE 每 3-5 轮 30% 概率切 INTERIOR
- INTERIOR 切换：从当前 EDGE 点起飞（snapTo 避免瞬移）→ 2-4 个航点逐个飞（spring stiffness 12）→ 飞回最近边
- 文字避让：`randomFreePoint` 30 次重试找空地、`findFreeNear` 螺旋搜索点击点附近空地
- `MascotDock` 分模式渲染：EDGE 用 HEAD_ONLY 旋转贴边；INTERIOR 用 HEAD_ONLY + coral 飞船叠加
- HomeScreen `Greeting` 三个 Text 加 `mascotAvoid()` 作为接入示范

**新建文件**
- `ui/component/MascotAvoidRegistry.kt`
- `ui/pet/saddle/SaddleshipFrames.kt`

**改动文件**
- `ui/component/MascotRoamer.kt`（加 Mode + 双状态 Animatable + 航点循环）
- `ui/component/MascotDock.kt`（CompositionLocalProvider + EdgeMascot / InteriorMascot 分支）
- `ui/home/HomeScreen.kt`（Greeting 三个 Text 接入示范）

**待补**
- 真正泛用前还需把更多页面（Map / Pet / Me）的关键 Text 挂上 `mascotAvoid()` —— 当前只 Home 试点
- 长按 MascotDock 任意位置切换 EDGE/INTERIOR 的"用户手动调度"入口
- INTERIOR 时鞍部猫朝向（往左飞 = gaze LEFT，往右飞 = gaze RIGHT）

---

## 完成 L4 第二阶段后回到白皮书主轴

按 `cached-sniffing-kite.md` 阶段交付顺序续推：
1. ~~**C2 玻璃 4 处接入**~~ ✅ 已完成
2. **A2 同步专注状态机 + Focus 双分栏** ← 下一步
3. **A3 邀请 sheet + A4 FCM 推送**
4. **A5 工作日断连 + B 系列围栏 + 防切出**
5. **A6 解绑冷静期 + D5/D6 鞍部猫感知与启动露脸**

---

## Quote Gallery（话廊）— 新增 P4 子项

**设计哲学**：一个不值得开 app 的小角落，塞进首页第二屏，要用户自己发现的浪漫。

**入口**
- HomeScreen 第二屏隐藏入口 —— 用户在主屏滚到底（ConnectorGrid 设置/历史等之后）继续下拉，overscroll 累计超 140dp 触发导航
- 不开新 tab，不破坏 4-tab 收敛节奏
- 提示极轻：滑到底显示 muted 文字 "↓ 再下滑一点"，hairline 随 progress 变长变深，触发时变 "↓ 松开，进入话廊"
- 删除原 "一个人也很好" 占位卡（用户确认无实际作用）

**数据**
- PB `quotes` collection（待手动建 schema）：authorId / pairId / text / source / visibility / createdAt
- visibility: PRIVATE（默认仅自己）/ PARTNER（写入时填 pairId，对端可见）
- QuoteRepo 列表 filter：`authorId='me' || (visibility='PARTNER' && pairId='myPairId')`

**UI**
- 极简列表 + 衬线 DisplayMd 大字 + hairline 分隔
- 每条：文本 + "— 来源" + 日期 + 可见性徽章
- 右下角圆形 + 按钮 → ComposeDialog（多行文本 + 来源 + "对 TA 可见" chip）

**改动文件**
- 新：`data/model/Models.kt`（Quote + QuoteVisibility）/ `data/repo/QuoteRepo.kt` / `ui/quote/QuoteScreen.kt` + `QuoteViewModel.kt`
- 改：`data/pb/PbConfig.kt`（QUOTES）/ `ui/nav/Screen.kt` + `V2NavGraph.kt` / `ui/home/HomeScreen.kt`（删 PartnerPresenceCard + NestedScrollConnection + QuotePortalHint）

**待补（下轮优先）**
- PocketBase Console 手建 `quotes` schema（authorId text / pairId text / text text / source text / visibility text / createdAt number）+ List/Create rule
- 触发过渡动画：当前是直接 navigate，要做整页淡出 + Quote 页向上滑入（"二屏揭开"感）
- 删除/编辑长按菜单
- 对端写入 PARTNER 条 → FCM 通知 "TA 写下了一句话"

---

## C2 玻璃接入笔记

**关键决策（务实主义）**
Android 的 `Modifier.blur` 是 **self-blur**（把 Composable 自身模糊），不是 iOS 那种背景磨砂。所以原 `GlassSurface.kt` 用 blur 包按钮/文本会把内容糊到看不清。要做真正的 backdrop blur 需要 API 33+ `RenderEffect.createBackdropEffect`，跨设备稳定性差且会掉帧。

**取而代之**：纯靠 `半透 tint (alpha 0.82) + 顶部 1px 高光渐变 + 1dp hairline` 三层叠加，**不用 blur**，跨 API 一致、零开销。视觉上仍能看出"层"。如果以后要升级到真磨砂，单点改 GlassSurface 即可。

**接入位置**
- `ui/nav/V2NavGraph.kt` BottomBar：包 `GlassPresets.Cream(shape=Rect)`
- `ui/component/AppSnackbar.kt` SnackbarBubble：换 `GlassPresets.Dark(shape=lg)`
- `ui/home/FocusSetupSheet.kt`：`ModalBottomSheet.containerColor = canvas.copy(alpha=0.82f)` 直接调通道
- `ui/map/MapScreen.kt`：顶部"你的位置"卡 + 底部"已开启位置"卡 都换 `GlassPresets.Cream`

---

## 验收速记
- `./gradlew :app-v2:assembleDebug` 用 `JAVA_HOME=C:/Program Files/Android/Android Studio/jbr`
- `adb -s 127.0.0.1:7555 install -r app-v2/build/outputs/apk/debug/app-v2-debug.apk`
- 截图：`adb -s 127.0.0.1:7555 exec-out screencap -p > xxx.png`（不要用 `screencap -p` 直接重定向，MUMU 兼容性问题）
