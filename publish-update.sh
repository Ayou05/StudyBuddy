#!/usr/bin/env bash
# 发布一个新版本到 PocketBase app_versions collection
# 用法: ./publish-update.sh <versionCode> <versionName> "<releaseNotes>" [channel]
#
# 流程:
#   1. 编译 release/debug APK
#   2. 上传 APK 到 PocketBase 文件存储（或用户提供的 URL）
#   3. 创建 app_versions 记录
#
# 例子:
#   ./publish-update.sh 27 "2.7.0-beta" "修复地图崩溃 + 新增主题色板" beta

set -e

VERSION_CODE="${1:?需要 versionCode}"
VERSION_NAME="${2:?需要 versionName}"
RELEASE_NOTES="${3:?需要 releaseNotes}"
CHANNEL="${4:-beta}"

APK_PATH="app-v2/build/outputs/apk/debug/app-v2-debug.apk"
PB_BASE="https://catclaw.cloud"
PB_EMAIL="${PB_EMAIL:-171425455@qq.com}"
PB_PASSWORD="${PB_PASSWORD:-20050530Lzk}"

echo "== Build APK =="
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew :app-v2:assembleDebug

if [ ! -f "$APK_PATH" ]; then
  echo "APK 不存在: $APK_PATH"
  exit 1
fi

APK_SIZE=$(stat -c%s "$APK_PATH" 2>/dev/null || stat -f%z "$APK_PATH")
SHA256=$(sha256sum "$APK_PATH" 2>/dev/null | cut -d' ' -f1 || shasum -a 256 "$APK_PATH" | cut -d' ' -f1)

echo "== Login as superuser =="
TOKEN=$(curl -sS "${PB_BASE}/api/collections/_superusers/auth-with-password" \
  -X POST -H "Content-Type: application/json" \
  -d "{\"identity\":\"${PB_EMAIL}\",\"password\":\"${PB_PASSWORD}\"}" \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "PB 登录失败"
  exit 1
fi

# 简化：直接把 APK 放到 GitHub Releases 或自建 CDN
# 当前模式：上传到 PB Files (使用一个固定的 collection "app_versions" 的 file 字段)
# 但 app_versions schema 只有 downloadUrl 字段，所以需要外部 URL
#
# 推荐流程：
#   1. 推到 GitHub Releases
#   2. downloadUrl = "https://github.com/Ayou05/StudyBuddy/releases/download/v${VERSION_NAME}/studybuddy-${VERSION_NAME}.apk"
#
# 这里假设你已经手动上传到 GitHub Releases

DOWNLOAD_URL="https://github.com/Ayou05/StudyBuddy/releases/download/v${VERSION_NAME}/studybuddy-${VERSION_NAME}.apk"

read -p "确认 downloadUrl = ${DOWNLOAD_URL} [回车继续 / Ctrl-C 取消]: "

NOW_MS=$(($(date +%s) * 1000))

echo "== Create app_versions record =="
RESULT=$(curl -sS "${PB_BASE}/api/collections/app_versions/records" \
  -X POST -H "Authorization: ${TOKEN}" -H "Content-Type: application/json" \
  -d "{
    \"versionCode\": ${VERSION_CODE},
    \"versionName\": \"${VERSION_NAME}\",
    \"downloadUrl\": \"${DOWNLOAD_URL}\",
    \"apkSize\": ${APK_SIZE},
    \"sha256\": \"${SHA256}\",
    \"releaseNotes\": \"${RELEASE_NOTES}\",
    \"channel\": \"${CHANNEL}\",
    \"force\": false,
    \"publishedAt\": ${NOW_MS}
  }")

if echo "$RESULT" | grep -q '"id"'; then
  echo "== 发布成功 =="
  echo "$RESULT" | head -c 300
  echo ""
  echo ""
  echo "记得把 APK 上传到 GitHub Releases:"
  echo "  gh release create v${VERSION_NAME} ${APK_PATH} -t \"v${VERSION_NAME}\" -n \"${RELEASE_NOTES}\""
else
  echo "== 失败 =="
  echo "$RESULT"
  exit 1
fi
