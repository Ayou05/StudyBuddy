# PocketBase 后台字段补全清单

登录 https://catclaw.cloud/_/ 后台，依次给以下 collection 添加字段：

## 1. letters
- pairId (text, required)
- authorId (text, required)
- kind (text, 非必填)
- text (text, required)
- createdAt (number, 非必填)
- readAt (number, 非必填)

## 2. notes
- pairId (text, required)
- authorId (text, required)
- text (text, 非必填)
- imageUrls (file, 非必填, maxSelect: 4, maxSize: 5MB, mimeTypes: image/jpeg,image/png,image/webp)
- landmarkId (text, 非必填)
- meetingId (text, 非必填)
- positionX (number, 非必填)
- positionY (number, 非必填)
- createdAt (number, 非必填)

## 3. meetings
- pairId (text, required)
- startedAt (number, required)
- endedAt (number, 非必填)
- centerLat (number, 非必填)
- centerLng (number, 非必填)
- durationMs (number, 非必填)
- locationName (text, 非必填)

## 4. pair_landmarks
- pairId (text, required)
- name (text, 非必填)
- lat (number, required)
- lng (number, required)
- firstVisitedAt (number, 非必填)
- visitCount (number, 非必填)
- lastVisitedAt (number, 非必填)

## 5. focus_topics
- userId (text, required)
- name (text, 非必填)
- colorHex (text, 非必填)
- totalFocusMs (number, 非必填)
- sessionCount (number, 非必填)
- createdAt (number, 非必填)
- archivedAt (number, 非必填)

## 6. fund_transactions
- pairId (text, required)
- type (text, 非必填)
- amountCents (number, 非必填)
- note (text, 非必填)
- byUserId (text, 非必填)
- at (number, 非必填)
- voided (bool, 非必填)
- voidedAt (number, 非必填)
- voidedBy (text, 非必填)

## 7. unbind_requests
- pairId (text, required)
- byUserId (text, required)
- createdAt (number, 非必填)
- cooldownEndsAt (number, 非必填)
- cancelled (bool, 非必填)
- cancelledAt (number, 非必填)
- cancelledBy (text, 非必填)

## 8. status (补充 3 个字段)
- coarseLat (number, 非必填)
- coarseLng (number, 非必填)
- lastLocAt (number, 非必填)

---

**完成后**，写信和便签墙功能就能正常使用了。
