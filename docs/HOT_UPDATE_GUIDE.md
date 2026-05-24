# StudyBuddy v2 热更新发布教程

本文档说明如何通过热更新系统发布新版本 APK，让用户在 app 内自动收到更新提示。

---

## 📋 前置条件

1. **PocketBase 后台访问权限**
   - 地址：`https://your-pb-domain.com/_/`（替换为实际 PB 地址）
   - 需要 superuser 账号密码

2. **GitHub Releases 权限**
   - 能够创建 Release 并上传 APK
   - 或使用其他 CDN（需要公网可访问的 APK 下载链接）

3. **本地环境**
   - Java 17+（Android Studio JBR）
   - Git
   - adb（可选，用于测试）

---

## 🔄 发布流程（完整版）

### 步骤 1：修改版本号

编辑 `app-v2/build.gradle.kts`：

```kotlin
defaultConfig {
    applicationId = "com.studybuddy.v2"
    minSdk = 26
    targetSdk = 34
    versionCode = 30  // +1
    versionName = "2.7.2-beta"  // 更新版本号
    // ...
}
```

**规则**：
- `versionCode` 必须递增（整数）
- `versionName` 格式：`主版本.次版本.修订版-beta`
- 小修复：修订版 +1（如 2.7.1 → 2.7.2）
- 新功能：次版本 +1（如 2.7.2 → 2.8.0）
- 大重构：主版本 +1（如 2.8.0 → 3.0.0）

---

### 步骤 2：编译 APK

```bash
# 设置 JAVA_HOME（如果未设置）
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"

# 编译 debug APK
./gradlew :app-v2:assembleDebug

# APK 输出路径
# app-v2/build/outputs/apk/debug/app-v2-debug.apk
```

**验证编译**：
```bash
# 检查 APK 是否存在
ls -lh app-v2/build/outputs/apk/debug/app-v2-debug.apk

# 查看 APK 信息（可选）
aapt dump badging app-v2/build/outputs/apk/debug/app-v2-debug.apk | grep -E "package|versionCode|versionName"
```

---

### 步骤 3：上传 APK 到 GitHub Releases

```bash
# 创建 Git tag
git tag v2.7.2
git push origin v2.7.2

# 使用 gh CLI 创建 Release
gh release create v2.7.2 \
  app-v2/build/outputs/apk/debug/app-v2-debug.apk \
  --title "v2.7.2 - 修复说明" \
  --notes "## 更新内容

- 修复 XXX
- 新增 YYY

详见 RELEASE-v2.7.2.md"
```

**获取下载链接**：
```bash
# 方式 1：gh CLI
gh release view v2.7.2 --json assets --jq '.assets[0].url'

# 方式 2：手动构造
# https://github.com/你的用户名/StudyBuddy/releases/download/v2.7.2/app-v2-debug.apk
```

**替代方案（自建 CDN）**：
- 上传到阿里云 OSS / 腾讯云 COS
- 确保 APK 链接公网可访问
- 建议使用 HTTPS

---

### 步骤 4：在 PocketBase 创建更新记录

#### 4.1 登录 PocketBase Admin

1. 打开 `https://your-pb-domain.com/_/`
2. 使用 superuser 账号登录

#### 4.2 进入 `app_versions` collection

1. 左侧菜单 → Collections → `app_versions`
2. 点击右上角 **New record**

#### 4.3 填写字段

| 字段 | 值 | 说明 |
|------|-----|------|
| `versionCode` | `30` | 必须与 build.gradle.kts 一致 |
| `versionName` | `2.7.2-beta` | 显示给用户的版本号 |
| `downloadUrl` | `https://github.com/.../app-v2-debug.apk` | APK 下载链接 |
| `releaseNotes` | `修复 XXX\n新增 YYY` | 更新说明（支持换行） |
| `channel` | `beta` | 渠道（beta / stable） |
| `force` | `false` | 是否强制更新 |
| `minSupportedCode` | `26` | 最低支持的旧版本（可选） |

**示例**：
```json
{
  "versionCode": 30,
  "versionName": "2.7.2-beta",
  "downloadUrl": "https://github.com/Ayou/StudyBuddy/releases/download/v2.7.2/app-v2-debug.apk",
  "releaseNotes": "修复新用户绑定搭档引导\n优化多主题色板\n修复解绑页面文案",
  "channel": "beta",
  "force": false,
  "minSupportedCode": 26
}
```

