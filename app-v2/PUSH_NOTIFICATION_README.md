# 推送通知接入说明（PocketBase 路线）

## 决策：v2 不使用 FCM / Firebase

理由：
- FCM 在中国大陆不稳定（需谷歌服务，国行 ROM 大量缺失）
- Firebase 项目配置 + google-services.json 流程繁琐
- v2 已有 PocketBase Realtime SSE 跑实时通信，再叠 FCM 是重复
- 早期白皮书写的 FCM 已废弃；本文件是新决策的来源

## v2 推送实际方案

### 前台（app 在前台或后台进程未杀）

**走 PocketBase Realtime SSE**（已跑通）：
- `PbRealtime.kt` 单例长连接，`GET /api/realtime` + 订阅 status / sync_invites / fund_transactions / unbind_requests / letters
- OkHttp-sse 实现，断线 1→30s 指数退避自动重连
- `HomeViewModel.startInviteWatcher()` 已订阅，邀请到达 → SyncInviteSheet 立刻弹

### 杀进程兜底（app 完全退出）

**没有真正的推送**——这是 v2 已知 limitation。补救机制：

1. **启动补漏**：app 启动 30s 内调 `inviteRepo.fetchPendingForMe()`，把杀进程期间错过的 PENDING 邀请拉出来
2. **本地通知**：前台收到 SSE 事件时，用 Android 原生 `NotificationManager` 弹本地通知（锁屏可见）
3. **未来方案**（不依赖 FCM）：
   - **UnifiedPush**：开源协议，国内有 ntfy / Gotify 等开源服务可对接
   - **小米推送 / 华为推送 / 厂商推送聚合**：覆盖国内主要 ROM
   - **WebSocket 长连保活 service**：自建前台 service 维持 SSE 连接（耗电，但完全不依赖谷歌）

P5/P6 阶段选 **方案 1+2 组合**，不接厂商推送（工程量大、各家配置不同）。

## 当前状态

✅ SSE 已跑通（PbRealtime / HomeViewModel.startInviteWatcher）
✅ 启动补漏已实现（inviteRepo.fetchPendingForMe）
⬜ 本地通知 NotificationManager 还没接 —— 当前 app 内只是 SyncInviteSheet 弹窗，锁屏看不见
⬜ 4 通知 channel 还没建（sync_invite / state_change / social / system）
⬜ UnifiedPush / 厂商推送是更后期的事

## 何时实施"本地通知"

P5 阶段对锁屏推送需求不强（用户基本会在前台 / 切回看 SSE）。
等用户反馈"杀 app 收不到邀请"再做，预计 2-3 小时工程量：
- `data/notification/NotificationChannelHelper.kt`：建 4 channel
- `data/notification/LocalPushNotifier.kt`：在 SSE 收到 invite/state-change 事件时弹本地通知
- `AndroidManifest.xml` 加 `POST_NOTIFICATIONS` 权限（Android 13+）

## 不要做的事

- ❌ 加 firebase-bom / firebase-messaging 依赖
- ❌ apply google-services plugin
- ❌ 写 FirebaseMessagingService
- ❌ 在白皮书里再提 FCM 当推送方案

如果未来真要接厂商推送，走 UnifiedPush 协议或国内推送聚合 SDK（个推 / 极光），**不要回头碰 FCM**。
