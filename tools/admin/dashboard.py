"""
StudyBuddy 本地可视化运维 dashboard

启动：
  python dashboard.py

打开 http://localhost:5005

功能：
  - 看用户、关系、专注会话、话廊、账本统计
  - 一键给用户绑定/解绑测试搭档
  - 重建 collections schema
"""
import json
import urllib.request
import urllib.error
import urllib.parse
import hashlib
import time
from pathlib import Path
from flask import Flask, jsonify, request, render_template_string


app = Flask(__name__)
CFG = json.loads((Path(__file__).parent / "config.json").read_text(encoding="utf-8"))
PB = CFG["pb_url"].rstrip("/")


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


_TOKEN = None
def token():
    global _TOKEN
    if _TOKEN:
        return _TOKEN
    code, body = http(
        "POST",
        f"{PB}/api/collections/_superusers/auth-with-password",
        body={"identity": CFG["admin_email"], "password": CFG["admin_password"]},
    )
    if code == 200:
        _TOKEN = body["token"]
        return _TOKEN
    raise RuntimeError(f"login failed: {body}")


def auth():
    return {"Authorization": token()}


def list_records(coll, filt=None, sort=None, page=1, per_page=200):
    q = {"page": page, "perPage": per_page}
    if filt: q["filter"] = filt
    if sort: q["sort"] = sort
    qs = urllib.parse.urlencode(q)
    code, body = http("GET", f"{PB}/api/collections/{coll}/records?{qs}", headers=auth())
    return body.get("items", []) if code == 200 else []