#### 4.4 保存

点击 **Create** 保存记录。

---

### 步骤 5：测试热更新

#### 5.1 安装旧版本

```bash
# 安装旧版本 APK（versionCode < 30）
adb install -r app-v2-old.apk
```

#### 5.2 启动 app 并等待

1. 打开 StudyBuddy app
2. 等待 2-3 秒（app 启动后自动检查更新）
3. 应该弹出 `UpdateDialog` 提示新版本

#### 5.3 验证弹窗内容

- 标题：`发现新版本`
- 版本号：`2.7.2-beta`
- 更新说明：显示 `releaseNotes` 内容
- 按钮：`立即更新` / `稍后`（如果 `force=true` 则只有"立即更新"）

#### 5.4 点击"立即更新"

1. 浏览器打开下载链接
2. 下载 APK
3. 安装新版本
4. 重新打开 app，不再弹窗

---

## 🚀 快速发布脚本（推荐）

创建 `scripts/publish-update.sh`：

```bash
#!/bin/bash
set -e

# 参数
VERSION_CODE=$1
VERSION_NAME=$2
RELEASE_NOTES=$3
CHANNEL=${4:-beta}

if [ -z "$VERSION_CODE" ] || [ -z "$VERSION_NAME" ] || [ -z "$RELEASE_NOTES" ]; then
  echo "用法: ./publish-update.sh <versionCode> <versionName> \"<releaseNotes>\" [channel]"
  echo "示例: ./publish-update.sh 30 2.7.2-beta \"修复 XXX\" beta"
  exit 1
fi

echo "📦 开始发布 v$VERSION_NAME (code: $VERSION_CODE)"

# 1. 编译 APK
echo "🔨 编译 APK..."
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew :app-v2:assembleDebug

# 2. 创建 GitHub Release
echo "📤 上传到 GitHub..."
git tag "v$VERSION_NAME"
git push origin "v$VERSION_NAME"
gh release create "v$VERSION_NAME" \
  app-v2/build/outputs/apk/debug/app-v2-debug.apk \
  --title "v$VERSION_NAME" \
  --notes "$RELEASE_NOTES"

# 3. 获取下载链接
DOWNLOAD_URL=$(gh release view "v$VERSION_NAME" --json assets --jq '.assets[0].url')
echo "📥 下载链接: $DOWNLOAD_URL"

# 4. 写入 PocketBase（需要 PB_ADMIN_EMAIL 和 PB_ADMIN_PASSWORD 环境变量）
echo "📝 写入 PocketBase..."
PB_URL="https://your-pb-domain.com"

# 登录获取 token
TOKEN=$(curl -s -X POST "$PB_URL/api/admins/auth-with-password" \
  -H "Content-Type: application/json" \
  -d "{\"identity\":\"$PB_ADMIN_EMAIL\",\"password\":\"$PB_ADMIN_PASSWORD\"}" \
  | jq -r '.token')

# 创建更新记录
curl -X POST "$PB_URL/api/collections/app_versions/records" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"versionCode\": $VERSION_CODE,
    \"versionName\": \"$VERSION_NAME\",
    \"downloadUrl\": \"$DOWNLOAD_URL\",
    \"releaseNotes\": \"$RELEASE_NOTES\",
    \"channel\": \"$CHANNEL\",
    \"force\": false
  }"

echo "✅ 发布完成！"
echo "用户将在下次启动 app 时收到更新提示。"
```

**使用方式**：

```bash
# 设置 PB 管理员凭据（一次性）
export PB_ADMIN_EMAIL="admin@example.com"
export PB_ADMIN_PASSWORD="your-password"

# 发布更新
./scripts/publish-update.sh 30 2.7.2-beta "修复新用户绑定引导\n优化色板" beta
```

---

## 📊 热更新系统架构

### 客户端逻辑

**文件**：`app-v2/src/main/java/com/studybuddy/v2/ui/update/UpdateViewModel.kt`

**检查时机**：
1. App 启动后 2 秒（`V2NavGraph.kt` 中 `LaunchedEffect`）
2. 用户手动点击"检查更新"（Settings 页面）

**检查逻辑**：
```kotlin
// 1. 请求 PB API
GET /api/collections/app_versions/records?filter=channel='beta'&sort=-versionCode&limit=1

// 2. 比较 versionCode
if (latestVersionCode > BuildConfig.VERSION_CODE) {
    // 显示 UpdateDialog
}
```

