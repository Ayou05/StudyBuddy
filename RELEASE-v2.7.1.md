# StudyBuddy v2.7.1 发布说明

**发布日期**：2026-05-24  
**版本号**：versionCode 29 / versionName 2.7.1-beta  
**类型**：小版本修复（热更新）

---

## 🔧 修复内容

### 1. 新用户绑定搭档引导优化
**问题**：新用户登录后找不到绑定搭档入口，解绑页面提示"去我的页绑定"已过时（Me tab 已合并入 Settings）

**修复**：
- 改回 5 tab 布局：**今天 / 写信 / 陪伴 / 在哪 / 我的**
- 最右侧"我的" tab 直接进入 Settings，符合用户习惯
- 移除 Home 页右上角齿轮图标（Settings 已在 BottomBar）
- 移除 Home 页 ConnectorGrid 的"设置"入口（避免重复）
- 解绑页面文案改为："在主页绑定搭档后，这里可以解除关系。"

**影响**：新用户能更快找到绑定搭档入口，老用户习惯不变（Settings 更明显）

---

### 2. 多主题色板扩展
**问题**：创建专注主题时只能输入 hex 颜色，门槛高

**优化**：
- 色板从 6 个扩展到 **8 个鞍部风预设色**：
  - Coral（珊瑚，默认）
  - Amber（琥珀）
  - Sage（沉静绿）
  - Saddle Ink（鞍部猫色）
  - 暮色棕
  - 雾蓝灰
  - 薰衣草灰（新增）
  - 赭石灰（新增）
- 所有颜色均为低饱和暖灰/冷灰系，符合鞍部风美学

**影响**：用户创建主题时可直接点选预设色，无需手动输入 hex

---

### 3. 解绑页面文案叠加修复
**问题**：NoPartnerState 两行文字叠在一起显示

**修复**：精简为单行文案，布局更清晰

---

## 📦 技术细节

### 改动文件
- `app-v2/src/main/java/com/studybuddy/v2/ui/nav/Screen.kt` — bottomTabs 加回 Settings
- `app-v2/src/main/java/com/studybuddy/v2/ui/nav/V2NavGraph.kt` — Settings 路由加 showBackButton 参数
- `app-v2/src/main/java/com/studybuddy/v2/ui/settings/SettingsScreen.kt` — 作为 BottomBar tab 时不显示返回按钮
- `app-v2/src/main/java/com/studybuddy/v2/ui/home/HomeScreen.kt` — 移除齿轮图标 + ConnectorGrid 设置入口
- `app-v2/src/main/java/com/studybuddy/v2/ui/unbind/UnbindScreen.kt` — NoPartnerState 文案精简
- `app-v2/src/main/java/com/studybuddy/v2/ui/topic/TopicPicker.kt` — PALETTE 扩展到 8 色
- `app-v2/build.gradle.kts` — versionCode 26 → 29 / versionName 2.6.0 → 2.7.1

### 兼容性
- 向下兼容 v2.6.0 / v2.7.0
- 无数据迁移
- 无 PB schema 变更

---

## 🚀 下一步（v2.8.0）

1. **鞍部猫跨页持久化完整版**（P0，用户强烈期待）
   - MascotDock 真正读 MascotState 渲染
   - Focus 页"陪伴位"
   - Pet 页"窝"
   - 跨页过渡动画

2. **地图共享地标完整版**
   - 反向地理编码（地标自动命名）
   - 见面记录历史轨迹

3. **Stats 多主题归档 UI**
   - 归档/取消归档操作
   - 归档主题列表分组

---

## 📝 用户可见变化

- BottomBar 从 4 个变成 5 个 tab，最右侧是"我的"
- Home 页右上角齿轮图标消失（Settings 在 BottomBar）
- 创建专注主题时有 8 个预设色可选
- 解绑页面文案更清晰

---

最后更新：2026-05-24 23:58
