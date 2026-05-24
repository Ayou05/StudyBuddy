# StudyBuddy v2 — 完整 Backlog（截至 2026-05-24）

本文档汇总当前所有未完成功能，按优先级分组，方便后续推进。
封测期间用户能感知的功能均已上线（情侣/陪伴/专注 三大主线）。

---

## 🔴 P0 / 强烈建议下一轮做（核心陪伴感）

### 鞍部猫跨页持久化 L5
- ✅ 单例 `MascotState`（emotion / spookedCount / lastInteractAt）
- ✅ PetViewModel 互动会同步更新 MascotState（feed/play/stroke → happy）
- 待做：MascotDock 真正读 MascotState 渲染（当前还是 dock 内部 state）
- 待做：Focus 页"陪伴位"真正读 MascotState
- 待做：Pet 页"窝"占据角落
- 待做：跨页过渡动画
- 工程量：剩余 1 天

### Q 弹动画补完 - 眨眼
- 局部矩阵改变眼睛 row 2-3，每 30-60s 触发
- 工程量：3-5 小时

### 鞍部猫朋友们的更多动作
- 当前只有跳起 / 喂食 / 开心摇摆 / 呼吸
- 应该补：抚摸响应、清洁响应、被戳抖动、睡觉的呼吸起伏
- 工程量：1 天

---

## 🟡 P1 / 增强体验（封测后做）

### 横屏翻页时钟 + 沉浸活动（#63）
- Focus 页横屏自动进入沉浸模式
- 大时钟 + 鞍部猫桌面活动（散步、趴下、看时钟）
- 工程量：1-2 天

### Stats 多主题归档 UI
- 已有横向堆叠条 + 图例（v2.7.0 上线）
- 缺：归档/取消归档操作、归档主题列表分组、按时间筛选主题
- 工程量：1 天

### 地图 tab 增强
- 现状：双方坐标 + 距离 + 共享地标
- 缺：
  - 共享地标渲染（marker + 名字气泡）
  - 见面记录历史轨迹（地图叠加最近 7 天的轨迹）
  - 反向地理编码（地标自动命名）
  - 长按创建共享地标
- 工程量：2-3 天

### 解绑冷静期 - 7 天后真解绑
- 当前只有"启动冷静期 + 取消"
- 缺：服务端定时检查 cooldownEndsAt，到期后真切关系（pet 归档、relationship 状态改 EXPIRED）
- 工程量：1 天

### 多主题专注的色板
- 当前只能输入 hex
- 缺：Settings 里给 8-12 个预设色（鞍部风偏低饱和）
- 工程量：3 小时

### Moment 触发的更多事件
- 当前 5 种：见面开始/见面结束/共同停留/TA开始专注/工作日断连
- 可加：周年纪念、生日、季节问候、连续打卡里程碑
- 工程量：1 天 / 每种

---

## 🟢 P2 / 打磨（上线后慢慢做）

### EB Garamond 真字体
- 目前用 Compose Serif fallback
- 需要把字体 ttf 放进 res/font/，改 ClaudeType 用 FontFamily(Font(R.font.eb_garamond))
- 工程量：半天（需找到合适的开源字体）

### 真 backdrop blur
- BottomBar 当前是半透色
- 改用 RenderEffect.createBlurEffect（仅 Android 12+）
- 工程量：1 小时

### 反向地理编码
- meetings.locationName / pair_landmarks.name 当前留空
- 调高德 GeocodeSearch
- 工程量：半天

### FCM / UnifiedPush 真推送
- 当前只有 PB SSE 实时（app 在前台才收到）
- 锁屏推送需要 FCM 或 UnifiedPush
- FCM 中国可用性差，UnifiedPush 需厂商配置
- 工程量：1-2 天 + 厂商联调

---

## 🔵 长期 / 探索向