# ─── HTML ───────────────────────────────────────────────────────────────────
PAGE = """<!doctype html>
<html lang="zh-CN"><head>
<meta charset="utf-8"/>
<title>StudyBuddy 后台</title>
<style>
  * { box-sizing: border-box; }
  body { font-family: -apple-system, "Segoe UI", "PingFang SC", sans-serif; background: #faf9f5; color: #181715; margin: 0; padding: 24px; max-width: 1200px; margin-left: auto; margin-right: auto; }
  h1 { font-weight: 400; letter-spacing: -0.5px; margin: 0 0 4px 0; }
  .sub { color: #6c6a64; font-size: 14px; margin-bottom: 24px; }
  .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; margin-bottom: 24px; }
  .stat { background: #efe9de; padding: 16px; border-radius: 12px; }
  .stat-num { font-size: 32px; font-weight: 500; color: #cc785c; }
  .stat-label { font-size: 12px; color: #6c6a64; text-transform: uppercase; letter-spacing: 1px; margin-top: 4px; }
  section { background: #fff8ee; border: 1px solid #ebe6df; border-radius: 12px; padding: 16px; margin-bottom: 16px; }
  section h2 { font-size: 16px; font-weight: 500; margin: 0 0 12px 0; }
  table { width: 100%; border-collapse: collapse; font-size: 13px; }
  th, td { padding: 8px 12px; text-align: left; border-bottom: 1px solid #f0ebe2; }
  th { color: #6c6a64; font-weight: 400; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; }
  tr:hover { background: #faf6e8; }
  button { background: #cc785c; color: white; border: none; padding: 6px 12px; border-radius: 6px; font-size: 13px; cursor: pointer; }
  button:hover { background: #a9583e; }
  button.ghost { background: transparent; color: #cc785c; border: 1px solid #cc785c; }
  button.ghost:hover { background: #cc785c11; }
  button.danger { background: #c64545; }
  input { padding: 6px 10px; border: 1px solid #ebe6df; border-radius: 6px; font-size: 13px; background: white; }
  .toolbar { display: flex; gap: 8px; align-items: center; margin-bottom: 12px; }
  .badge { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 11px; }
  .badge-active { background: #5db87233; color: #2d6a3f; }
  .badge-none { background: #6c6a6422; color: #6c6a64; }
  .row-actions { text-align: right; }
  details { margin-top: 8px; }
  details summary { cursor: pointer; color: #6c6a64; font-size: 12px; }
  pre { background: #181715; color: #faf9f5; padding: 12px; border-radius: 6px; font-size: 12px; overflow-x: auto; }
</style>
</head>
<body>
<h1>StudyBuddy 后台</h1>
<div class="sub">{{ pb_url }}</div>

<div class="grid">
  <div class="stat"><div class="stat-num" id="cnt-users">—</div><div class="stat-label">用户</div></div>
  <div class="stat"><div class="stat-num" id="cnt-relationships">—</div><div class="stat-label">绑定关系</div></div>
  <div class="stat"><div class="stat-num" id="cnt-sessions">—</div><div class="stat-label">专注次数</div></div>
  <div class="stat"><div class="stat-num" id="cnt-quotes">—</div><div class="stat-label">话廊条目</div></div>
  <div class="stat"><div class="stat-num" id="cnt-debts">—</div><div class="stat-label">欠条记录</div></div>
  <div class="stat"><div class="stat-num" id="cnt-pets">—</div><div class="stat-label">宠物</div></div>
</div>

<section>
  <h2>用户 + 一键绑定测试搭档</h2>
  <div class="toolbar">
    <input id="search" placeholder="按邮箱搜索…" style="flex:1" />
    <button onclick="loadUsers()">刷新</button>
  </div>
  <table id="user-table">
    <thead><tr><th>邮箱</th><th>昵称</th><th>搭档</th><th class="row-actions">操作</th></tr></thead>
    <tbody></tbody>
  </table>
</section>

<section>
  <h2>最新话廊（{{ '{}' }}）</h2>
  <table id="quote-table">
    <thead><tr><th>时间</th><th>作者</th><th>内容</th><th>可见性</th></tr></thead>
    <tbody></tbody>
  </table>
</section>

<section>
  <h2>最新会话</h2>
  <table id="session-table">
    <thead><tr><th>时间</th><th>用户</th><th>类型</th><th>计划</th><th>状态</th></tr></thead>
    <tbody></tbody>
  </table>
</section>

<section>
  <h2>欠条流水</h2>
  <table id="debt-table">
    <thead><tr><th>时间</th><th>从</th><th>到</th><th>金额</th><th>原因</th><th>状态</th></tr></thead>
    <tbody></tbody>
  </table>
</section>

<section>
  <h2>系统</h2>
  <button class="ghost" onclick="recreateSchemas()">检查 / 创建 schema</button>
</section>

<script>
async function api(path, method='GET', body=null) {
  const r = await fetch(path, {method, headers: {'Content-Type':'application/json'}, body: body ? JSON.stringify(body) : null});
  return r.json();
}

function badge(text, cls='badge-none') { return `<span class="badge ${cls}">${text}</span>`; }
function fmt(ms) { if(!ms) return '—'; const d = new Date(ms); return `${d.getMonth()+1}/${d.getDate()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`; }
function trim(s, n=80) { return (s||'').length > n ? (s.slice(0,n)+'…') : (s||''); }

let _users = [];

async function loadStats() {
  const s = await api('/api/stats');
  for (const k of ['users','relationships','sessions','quotes','debts','pets']) {
    document.getElementById('cnt-'+k).textContent = s[k] ?? 0;
  }
}

async function loadUsers() {
  _users = await api('/api/users');
  const q = document.getElementById('search').value.toLowerCase();
  const tbody = document.querySelector('#user-table tbody');
  tbody.innerHTML = '';
  for (const u of _users) {
    if (q && !(u.email||'').toLowerCase().includes(q)) continue;
    const row = document.createElement('tr');
    const partner = u.partnerNickname ? `${u.partnerNickname} (${u.partnerEmail||'—'})` : badge('未绑定');
    row.innerHTML = `<td>${u.email}</td><td>${u.nickname||'—'}</td><td>${partner}</td><td class="row-actions">
      ${u.partnerId
        ? `<button class="danger" onclick="unbind('${u.email}')">解绑</button>`
        : `<button onclick="bind('${u.email}')">绑定测试搭档</button>`}
      ${u.partnerId ? `<button class="ghost" onclick="resetPet('${u.email}')">重置宠物</button>` : ''}
      <button class="danger" onclick="deleteUser('${u.email}','${u.id}')">删除</button>
    </td>`;
    tbody.appendChild(row);
  }
}

async function bind(email) {
  if (!confirm(`给 ${email} 绑定测试搭档？`)) return;
  const r = await api('/api/bind-test', 'POST', {email});
  alert(r.ok ? '绑定成功' : '失败：' + r.error);
  loadUsers(); loadStats();
}
async function unbind(email) {
  if (!confirm(`解绑 ${email}？这会保留测试搭档号但清除绑定关系。`)) return;
  const r = await api('/api/unbind', 'POST', {email});
  alert(r.ok ? '已解绑' : '失败：' + r.error);
  loadUsers(); loadStats();
}
async function deleteUser(email, id) {
  if (!confirm(`确定删除用户 ${email}？\n会同时删除其 relationship/sessions/quotes/debts/pet 等关联数据，不可恢复。`)) return;
  const r = await api('/api/delete-user', 'POST', {id});
  if (r.ok) {
    alert(`已删除 ${email}\n清理：${r.cleaned.join('、')}`);
  } else {
    alert('失败：' + r.error);
  }
  loadUsers(); loadStats();
}
async function resetPet(email) {
  if (!confirm(`重置 ${email} 的宠物？\n会删掉旧 pet 记录，重新创建一个 EGG 阶段的橘猫。`)) return;
  const r = await api('/api/reset-pet', 'POST', {email});
  alert(r.ok ? `已重置：${r.msg}` : '失败：' + r.error);
  loadStats();
}

async function loadQuotes() {
  const list = await api('/api/quotes');
  const tbody = document.querySelector('#quote-table tbody');
  tbody.innerHTML = list.slice(0,30).map(q => `<tr>
    <td>${fmt(q.createdAt)}</td><td>${q.authorEmail||q.authorId.slice(0,8)}</td>
    <td>${trim(q.text)}</td><td>${q.visibility==='PARTNER'?badge('对TA','badge-active'):badge('私密')}</td>
  </tr>`).join('');
}

async function loadSessions() {
  const list = await api('/api/sessions');
  const tbody = document.querySelector('#session-table tbody');
  tbody.innerHTML = list.slice(0,30).map(s => `<tr>
    <td>${fmt(s.startedAt)}</td><td>${s.userEmail||s.userId.slice(0,8)}</td>
    <td>${s.type}</td><td>${Math.round((s.plannedDurationMs||0)/60000)}min</td><td>${s.status}</td>
  </tr>`).join('');
}

async function loadDebts() {
  const list = await api('/api/debts');
  const tbody = document.querySelector('#debt-table tbody');
  tbody.innerHTML = list.slice(0,30).map(d => `<tr>
    <td>${fmt(d.createdAt)}</td><td>${d.fromEmail||d.fromUserId.slice(0,8)}</td>
    <td>${d.toEmail||d.toUserId.slice(0,8)}</td>
    <td>¥${(d.unitCents*d.count/100).toFixed(0)}</td><td>${trim(d.reason||'',40)}</td>
    <td>${d.settled?badge('已结算','badge-active'):badge('未结')}</td>
  </tr>`).join('');
}

async function recreateSchemas() {
  if (!confirm('检查 quotes/debts/sync_invites 是否存在，缺失则创建。已存在的不会被改。')) return;
  const r = await api('/api/recreate-schemas', 'POST');
  alert(JSON.stringify(r, null, 2));
}

document.getElementById('search').addEventListener('input', loadUsers);

loadStats(); loadUsers(); loadQuotes(); loadSessions(); loadDebts();
setInterval(() => { loadStats(); }, 10000);
</script>
</body></html>"""


