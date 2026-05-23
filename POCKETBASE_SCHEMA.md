# PocketBase Schema 上线前配置清单

> 上 https://catclaw.cloud/_/ 后台 Collections 标签页，点 New collection 创建以下 4 张表（Type=Base）。

字段类型对照：
- text = Plain text
- number = Number  
- bool = Bool
- json/relation 暂用 text 即可

## 1. quotes（话廊）

| Field | Type | Required | Notes |
|---|---|---|---|
| authorId | text | ✓ |  |
| pairId | text |  |  |
| text | text | ✓ | Max 5000 |
| source | text |  |  |
| visibility | text | ✓ | PRIVATE / PARTNER |
| createdAt | number | ✓ |  |

**API Rules**:
- List: `@request.auth.id != "" && (authorId = @request.auth.id || (visibility = "PARTNER" && pairId != ""))`
- View: 同上
- Create: `@request.auth.id != "" && authorId = @request.auth.id`
- Update / Delete: `@request.auth.id = authorId`

## 2. debts（账本）

| Field | Type | Required |
|---|---|---|
| pairId | text | ✓ |
| fromUserId | text | ✓ |
| toUserId | text | ✓ |
| unitCents | number | ✓ |
| count | number | ✓ |
| reason | text |  |
| createdAt | number | ✓ |
| settled | bool |  |
| settledAt | number |  |
| settledBy | text |  |

**API Rules**:
- List/View: `@request.auth.id != "" && (fromUserId = @request.auth.id || toUserId = @request.auth.id)`
- Create: `@request.auth.id != "" && (fromUserId = @request.auth.id || toUserId = @request.auth.id)`
- Update: `@request.auth.id = toUserId`（仅被欠方可结算）

## 3. sync_invites（同步专注邀请）

| Field | Type | Required |
|---|---|---|
| fromUserId | text | ✓ |
| toUserId | text | ✓ |
| plannedDurationMs | number | ✓ |
| mode | text | ✓ |
| goal | text |  |
| status | text | ✓ |
| createdAt | number | ✓ |
| expiresAt | number | ✓ |
| sessionId | text |  |

**API Rules**:
- List/View: `@request.auth.id != "" && (fromUserId = @request.auth.id || toUserId = @request.auth.id)`
- Create: `@request.auth.id = fromUserId`
- Update: `@request.auth.id = toUserId || @request.auth.id = fromUserId`

## 4. landmarks（地标 / 围栏）

| Field | Type | Required |
|---|---|---|
| userId | text | ✓ |
| name | text | ✓ |
| type | text | ✓ |
| lat | number | ✓ |
| lng | number | ✓ |
| radiusM | number | ✓ |
| multiplier | number |  |
| createdAt | number | ✓ |

**API Rules**: 全部 `@request.auth.id = userId`

---

建完之后话廊 / 账本 / 同步专注 / 围栏才能真正写入云端，否则会 404 报错"没保存上"。
