"""
PocketBase 0.23+ Collections 自动创建器

绕开 Web Console，直接用 _superusers API 建表。
适配 PB 0.23+ 的新 schema 格式（fields 数组，非旧的 schema 数组）。

使用：
  python create_collections.py
"""
import json
import sys
import urllib.request
import urllib.error
from pathlib import Path


def http(method, url, headers=None, body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, method=method, data=data)
    req.add_header("Content-Type", "application/json")
    if headers:
        for k, v in headers.items():
            req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8") or "{}")
    except urllib.error.HTTPError as e:
        txt = e.read().decode("utf-8", errors="ignore")
        try:
            return e.code, json.loads(txt)
        except Exception:
            return e.code, {"raw": txt}


def admin_auth(pb_url, email, password):
    code, body = http(
        "POST",
        f"{pb_url}/api/collections/_superusers/auth-with-password",
        body={"identity": email, "password": password},
    )
    if code == 200 and "token" in body:
        return body["token"]
    print(f"[error] 登录失败 HTTP {code}: {body}")
    sys.exit(1)


def collection_exists(pb_url, token, name):
    code, _ = http(
        "GET",
        f"{pb_url}/api/collections/{name}",
        headers={"Authorization": token},
    )
    return code == 200


def create_collection(pb_url, token, schema):
    name = schema["name"]
    if collection_exists(pb_url, token, name):
        print(f"[skip] {name} 已存在")
        return True
    code, body = http(
        "POST",
        f"{pb_url}/api/collections",
        headers={"Authorization": token},
        body=schema,
    )
    if code in (200, 201):
        print(f"[ok] 创建 {name}")
        return True
    print(f"[fail] {name} HTTP {code}:")
    print(json.dumps(body, ensure_ascii=False, indent=2))
    return False


# ─── PB 0.23+ 字段定义 ────────────────────────────────────────────────────────
def text(name, required=False, max_=0):
    return {"name": name, "type": "text", "required": required, "max": max_}


def number(name, required=False, no_decimal=False):
    return {"name": name, "type": "number", "required": required, "onlyInt": no_decimal}


def boolean(name, required=False):
    return {"name": name, "type": "bool", "required": required}


def auto_id():
    return {"name": "id", "type": "text", "system": True, "primaryKey": True,
            "min": 15, "max": 15, "pattern": "^[a-z0-9]+$",
            "autogeneratePattern": "[a-z0-9]{15}"}


def auto_created():
    return {"name": "created", "type": "autodate", "system": True, "onCreate": True}


def auto_updated():
    return {"name": "updated", "type": "autodate", "system": True, "onCreate": True, "onUpdate": True}


def base_collection(name, fields, list_rule=None, view_rule=None,
                    create_rule=None, update_rule=None, delete_rule=None):
    return {
        "name": name,
        "type": "base",
        "fields": [auto_id()] + fields + [auto_created(), auto_updated()],
        "listRule": list_rule,
        "viewRule": view_rule,
        "createRule": create_rule,
        "updateRule": update_rule,
        "deleteRule": delete_rule,
    }


SCHEMAS = [
    base_collection(
        "quotes",
        [
            text("authorId", required=True),
            text("pairId"),
            text("text", required=True, max_=5000),
            text("source"),
            text("visibility", required=True),
            number("createdAt", required=True, no_decimal=True),
        ],
        list_rule='@request.auth.id != "" && (authorId = @request.auth.id || (visibility = "PARTNER" && pairId != ""))',
        view_rule='@request.auth.id != "" && (authorId = @request.auth.id || (visibility = "PARTNER" && pairId != ""))',
        create_rule='@request.auth.id != "" && authorId = @request.auth.id',
        update_rule='@request.auth.id = authorId',
        delete_rule='@request.auth.id = authorId',
    ),
    base_collection(
        "debts",
        [
            text("pairId", required=True),
            text("fromUserId", required=True),
            text("toUserId", required=True),
            number("unitCents", required=True, no_decimal=True),
            number("count", required=True, no_decimal=True),
            text("reason"),
            number("createdAt", required=True, no_decimal=True),
            boolean("settled"),
            number("settledAt", no_decimal=True),
            text("settledBy"),
        ],
        list_rule='@request.auth.id != "" && (fromUserId = @request.auth.id || toUserId = @request.auth.id)',
        view_rule='@request.auth.id != "" && (fromUserId = @request.auth.id || toUserId = @request.auth.id)',
        create_rule='@request.auth.id != "" && (fromUserId = @request.auth.id || toUserId = @request.auth.id)',
        update_rule='@request.auth.id = toUserId',
        delete_rule=None,
    ),
    base_collection(
        "sync_invites",
        [
            text("fromUserId", required=True),
            text("toUserId", required=True),
            number("plannedDurationMs", required=True, no_decimal=True),
            text("mode", required=True),
            text("goal"),
            text("status", required=True),
            number("createdAt", required=True, no_decimal=True),
            number("expiresAt", required=True, no_decimal=True),
            text("sessionId"),
        ],
        list_rule='@request.auth.id != "" && (fromUserId = @request.auth.id || toUserId = @request.auth.id)',
        view_rule='@request.auth.id != "" && (fromUserId = @request.auth.id || toUserId = @request.auth.id)',
        create_rule='@request.auth.id = fromUserId',
        update_rule='@request.auth.id = toUserId || @request.auth.id = fromUserId',
        delete_rule='@request.auth.id = fromUserId',
    ),
]


def main():
    cfg_path = Path(__file__).parent / "config.json"
    cfg = json.loads(cfg_path.read_text(encoding="utf-8"))
    pb_url = cfg["pb_url"].rstrip("/")
    print(f"[info] 目标 PB: {pb_url}")
    token = admin_auth(pb_url, cfg["admin_email"], cfg["admin_password"])
    print("[ok] 登录成功\n")
    success = 0
    for sch in SCHEMAS:
        if create_collection(pb_url, token, sch):
            success += 1
    print(f"\n[done] {success}/{len(SCHEMAS)} 张表已就绪")


if __name__ == "__main__":
    main()