@app.route("/")
def index():
    return render_template_string(PAGE, pb_url=PB)


@app.route("/api/stats")
def stats():
    counts = {}
    for c in ["users", "relationships", "sessions", "quotes", "debts", "pets"]:
        code, body = http(
            "GET",
            f"{PB}/api/collections/{c}/records?perPage=1",
            headers=auth(),
        )
        counts[c] = body.get("totalItems", 0) if code == 200 else 0
    return jsonify(counts)


@app.route("/api/users")
def users():
    items = list_records("users", sort="-created", per_page=200)
    # 反查 partner email/nickname
    by_id = {u["id"]: u for u in items}
    for u in items:
        pid = u.get("partnerId")
        if pid and pid in by_id:
            u["partnerEmail"] = by_id[pid].get("email")
            u["partnerNickname"] = by_id[pid].get("nickname")
    return jsonify(items)


@app.route("/api/quotes")
def quotes():
    items = list_records("quotes", sort="-createdAt", per_page=50)
    users = {u["id"]: u for u in list_records("users", per_page=200)}
    for q in items:
        u = users.get(q.get("authorId"))
        if u: q["authorEmail"] = u.get("email")
    return jsonify(items)


@app.route("/api/sessions")
def sessions():
    items = list_records("sessions", sort="-startedAt", per_page=50)
    users = {u["id"]: u for u in list_records("users", per_page=200)}
    for s in items:
        u = users.get(s.get("userId"))
        if u: s["userEmail"] = u.get("email")
    return jsonify(items)


