#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""仅对专用本机空库执行真实HTTP业务验收，私有快照不提交。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""

import concurrent.futures
import argparse, base64, hashlib, http.cookiejar, json, os, secrets, urllib.request, urllib.error, urllib.parse, uuid
from decimal import Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PNG = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jO1QAAAAASUVORK5CYII="
)
checks = []


class Client:
    """独立会话与真实CSRF头，不打印凭据。知华科技 https://www.zhuatech.cn/。"""

    def __init__(self, base):
        self.base = base
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
        )
        self.csrf = None

    def request(
        self, path, body=None, status=200, method=None, binary=False, headers=None
    ):
        method = method or ("POST" if body is not None else "GET")
        head = dict(headers or {})
        if method not in ["GET", "HEAD"]:
            if self.csrf is None:
                self.csrf = self.request("/auth/csrf")
            head[self.csrf["header"]] = self.csrf["token"]
        data = body
        if isinstance(body, dict):
            head["Content-Type"] = "application/json"
            data = json.dumps(body).encode()
        try:
            with self.opener.open(
                urllib.request.Request(
                    self.base + "/api" + path, data=data, method=method, headers=head
                ),
                timeout=40,
            ) as response:
                code = response.status
                raw = response.read()
        except urllib.error.HTTPError as e:
            code = e.code
            raw = e.read()
        assert code == status, f"{method} {path}: expected {status}, got {code}"
        checks.append({"method": method, "path": path, "status": code})
        return raw if binary else json.loads(raw)

    def login(self, name, password):
        p = self.request("/auth/login", {"username": name, "password": password})
        self.csrf = None
        return p

    def upload(self, order, kind, name, data, status=200):
        version = self.request("/orders/" + str(order))["version"]
        boundary = "PrintDesk" + uuid.uuid4().hex
        raw = b""
        for k, v in {
            "kind": kind,
            "version": version,
            "requestId": str(uuid.uuid4()),
        }.items():
            raw += f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode()
        raw += (
            f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{name}"\r\nContent-Type: application/octet-stream\r\n\r\n'.encode()
            + data
            + f"\r\n--{boundary}--\r\n".encode()
        )
        return self.request(
            f"/orders/{order}/files",
            raw,
            status=status,
            headers={"Content-Type": "multipart/form-data; boundary=" + boundary},
        )


def env(path):
    return dict(
        l.split("=", 1)
        for l in Path(path).read_text().splitlines()
        if "=" in l and not l.startswith("#")
    )


