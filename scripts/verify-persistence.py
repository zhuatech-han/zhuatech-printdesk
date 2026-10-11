#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""重启/独立恢复后比对完整记录、目录及二进制附件。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""

import argparse, json, urllib.parse
from quality import ROOT, Client, env, snapshot

p = argparse.ArgumentParser()
p.add_argument("--base", default="http://127.0.0.1:8143")
p.add_argument("--env-file", default=str(ROOT / ".env"))
a = p.parse_args()
if urllib.parse.urlparse(a.base).hostname != "127.0.0.1":
    p.error("Local isolated verification only")
state = json.loads((ROOT / "output/quality-state.json").read_text())
cfg = env(a.env_file)
c = Client(a.base)
c.login(cfg.get("ADMIN_USERNAME", "admin"), cfg["ADMIN_PASSWORD"])
now = snapshot(c, state["orderIds"], state["fileIds"])
assert now == state["snapshot"], "Persistence or restore mismatch"
print(
    json.dumps(
        {
            "passed": True,
            "completeOrders": len(now["orders"]),
            "binaryFiles": len(now["files"]),
            "catalogs": len(now["catalogs"]),
        }
    )
)