### 鞍部猫世界观扩展
- 朋友圈再扩到 12-16 种（添加水母、龙猫、刺猬变体等）
- "解锁"机制：累计专注达到某些里程碑解锁新品种
- 季节皮肤：圣诞戴帽、夏天戴墨镜

### Focus 多模式
- 当前只有倒计时 + 正计时
- 可加：番茄钟（25+5 循环）、52/17 节奏、自定义节拍

### 共建文档 / 时间胶囊
- 未来某天打开的"信件"
- 双人协同编辑的"约定列表"

### 拍立得功能
- 双人见面时，自动生成一张"双人合照"风格的便签
- 用 PB file 上传 + 模板合成

### 旅行回忆地图
- 见面记录自动生成旅行轨迹
- 时间轴 + 地图双视图

### 多语言
- 当前纯中文
- 未来加英文 / 日文（异地恋的国际场景）

---

## 已知小问题（可下一版修）

- StatsViewModel 的 `getCompletedSessionsBetween` 按 startedAt 落入哪天，跨午夜的 session 算到开始那天（不严格但够用）
- MapScreen 的 `partnerMarker` 是 mutable 状态 + AndroidView，重组时会重复创建（应该用 DisposableEffect 管理）
- Letter 的 readAt 用 1 作 placeholder（PB number required 不能 0），UI 端用 `letter.isRead()` 判断；但旧记录可能 readAt=null，需要兜底
- 便签墙图片上传单张限 5MB，没有客户端压缩（用户拍照原图可能超过）
- 鞍部猫的 8 个朋友的形态差异主要靠"主色 + 局部色块"，狐狸/兔子/刺猬有专属轮廓，但暹罗/水豚等只是色块差异
- AppMode 切换不会立刻刷新 Pet 页的衰减状态（要等下次 refresh）

---

## 数据/后端

### PocketBase schema
- 已建：users / relationships / status / sessions / messages / pets / funds / fund_transactions / unbind_requests / quotes / debts / letters / notes / meetings / pair_landmarks / focus_topics / checkins / landmarks / sync_invites
- status 表加了 coarseLat / coarseLng / lastLocAt
- 字段 default value 用代码补（PB 后台不支持，所以 number 字段不能用 0 作"未读"，用 1）

### 服务端待办
- 7 天冷静期到期的真解绑逻辑（PB hooks 或后端 cron）
- 周年/生日提醒（按 partnerSince 推算）
- PB file CDN 加速（如果用户多）

---

## 约定俗成（不再讨论）

- ❌ 不做 FCM / Firebase（中国不可用）
- ❌ 鞍部猫不做"养成"（它是漫游吉祥物）
- ❌ 不做"显式专注/娱乐切换" — 已通过 AppMode 隐性满足
- ❌ 便签墙不做无边记式无限画布（手机操作累）
- ❌ 写信不做 IM 化（保护 app 节奏）

---

最后更新：2026-05-24

---

## 发布流程（v2.6.0+ 新增热更新）

PB 端有 `app_versions` collection；App 启动 2s 后自动检查 channel="beta" 最新一条；versionCode 大于本机就弹 `UpdateDialog` 提示下载安装。

发布新版步骤（脚本：`publish-update.sh`）：
1. 改 `app-v2/build.gradle.kts` 的 versionCode +1、versionName 改新版
2. `./gradlew :app-v2:assembleDebug`
3. 用 `gh release create vX.Y.Z app-v2-debug.apk -t "vX.Y.Z" -n "更新说明"` 上传 GitHub Releases（或自建 CDN）
4. `./publish-update.sh <code> <name> "<notes>" beta`
   脚本登录 PB superuser → 新建一条 app_versions 记录 → 客户端就能拉到了

强制升级：把 `force=true` 写进记录，UI 不显示"稍后"按钮。
渠道：默认拉 `beta`，可在代码中改成 `stable` 给正式用户。
回滚：删除新版记录或改 channel，客户端下次启动检查到没新版就不弹。