def private(path, data):
    path.parent.mkdir(exist_ok=True, parents=True)
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def snapshot(admin, order_ids, file_ids):
    """对完整订单、目录、报告和文件哈希做持久化比对。知华科技 https://www.zhuatech.cn/。"""
    return {
        "orders": [admin.request("/orders/" + str(i)) for i in order_ids],
        "masters": {
            k: admin.request("/masters/" + k)
            for k in ["customers", "materials", "suppliers"]
        },
        "catalogs": {
            k: admin.request("/admin/" + k)
            for k in [
                "users",
                "roles",
                "permissions",
                "departments",
                "menus",
                "dictionaries",
                "settings",
            ]
        },
        "reports": admin.request("/reports"),
        "files": {
            str(i): hashlib.sha256(
                admin.request("/files/" + str(i), binary=True)
            ).hexdigest()
            for i in file_ids
        },
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--base", default="http://127.0.0.1:8143")
    a = p.parse_args()
    u = urllib.parse.urlparse(a.base)
    if u.hostname != "127.0.0.1" or u.scheme != "http":
        p.error("Only isolated localhost HTTP QA is allowed")
    cfg = env(ROOT / ".env")
    admin = Client(a.base)
    admin.login(cfg.get("ADMIN_USERNAME", "admin"), cfg["ADMIN_PASSWORD"])
    assert admin.request("/orders")["total"] == 0, "Refuse QA against existing orders"
    assert (
        len(admin.request("/masters/customers")) == 0
    ), "Refuse QA against existing business data"
    qa_pw = "Aa9" + secrets.token_hex(20)

    def account(name, role, dept=1):
        return admin.request(
            "/admin/users",
            {
                "username": name,
                "displayName": "TEST " + name,
                "password": qa_pw,
                "roleId": role,
                "departmentId": dept,
                "enabled": True,
            },
        )

    customer_account = account("test_customer", 5)
    operator_account = account("test_operator", 3)
    account("test_quality", 4)
    account("test_other_customer", 5)
    clients = {
        n: Client(a.base)
        for n in [
            "test_customer",
            "test_operator",
            "test_quality",
            "test_other_customer",
        ]
    }
    for n, c in clients.items():
        c.login(n, qa_pw)
    client = clients["test_customer"]
    worker = clients["test_operator"]
    reviewer = clients["test_quality"]
    other = clients["test_other_customer"]
    customer = admin.request(
        "/masters/customers",
        {
            "departmentId": 1,
            "accountId": customer_account["id"],
            "name": "TEST North Studio",
            "contact": "TEST buyer",
            "email": "buyer@example.invalid",
            "address": "TEST pickup desk",
            "enabled": True,
        },
    )
    material = admin.request(
        "/masters/materials",
        {
            "departmentId": 1,
            "code": "TEST-PAPER",
            "name": "TEST poster stock",
            "unit": "SHEET",
            "currency": "CNY",
            "enabled": True,
        },
    )
    material = admin.request(
        f"/materials/{material['id']}/receive",
        {
            "version": material["version"],
            "quantity": "100",
            "cost": "2",
            "reference": "TEST-RECEIPT-001",
        },
    )
    supplier = admin.request(
        "/masters/suppliers",
        {
            "departmentId": 1,
            "name": "TEST Finishing Partner",
            "contact": "TEST supplier",
            "enabled": True,
        },
    )
    draft = {
        "customerId": customer["id"],
        "title": "TEST exhibition posters",
        "dueDate": "2099-01-01",
        "currency": "CNY",
        "category": "DIGITAL",
        "instructions": "TEST A3 posters, matte finish",
        "discount": "0",
        "taxPercent": "0",
        "requestId": str(uuid.uuid4()),
        "lines": [
            {
                "name": "TEST posters",
                "materialId": material["id"],
                "quantity": 10,
                "usage": "1",
                "widthMm": 297,
                "heightMm": 420,
                "printRate": "1",
                "finishRate": "0",
                "setup": "5",
                "markupPercent": "20",
            }
        ],
    }
    order = admin.request("/orders", draft)
    oid = order["id"]
    line = order["lines"][0]["id"]
    assert Decimal(str(order["total"])) == Decimal("42")
    again = admin.request("/orders", draft)
    assert again["id"] == oid

    def act(who, action, body=None, status=200, order_id=oid):
        current = admin.request("/orders/" + str(order_id))
        b = dict(body or {})
        b.update(version=current["version"], requestId=str(uuid.uuid4()))
        return who.request(f"/orders/{order_id}/{action}", b, status=status)

    client.request("/orders/" + str(oid), status=403)
    worker.request("/orders/" + str(oid), status=403)
    admin.request(
        f"/orders/{oid}/QUOTE",
        {"version": 1, "requestId": str(uuid.uuid4())},
        status=409,
    )
    act(admin, "QUOTE")
    act(admin, "ACCEPT", status=409)
    other.request("/orders/" + str(oid), status=403)
    client_view = client.request("/orders/" + str(oid))
    assert "events" not in client_view and "materialCost" not in client_view["lines"][0]
    act(client, "REJECT", {"note": "TEST clarify finishing"})
    current = admin.request("/orders/" + str(oid))
    draft.update(version=current["version"], requestId=str(uuid.uuid4()))
    current = admin.request("/orders/" + str(oid), draft, method="PUT")
    line = current["lines"][0]["id"]
    act(admin, "QUOTE")
    act(client, "ACCEPT")
    files = []
    d = admin.upload(oid, "PROOF", "TEST-proof-v1.png", PNG)
    first = d["latestProofId"]
    files.append(first)
    act(client, "PROOF_ACCEPT", {"proofId": first})
    act(admin, "ASSIGN", {"assigneeId": operator_account["id"]})
    d = admin.upload(oid, "PROOF", "TEST-proof-v2.png", PNG)
    last = d["latestProofId"]
    files.append(last)
    act(worker, "START", status=409)
    act(client, "PROOF_ACCEPT", {"proofId": first}, status=409)
    act(client, "PROOF_ACCEPT", {"proofId": last})
    assert admin.request("/files/" + str(last), binary=True) == PNG
    other.request("/files/" + str(last), status=403)
    client.request("/masters/customers", status=403)
    worker.request("/orders/export", {}, status=403)
    task = act(
        admin,
        "VENDOR_CREATE",
        {
            "lineId": line,
            "supplierId": supplier["id"],
            "description": "TEST matte finishing",
            "quantity": 10,
            "amount": "5",
            "dueDate": "2099-01-01",
        },
    )["vendorTasks"][0]
    act(admin, "VENDOR_CONFIRM", {"taskId": task["id"], "reference": "TEST-VENDOR-001"})
    act(
        admin,
        "VENDOR_RECEIVE",
        {"taskId": task["id"], "quantity": 11, "reference": "TEST-RECEIPT-OVER"},
        status=400,
    )
    act(
        admin,
        "VENDOR_RECEIVE",
        {"taskId": task["id"], "quantity": 4, "reference": "TEST-VENDOR-RECEIPT-001"},
    )
    act(
        admin,
        "VENDOR_RECEIVE",
        {"taskId": task["id"], "quantity": 6, "reference": "TEST-VENDOR-RECEIPT-002"},
    )
    act(worker, "START")
    act(
        worker,
        "WORK",
        {"lineId": line, "good": 10, "scrap": 0, "minutes": 60, "hourlyRate": "12"},
        status=409,
    )
    act(
        worker,
        "ISSUE",
        {"lineId": line, "quantity": "101", "note": "TEST too much"},
        status=409,
    )
    d = act(worker, "ISSUE", {"lineId": line, "quantity": "12", "note": "TEST issued"})
    issue = d["stock"][0]["id"]
    act(
        worker,
        "RETURN",
        {"lineId": line, "sourceId": issue, "quantity": "13", "note": "TEST too much"},
        status=409,
    )
    act(
        worker,
        "RETURN",
        {"lineId": line, "sourceId": issue, "quantity": "2", "note": "TEST unused"},
    )
    act(
        worker,
        "WORK",
        {"lineId": line, "good": 11, "scrap": 0, "minutes": 60, "hourlyRate": "12"},
        status=400,
    )
    act(
        worker,
        "WORK",
        {"lineId": line, "good": 10, "scrap": 0, "minutes": 60, "hourlyRate": "12"},
    )
    worker_view = worker.request("/orders/" + str(oid))
    assert (
        "total" not in worker_view
        and "cost" not in worker_view
        and "payments" not in worker_view
    )
    act(
        worker,
        "RETURN",
        {
            "lineId": line,
            "sourceId": issue,
            "quantity": "1",
            "note": "TEST already consumed",
        },
        status=409,
    )
    act(reviewer, "QUALITY_FAIL", {"note": "TEST finish rework"})
    act(reviewer, "QUALITY_PASS", status=409)
    act(worker, "ISSUE", {"lineId": line, "quantity": "10", "note": "TEST rework"})
    act(
        worker,
        "WORK",
        {"lineId": line, "good": 10, "scrap": 0, "minutes": 30, "hourlyRate": "12"},
    )
    act(reviewer, "QUALITY_PASS")
    act(
        reviewer,
        "DELIVER",
        {
            "lines": [{"lineId": line, "quantity": 4}],
            "method": "PICKUP",
            "address": "TEST pickup desk",
            "reference": "TEST-DELIVERY-001",
        },
    )
    act(
        reviewer,
        "DELIVER",
        {
            "lines": [{"lineId": line, "quantity": 7}],
            "method": "PICKUP",
            "address": "TEST pickup desk",
            "reference": "TEST-OVER",
        },
        status=400,
    )
    act(
        reviewer,
        "DELIVER",
        {
            "lines": [{"lineId": line, "quantity": 6}],
            "method": "SHIP",
            "address": "TEST destination",
            "reference": "TEST-DELIVERY-002",
        },
    )
    act(admin, "CLOSE", status=409)
    act(
        admin,
        "RECEIVE",
        {"amount": "43", "reference": "TEST-OVER", "note": "TEST"},
        status=409,
    )
    current = admin.request("/orders/" + str(oid))
    payment_body = {
        "version": current["version"],
        "requestId": str(uuid.uuid4()),
        "amount": "20",
        "reference": "TEST-PAYMENT-001",
        "note": "TEST paid outside",
    }
    admin.request("/auth/csrf")
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        results = list(
            pool.map(
                lambda _: admin.request(f"/orders/{oid}/RECEIVE", payment_body),
                range(2),
            )
        )
    d = results[-1]
    assert len(d["payments"]) == 1
    source = d["payments"][0]["id"]
    act(
        admin,
        "REFUND",
        {
            "sourceId": source,
            "amount": "21",
            "reference": "TEST-REFUND-OVER",
            "note": "TEST",
        },
        status=409,
    )
    act(
        admin,
        "REFUND",
        {
            "sourceId": source,
            "amount": "2",
            "reference": "TEST-REFUND-001",
            "note": "TEST external correction",
        },
    )
    act(
        admin,
        "RECEIVE",
        {"amount": "24", "reference": "TEST-PAYMENT-002", "note": "TEST balance"},
    )
    closed = act(admin, "CLOSE")
    assert closed["state"] == "CLOSED"
    assert Decimal(str(closed["paid"])) == 42
    assert Decimal(str(closed["cost"])) == 63
    export = admin.request("/orders/export", {}, binary=True)
    assert b"TEST exhibition posters" in export
    draft.pop("version", None)
    draft.update(title="TEST pending customer sample", requestId=str(uuid.uuid4()))
    pending = admin.request("/orders", draft)
    act(admin, "QUOTE", order_id=pending["id"])
    admin.upload(pending["id"], "PROOF", "TEST-client-proof.png", PNG)
    files.append(admin.request("/orders/" + str(pending["id"]))["latestProofId"])
    draft.update(title="TEST unfinished removable draft", requestId=str(uuid.uuid4()))
    removable = admin.request("/orders", draft)
    admin.request(
        f"/orders/{removable['id']}/delete", {"version": removable["version"]}
    )
    admin.request("/orders/" + str(removable["id"]), status=404)
    dep = admin.request(
        "/admin/departments",
        {"name": "TEST other organization", "zone": "UTC", "enabled": True},
    )
    account("test_other_manager", 2, dep["id"])
    manager = Client(a.base)
    manager.login("test_other_manager", qa_pw)
    manager.request("/orders/" + str(oid), status=403)
    assert manager.request("/orders")["total"] == 0
    assert not manager.request("/options")["customers"]
    catalogs = admin.request("/admin/menus")
    assert len(catalogs) == 7
    snap = snapshot(admin, [oid, pending["id"]], files)
    private(
        ROOT / "output/quality-state.json",
        {
            "qaPassword": qa_pw,
            "orderIds": [oid, pending["id"]],
            "fileIds": files,
            "snapshot": snap,
        },
    )
    report = {
        "checks": len(checks),
        "passed": True,
        "workflow": "quote revision -> bound customer approval -> current proof -> material conservation -> outsource partial receipt -> production/rework -> independent QC -> partial delivery -> original refund -> full settlement",
        "orders": 2,
        "files": 3,
        "backend": "actual MySQL deployment",
    }
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    main()