@app.route("/api/debts")
def debts():
    items = list_records("debts", sort="-createdAt", per_page=50)
    users = {u["id"]: u for u in list_records("users", per_page=200)}
    for d in items:
        d["fromEmail"] = (users.get(d.get("fromUserId")) or {}).get("email")
        d["toEmail"] = (users.get(d.get("toUserId")) or {}).get("email")
    return jsonify(items)


@app.route("/api/bind-test", methods=["POST"])
def bind_test():
    data = request.get_json() or {}
    email = data.get("email")
    try:
        items = list_records("users", filt=f'email="{email}"', per_page=1)
        if not items:
            return jsonify({"ok": False, "error": f"没找到用户 {email}"})
        me = items[0]

        h = hashlib.md5(email.encode()).hexdigest()[:8]
        test_email = f"testbuddy_{h}@studybuddy.local"
        partners = list_records("users", filt=f'email="{test_email}"', per_page=1)
        if partners:
            partner = partners[0]
        else:
            code, partner = http(
                "POST",
                f"{PB}/api/collections/users/records",
                headers=auth(),
                body={
                    "email": test_email,
                    "password": "testpartner123",
                    "passwordConfirm": "testpartner123",
                    "nickname": "测试小伙伴",
                    "emailVisibility": False,
                },
            )
            if code not in (200, 201):
                return jsonify({"ok": False, "error": f"创建测试搭档失败 {partner}"})

        # 关系
        f = (f'(userAId="{me["id"]}" && userBId="{partner["id"]}") || '
             f'(userAId="{partner["id"]}" && userBId="{me["id"]}")')
        rels = list_records("relationships", filt=f, per_page=1)
        if not rels:
            now = int(time.time() * 1000)
            code, _ = http(
                "POST",
                f"{PB}/api/collections/relationships/records",
                headers=auth(),
                body={
                    "userAId": me["id"], "userBId": partner["id"],
                    "status": "active", "boundAt": now,
                    "streakDays": 0, "totalFocusSessions": 0,
                    "intimacyScore": 0, "activeBreed": "ORANGE_CAT",
                },
            )

        # partnerId
        for u, pid in [(me, partner["id"]), (partner, me["id"])]:
            http(
                "PATCH",
                f"{PB}/api/collections/users/records/{u['id']}",
                headers=auth(),
                body={"partnerId": pid, "partnerSince": int(time.time() * 1000)},
            )
        return jsonify({"ok": True})
    except Exception as e:
        return jsonify({"ok": False, "error": str(e)})


@app.route("/api/unbind", methods=["POST"])
def unbind():
    data = request.get_json() or {}
    email = data.get("email")
    try:
        items = list_records("users", filt=f'email="{email}"', per_page=1)
        if not items:
            return jsonify({"ok": False, "error": "用户不存在"})
        me = items[0]
        pid = me.get("partnerId")
        if pid:
            f = (f'(userAId="{me["id"]}" && userBId="{pid}") || '
                 f'(userAId="{pid}" && userBId="{me["id"]}")')
            for r in list_records("relationships", filt=f, per_page=10):
                http(
                    "DELETE",
                    f"{PB}/api/collections/relationships/records/{r['id']}",
                    headers=auth(),
                )
            for uid in [me["id"], pid]:
                http(
                    "PATCH",
                    f"{PB}/api/collections/users/records/{uid}",
                    headers=auth(),
                    body={"partnerId": None, "partnerSince": None},
                )
        return jsonify({"ok": True})
    except Exception as e:
        return jsonify({"ok": False, "error": str(e)})


