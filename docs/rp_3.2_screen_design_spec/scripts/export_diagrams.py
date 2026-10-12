"""Export every draw.io tab to PNG with the draw.io Desktop CLI.

Usage:  python export_diagrams.py [--force] [--drawio "C:/Program Files/draw.io/draw.io.exe"]
Each tab '<ID> / <Frame>' becomes diagrams/exports/<ID>__<frame-slug>.png.
"""
import argparse
import os
import re
import subprocess
import sys
import unicodedata
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))
DIAGRAMS = os.path.join(ROOT, "diagrams")
EXPORTS = os.path.join(DIAGRAMS, "exports")
DEFAULT_DRAWIO = r"C:\Program Files\draw.io\draw.io.exe"


def slug(text):
    text = unicodedata.normalize("NFKD", text.replace("đ", "d").replace("Đ", "D"))
    text = text.encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "-", text).strip("-")


def export_name(page_name):
    """'P-30a / Exam — Writing' -> 'P-30a__exam-writing'."""
    if " / " in page_name:
        ident, frame = page_name.split(" / ", 1)
        return f"{ident.strip()}__{slug(frame)}"
    return slug(page_name)


def drawio_files():
    for base, _, files in os.walk(DIAGRAMS):
        if os.path.abspath(base).startswith(os.path.abspath(EXPORTS)):
            continue
        for name in sorted(files):
            if name.endswith(".drawio") and "Prototype" not in name:  # prototype reuses the wireframe tabs
                yield os.path.join(base, name)


def jobs(force):
    for path in drawio_files():
        pages = [d.get("name") for d in ET.parse(path).getroot().findall("diagram")]
        for index, page in enumerate(pages, start=1):
            out = os.path.join(EXPORTS, export_name(page) + ".png")
            if force or not os.path.exists(out) or os.path.getmtime(out) < os.path.getmtime(path):
                yield path, index, out


def run(exe, job):
    path, index, out = job
    cmd = [exe, "--export", "--format", "png", "--scale", "1.25", "--border", "24",
           "--page-index", str(index), "--output", out, path]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=180)
    ok = result.returncode == 0 and os.path.exists(out)
    return out, ok, result.stderr.strip()[-300:]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--drawio", default=DEFAULT_DRAWIO)
    args = parser.parse_args()
    os.makedirs(EXPORTS, exist_ok=True)
    todo = list(jobs(args.force))
    print(f"{len(todo)} tab(s) to export")
    failed = 0
    with ThreadPoolExecutor(max_workers=4) as pool:
        for out, ok, err in pool.map(lambda j: run(args.drawio, j), todo):
            print(("ok   " if ok else "FAIL ") + os.path.relpath(out, ROOT) + ("" if ok else f"  {err}"))
            failed += 0 if ok else 1
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
