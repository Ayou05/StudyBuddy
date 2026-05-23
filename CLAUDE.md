# StudyBuddy v2 — 双人专注 App

异地大学情侣的关系容器。Kotlin + Jetpack Compose 单模块。

## 模块

- `app-v2/` —— **当前唯一模块**。所有开发都在这里。
- `app/` 已删除（v1 弃用，prompt 明确不复用一行 v1 代码）
- `functions/` 已删除（Firebase Cloud Functions 不再使用）

## 技术栈

- Kotlin + Jetpack Compose（BOM 2024.04，Compiler 1.5.8）+ Material 3
- MVVM + Hilt DI + Kotlin Coroutines/Flow
- **PocketBase**（自建后端，部署在 catclaw.cloud）
  - REST + Realtime SSE（OkHttp-sse）
  - 邮箱密码认证 + token 存 DataStore
  - File field 直接附件上传（便签图片 / 头像 / 宠物图）
- Room + DataStore 本地缓存（PB 是 source of truth，Room 仅做缓存 + 离线查询）
- 高德地图 SDK（`MapsInitializer.updatePrivacyShow/Agree` 已合规初始化）
- WorkManager 定时任务（WeekdayBreakWorker 每日零点判定工作日断连）
- 鞍部猫像素吉祥物（Compose Canvas drawRect 渲染 0/1 矩阵，不走 PNG）

## 不要做的事（关键决策点）

- ❌ **不要加 Firebase / FCM / Firestore / google-services 任何东西** —— v2 全部走 PocketBase
- ❌ 不要回头碰 v1 代码（已删）
- ❌ 不要把鞍部猫做成"养成宠物"（它是漫游吉祥物，跟橘猫/暹罗分两层）
- ❌ 不要加显式"专注/娱乐"模式切换 —— 用姿态系统自动感知
- ❌ 不要在不放 google-services.json 的情况下加 firebase-bom 依赖（gradle 会断）

## 关键设计决策

- **三层表面节奏**：canvas（米色底）→ cream-card → dark-mockup（深色嵌入）
- **TextStyle 禁止 .copy() 和 PlatformTextStyle**
- **鞍部猫**："它就那样，但它在" —— 不跟踪用户、不为响应而切表情
- **30 字限制纸飞机**：保护 app 节奏，不让飞机退化成 IM
- **双人姿态系统**：单次定位 + 时间日历 + 双人距离辅助加成
- **苹果手记式延迟提示**：不当场打扰，等用户回家再问"今天去了 XX 要不要记下来"

## 项目入口

- 白皮书：`E:\Togedy\Kimi_Agent_双人专注App\异地自习室App_需求白皮书.md`
- 代码：`E:\Togedy\StudyBuddy\app-v2\`
- 启动入口：`V2MainActivity.kt` → `V2NavGraph.kt` → Home 路由
- BottomBar 4 tab：Home / 信件 / 共养 / 探索（齿轮在 Home 右上角）
- 推送：`PUSH_NOTIFICATION_README.md`（PocketBase Realtime SSE + 本地通知）

## v2 当前进度（P5 W1）

✅ 双人闭环（同步专注 + 邀请 sheet + 承诺账本 + 工作日断连 Worker）
✅ 围栏 + 加成徽章
✅ 玻璃质感 4 处 + Liquid Glass BottomBar
✅ 鞍部猫吉祥物（漫游 + 三手势 + 受惊 + 首页睡眠 + 跨页持久状态 + /mascot 全屏）
✅ 宠物自动衰减系统（粘性密码）
✅ 宠物起名 + EGG 自动孵化
✅ Stats 下钻（点某天看详细 sessions）
✅ 话廊 + EB Garamond 字体准备就绪（res/font/ 待放 ttf）
✅ BottomBar 重构（Me 删除合并 Settings / Pet → 共养 / 加 Letter + Explore）
✅ 信件 + 纸飞机双载体（30 字限制 + 折纸飞出动画）
✅ 姿态系统（多维加权 + Home 顶部诗化文案）
✅ 探索 tab 四种姿态形态
✅ 便签墙（拖动 + 长按删除，PB 文件上传 P6 接）
✅ 解绑冷静期 + 宠物冬眠

## 待续（P5 W2 / P6）

- 多主题专注（鞍部风形态打磨中）
- 共同停留 detector + 苹果手记式 SoftPromptCard
- 见面 detector + 见面详情页 + 离别便签
- PB file 上传（便签真接图片）
- 鞍部猫跨页过渡完整版（Singleton + Focus 陪伴位 + Pet 窝）
- 真 backdrop blur + 4K 治愈壁纸
- UnifiedPush / 厂商推送（替代 FCM 的方案）