### 服务端 Schema

**Collection**: `app_versions`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `versionCode` | Number | ✅ | 版本号（整数，递增） |
| `versionName` | Text | ✅ | 显示版本号（如 2.7.2-beta） |
| `downloadUrl` | URL | ✅ | APK 下载链接 |
| `releaseNotes` | Text | ✅ | 更新说明 |
| `channel` | Text | ✅ | 渠道（beta / stable） |
| `force` | Boolean | ❌ | 是否强制更新（默认 false） |
| `minSupportedCode` | Number | ❌ | 最低支持版本 |

**索引**：
- `channel` + `versionCode` 降序

---

## 🔧 常见问题

### Q1: 用户没有收到更新提示？

**排查步骤**：
1. 检查 PB 记录是否创建成功
2. 确认 `channel` 字段为 `beta`（客户端默认拉 beta）
3. 确认 `versionCode` 大于用户当前版本
4. 检查客户端网络连接（PB API 是否可访问）
5. 查看 Logcat：`adb logcat | grep UpdateViewModel`

### Q2: 点击"立即更新"后无法下载？

**原因**：
- `downloadUrl` 链接失效或不可访问
- GitHub Release 未设置为 public
- 用户网络无法访问 GitHub

**解决**：
- 使用国内 CDN（阿里云 OSS / 腾讯云 COS）
- 确保链接返回 APK 文件（Content-Type: application/vnd.android.package-archive）

### Q3: 如何回滚版本？

**方式 1**：删除 PB 记录
1. 进入 PB Admin → `app_versions`
2. 删除最新版本记录
3. 客户端下次检查时会拉到上一个版本

**方式 2**：改 `channel`
1. 把新版本的 `channel` 改为 `stable`
2. beta 用户不再收到该版本

### Q4: 强制更新如何使用？

**场景**：修复严重 bug，必须让用户更新

**操作**：
1. PB 记录中设置 `force: true`
2. 客户端弹窗只显示"立即更新"按钮，无法关闭
3. 用户必须更新才能继续使用

**注意**：谨慎使用，影响用户体验

### Q5: 如何区分 beta 和 stable 渠道？

**当前实现**：
- 客户端硬编码拉 `channel="beta"`
- 如需支持 stable，需修改 `UpdateViewModel.kt`

**建议**：
- beta：封测用户，频繁更新
- stable：正式用户，稳定版本

---

## 📝 发布检查清单

发布前确认：

- [ ] `versionCode` 已递增
- [ ] `versionName` 已更新
- [ ] APK 编译成功
- [ ] Git commit 已提交
- [ ] Git tag 已创建并推送
- [ ] GitHub Release 已创建
- [ ] APK 下载链接可访问
- [ ] PB 记录已创建
- [ ] 测试旧版本能收到更新提示
- [ ] 测试下载安装流程正常
- [ ] 更新说明文案准确

---

## 📚 相关文件

- `app-v2/build.gradle.kts` — 版本号配置
- `app-v2/src/main/java/com/studybuddy/v2/ui/update/UpdateViewModel.kt` — 更新检查逻辑
- `app-v2/src/main/java/com/studybuddy/v2/ui/update/UpdateDialog.kt` — 更新弹窗 UI
- `app-v2/src/main/java/com/studybuddy/v2/ui/nav/V2NavGraph.kt` — 启动时检查更新
- `BACKLOG.md` — 功能规划
- `RELEASE-v*.md` — 各版本发布说明

---

## 🎯 最佳实践

1. **版本号规范**
   - 小修复：修订版 +1
   - 新功能：次版本 +1，修订版归 0
   - 大重构：主版本 +1，次版本和修订版归 0

2. **发布节奏**
   - 小修复：随时发布（1-2 天）
   - 新功能：每周发布（7-10 天）
   - 大版本：每月发布（3-4 周）

3. **测试流程**
   - 本地测试 → 内部测试 → 小范围 beta → 全量发布
   - 关键功能必须在真机测试

4. **回滚预案**
   - 保留最近 3 个版本的 APK
   - 发现严重 bug 立即回滚
   - 回滚后修复 bug 再发布

5. **用户沟通**
   - 更新说明简洁明了
   - 重大变更提前通知
   - 收集用户反馈

---

最后更新：2026-05-24
