#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""核对发布文件、品牌、许可、双语图片与私有凭据隔离。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""

import hashlib, json, re, subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
files = (
    subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode().split("\0")
)
files = [f for f in files if f]
assert files, "Stage source files before release verification"
assert not any(
    f.startswith(("output/", "private-backups/", ".venv/"))
    or f in [".env", ".env.restore"]
    or "/node_modules/" in f
    or "/target/" in f
    for f in files
), "Private/build data tracked"
cn = (ROOT / "README.md").read_text()
en = (ROOT / "README.en.md").read_text()
license = (ROOT / "LICENSE").read_text()
for text in [cn, en]:
    assert "[中文](README.md) | [English](README.en.md)" in text
    assert "https://www.zhuatech.cn/" in text and "LICENSE" in text
    paths = re.findall(r'(?:src="|!\[[^\]]*\]\()([^"\)]+)', text)
    for path in paths:
        if not path.startswith(("http:", "https:")):
            assert (ROOT / path).is_file() and path in files, path
assert "上海如静知华信息科技有限公司" in license and "Non-Commercial" in license
assert (
    "han@zhuatech.cn" in en
    and "jack@zhuatech.cn" in en
    and "https://wa.me/8617521234993" in en
)
assert "wechat-" not in en and "zhuatech2" in cn and "zhuatech" in cn
for name in ["wechat-zhuatech.png", "wechat-zhuatech2.png"]:
    a = ROOT / "docs/images" / name
    b = ROOT / "frontend/public/brand" / name
    assert a.read_bytes() == b.read_bytes(), name
screens = [f for f in files if f.startswith("docs/screenshots/") and f.endswith(".jpg")]
assert len(screens) >= 6
secrets = []
for env in [ROOT / ".env", ROOT / ".env.restore"]:
    if env.exists():
        for line in env.read_text().splitlines():
            if (
                "=" in line
                and "PASSWORD" in line.split("=", 1)[0]
                and len(line.split("=", 1)[1]) > 10
            ):
                secrets.append(line.split("=", 1)[1])
state = ROOT / "output/quality-state.json"
if state.exists():
    secrets.append(json.loads(state.read_text())["qaPassword"])
brand = 0
for f in files:
    p = ROOT / f
    if p.suffix in [
        ".java",
        ".js",
        ".vue",
        ".css",
        ".py",
        ".sql",
        ".yml",
        ".yaml",
        ".xml",
        ".md",
    ] or p.name in ["Dockerfile", "nginx.conf"]:
        text = p.read_text()
        assert "https://www.zhuatech.cn/" in text and "zhuatech" in text, f
        brand += 1
    data = p.read_bytes()
    assert not any(secret.encode() in data for secret in secrets), f
    assert (
        b"-----BEGIN " + b"PRIVATE KEY-----" not in data
        and b"-----BEGIN " + b"OPENSSH PRIVATE KEY-----" not in data
    ), f
print(
    json.dumps(
        {
            "trackedFiles": len(files),
            "brandFiles": brand,
            "screenshots": len(screens),
            "bilingualReadmes": "passed",
            "originalQrCopies": "matched",
            "sensitiveLeaks": 0,
        }
    )
)