@app.route("/api/delete-user", methods=["POST"])
def delete_user():
    data = request.get_json() or {}
    uid = data.get("id")
    if not uid:
        return jsonify({"ok": False, "error": "missing id"})
    cleaned = []
    try:
        # 1. relationships
        f = f'userAId="{uid}" || userBId="{uid}"'
        for r in list_records("relationships", filt=f, per_page=50):
            http("DELETE", f"{PB}/api/collections/relationships/records/{r['id']}", headers=auth())
            cleaned.append(f"relationship/{r['id']}")
            # partnerId 清掉对端
            other = r["userBId"] if r["userAId"] == uid else r["userAId"]
            http("PATCH", f"{PB}/api/collections/users/records/{other}",
                 headers=auth(), body={"partnerId": None, "partnerSince": None})
        # 2. status
        for c in ["status", "sessions", "messages", "checkins", "landmarks"]:
            for r in list_records(c, filt=f'userId="{uid}"', per_page=200):
                http("DELETE", f"{PB}/api/collections/{c}/records/{r['id']}", headers=auth())
            cleaned.append(c)
        # 3. quotes
        for r in list_records("quotes", filt=f'authorId="{uid}"', per_page=200):
            http("DELETE", f"{PB}/api/collections/quotes/records/{r['id']}", headers=auth())
        cleaned.append("quotes")
        # 4. debts / sync_invites
        for c in ["debts", "sync_invites"]:
            f2 = f'fromUserId="{uid}" || toUserId="{uid}"'
            for r in list_records(c, filt=f2, per_page=200):
                http("DELETE", f"{PB}/api/collections/{c}/records/{r['id']}", headers=auth())
            cleaned.append(c)
        # 5. pets / funds 这两个挂在 pairId 上，relationship 删了关联也没了，跳过
        # 6. 删用户本体
        http("DELETE", f"{PB}/api/collections/users/records/{uid}", headers=auth())
        cleaned.append("user")
        return jsonify({"ok": True, "cleaned": cleaned})
    except Exception as e:
        return jsonify({"ok": False, "error": str(e), "cleaned": cleaned})


@app.route("/api/reset-pet", methods=["POST"])
def reset_pet():
    """删指定用户 pair 下的 pet 记录。下次进 PetScreen 会重新孵化一颗 EGG。"""
    data = request.get_json() or {}
    email = data.get("email")
    if not email:
        return jsonify({"ok": False, "error": "missing email"})
    try:
        items = list_records("users", filt=f'email="{email}"', per_page=1)
        if not items:
            return jsonify({"ok": False, "error": f"没找到用户 {email}"})
        me = items[0]
        pid = me.get("partnerId")
        if not pid:
            return jsonify({"ok": False, "error": "用户未绑定搭档，没有 pet"})
        # 找 relationship id 作为 pairId
        f = (f'(userAId="{me["id"]}" && userBId="{pid}") || '
             f'(userAId="{pid}" && userBId="{me["id"]}")')
        rels = list_records("relationships", filt=f, per_page=1)
        if not rels:
            return jsonify({"ok": False, "error": "找不到 relationship"})
        pair_id = rels[0]["id"]
        # 删该 pair 下所有 pet 记录
        pets = list_records("pets", filt=f'pairId="{pair_id}"', per_page=10)
        for p in pets:
            http("DELETE", f"{PB}/api/collections/pets/records/{p['id']}", headers=auth())
        return jsonify({"ok": True, "msg": f"删了 {len(pets)} 只宠物，下次进 PetScreen 会得到新蛋"})
    except Exception as e:
        return jsonify({"ok": False, "error": str(e)})


@app.route("/api/recreate-schemas", methods=["POST"])
def recreate_schemas():
    # 调本目录下 create_collections.py 的 SCHEMAS 逻辑（避免重复定义）
    import importlib.util
    spec = importlib.util.spec_from_file_location("cc", Path(__file__).parent / "create_collections.py")
    cc = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(cc)
    log = []
    for sch in cc.SCHEMAS:
        if cc.collection_exists(PB, token(), sch["name"]):
            log.append(f"[skip] {sch['name']} 已存在")
        else:
            ok = cc.create_collection(PB, token(), sch)
            log.append(f"[{'ok' if ok else 'fail'}] {sch['name']}")
    return jsonify({"log": log})


if __name__ == "__main__":
    print(f"\n  StudyBuddy 后台启动: http://localhost:5005\n  PocketBase: {PB}\n")
    app.run(port=5005, debug=False)
