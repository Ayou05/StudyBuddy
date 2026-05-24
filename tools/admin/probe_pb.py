"""
PB 现状只读探测器 —— 不改任何东西，只看版本 / collection 列表 / 字段结构

用法：
  1. 复制 config.example.json 为 config.json，填地址 + admin
  2. python probe_pb.py
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


def main():
    cfg_path = Path(__file__).parent / "config.json"
    if not cfg_path.exists():
        print(f"[error] {cfg_path} 不存在")
        sys.exit(1)
    cfg = json.loads(cfg_path.read_text(encoding="utf-8"))
    pb = cfg["pb_url"].rstrip("/")
    print(f"\n=== 探测 {pb} ===\n")

    # 1. 健康检查 + 版本
    print("[1] 健康检查")
    code, body = http("GET", f"{pb}/api/health")
    print(f"    HTTP {code}: {body}")

    # 2. 尝试 superuser auth
    print("\n[2] 尝试 superuser 登录（PB 0.23+）")
    code, body = http(
        "POST",
        f"{pb}/api/collections/_superusers/auth-with-password",
        body={"identity": cfg["admin_email"], "password": cfg["admin_password"]},
    )
    print(f"    HTTP {code}: {('token=' + body['token'][:30] + '...') if isinstance(body, dict) and 'token' in body else body}")
    token_new = body.get("token") if code == 200 and isinstance(body, dict) else None

    # 3. 尝试 admin auth（旧版）
    print("\n[3] 尝试 admin 登录（PB ≤0.22）")
    code, body = http(
        "POST",
        f"{pb}/api/admins/auth-with-password",
        body={"identity": cfg["admin_email"], "password": cfg["admin_password"]},
    )
    print(f"    HTTP {code}: {('token=' + body['token'][:30] + '...') if isinstance(body, dict) and 'token' in body else body}")
    token_old = body.get("token") if code == 200 and isinstance(body, dict) else None

    token = token_new or token_old
    if not token:
        print("\n[error] 两种端点都没登录上。检查邮箱密码 / 服务器是否在跑")
        sys.exit(1)

    print(f"\n[ok] 用 {'_superusers (新版)' if token_new else 'admins (旧版)'} 登录成功\n")

    # 4. 列出所有 collections
    print("[4] 现有 collections")
    code, body = http(
        "GET",
        f"{pb}/api/collections?perPage=100",
        headers={"Authorization": token},
    )
    if code == 200 and "items" in body:
        for c in body["items"]:
            sys_flag = "  (system)" if c.get("system") else ""
            n_fields = len(c.get("schema", c.get("fields", [])))
            print(f"    - {c['name']:<24} type={c.get('type','?'):<8} fields={n_fields}{sys_flag}")
    else:
        print(f"    HTTP {code}: {body}")

    # 5. 检查我们关心的 4 张表是否存在
    print("\n[5] 我们要的 4 张表状态")
    for n in ["quotes", "debts", "sync_invites", "landmarks"]:
        code, body = http(
            "GET",
            f"{pb}/api/collections/{n}",
            headers={"Authorization": token},
        )
        if code == 200:
            schema = body.get("schema", body.get("fields", []))
            print(f"    ✓ {n} 已存在（{len(schema)} 字段）")
        elif code == 404:
            print(f"    × {n} 不存在")
        else:
            print(f"    ? {n} HTTP {code}: {body}")


if __name__ == "__main__":
    main()
