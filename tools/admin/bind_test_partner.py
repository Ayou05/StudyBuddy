"""
测试搭档绑定 / 解绑工具

使用 superuser 权限直接操作 PocketBase relationships 表，绕过双方投票逻辑。
让你（一个号）能立刻测试 Pet / Sync Focus / 账本等需要 partner 的功能。

用法：
  python bind_test_partner.py bind your@email.com         # 创建 mock 搭档并绑定
  python bind_test_partner.py unbind your@email.com       # 解绑（删 relationship + 清 partnerId）
  python bind_test_partner.py status your@email.com       # 查看当前绑定状态

mock 搭档昵称固定为"测试小伙伴"，邮箱为 testbuddy_<8位hash>@studybuddy.local，
密码"testpartner123"，不会重复创建：解绑时会保留这个测试号。
"""
import json
import sys
import urllib.request
import urllib.error
import hashlib
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


def admin_auth(pb, email, pw):
    code, body = http(
        "POST",
        f"{pb}/api/collections/_superusers/auth-with-password",
        body={"identity": email, "password": pw},
    )
    if code == 200 and "token" in body:
        return body["token"]
    print(f"[error] superuser 登录失败 HTTP {code}: {body}")
    sys.exit(1)


def find_user_by_email(pb, token, email):
    filt = f'email="{email}"'
    code, body = http(
        "GET",
        f"{pb}/api/collections/users/records?filter={urllib.parse.quote(filt)}&perPage=1",
        headers={"Authorization": token},
    )
    if code == 200 and body.get("items"):
        return body["items"][0]
    return None


def find_or_create_test_partner(pb, token, my_email):
    h = hashlib.md5(my_email.encode()).hexdigest()[:8]
    test_email = f"testbuddy_{h}@studybuddy.local"
    existing = find_user_by_email(pb, token, test_email)
    if existing:
        print(f"[info] 已有测试搭档 {test_email} (id={existing['id']})")
        return existing
    print(f"[info] 创建测试搭档 {test_email}")
    code, body = http(
        "POST",
        f"{pb}/api/collections/users/records",
        headers={"Authorization": token},
        body={
            "email": test_email,
            "password": "testpartner123",
            "passwordConfirm": "testpartner123",
            "nickname": "测试小伙伴",
            "emailVisibility": False,
        },
    )
    if code in (200, 201):
        print(f"[ok] 创建成功 id={body['id']}")
        return body
    print(f"[error] 创建失败 HTTP {code}: {body}")
    sys.exit(1)


def find_relationship(pb, token, uid_a, uid_b):
    f = (f'(userAId="{uid_a}" && userBId="{uid_b}") || '
         f'(userAId="{uid_b}" && userBId="{uid_a}")')
    code, body = http(
        "GET",
        f"{pb}/api/collections/relationships/records?filter={urllib.parse.quote(f)}&perPage=1",
        headers={"Authorization": token},
    )
    if code == 200 and body.get("items"):
        return body["items"][0]
    return None


def cmd_bind(pb, token, my_email):
    me = find_user_by_email(pb, token, my_email)
    if not me:
        print(f"[error] 没找到用户 {my_email}，先在 app 里注册")
        return
    print(f"[ok] 你的 id={me['id']} nickname={me.get('nickname','')}")

    partner = find_or_create_test_partner(pb, token, my_email)

    # 检查现有 relationship
    rel = find_relationship(pb, token, me["id"], partner["id"])
    if rel:
        print(f"[skip] 关系已存在 id={rel['id']}")
    else:
        print(f"[info] 创建 relationship")
        now_ms = int(__import__("time").time() * 1000)
        code, body = http(
            "POST",
            f"{pb}/api/collections/relationships/records",
            headers={"Authorization": token},
            body={
                "userAId": me["id"],
                "userBId": partner["id"],
                "status": "active",
                "boundAt": now_ms,
                "streakDays": 0,
                "totalFocusSessions": 0,
                "intimacyScore": 0,
                "activeBreed": "ORANGE_CAT",
            },
        )
        if code in (200, 201):
            print(f"[ok] relationship 创建成功 id={body['id']}")
        else:
            print(f"[error] 创建失败 HTTP {code}: {body}")
            return

    # 双方 user 记录写 partnerId
    for u, pid in [(me, partner["id"]), (partner, me["id"])]:
        code, body = http(
            "PATCH",
            f"{pb}/api/collections/users/records/{u['id']}",
            headers={"Authorization": token},
            body={"partnerId": pid, "partnerSince": int(__import__("time").time() * 1000)},
        )
        if code == 200:
            print(f"[ok] {u.get('nickname','?')} partnerId 已设置")
        else:
            print(f"[warn] 设置 partnerId 失败 ({u['id']}): {body}")

    print("\n[done] 绑定完成。重启 app 后 Pet / Map / Ledger / SyncFocus 都能进了。")


def cmd_unbind(pb, token, my_email):
    me = find_user_by_email(pb, token, my_email)
    if not me:
        print(f"[error] 没找到 {my_email}")
        return

    h = hashlib.md5(my_email.encode()).hexdigest()[:8]
    partner = find_user_by_email(pb, token, f"testbuddy_{h}@studybuddy.local")
    if not partner:
        print(f"[info] 没有测试搭档，可能本来就没绑定")
    else:
        rel = find_relationship(pb, token, me["id"], partner["id"])
        if rel:
            code, _ = http(
                "DELETE",
                f"{pb}/api/collections/relationships/records/{rel['id']}",
                headers={"Authorization": token},
            )
            print(f"[{'ok' if code in (200,204) else 'warn'}] 删 relationship HTTP {code}")
        else:
            print("[info] 没找到 relationship")

    # 清 partnerId
    for u in [me] + ([partner] if partner else []):
        code, _ = http(
            "PATCH",
            f"{pb}/api/collections/users/records/{u['id']}",
            headers={"Authorization": token},
            body={"partnerId": None, "partnerSince": None},
        )
        print(f"[{'ok' if code == 200 else 'warn'}] 清 {u.get('nickname','?')} partnerId HTTP {code}")

    print("\n[done] 解绑完成。可以正式和你对象绑定了。")


def cmd_status(pb, token, my_email):
    me = find_user_by_email(pb, token, my_email)
    if not me:
        print(f"[error] 没找到 {my_email}")
        return
    print(f"id: {me['id']}")
    print(f"nickname: {me.get('nickname', '')}")
    print(f"partnerId: {me.get('partnerId', '(无)')}")
    print(f"partnerSince: {me.get('partnerSince', '(无)')}")


def main():
    import urllib.parse  # noqa
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)
    cmd, my_email = sys.argv[1], sys.argv[2]
    cfg = json.loads((Path(__file__).parent / "config.json").read_text(encoding="utf-8"))
    pb = cfg["pb_url"].rstrip("/")
    token = admin_auth(pb, cfg["admin_email"], cfg["admin_password"])
    print(f"[ok] 登录 {pb}\n")

    if cmd == "bind":
        cmd_bind(pb, token, my_email)
    elif cmd == "unbind":
        cmd_unbind(pb, token, my_email)
    elif cmd == "status":
        cmd_status(pb, token, my_email)
    else:
        print(f"[error] 未知命令 {cmd}，支持 bind / unbind / status")


if __name__ == "__main__":
    import urllib.parse
    main()
