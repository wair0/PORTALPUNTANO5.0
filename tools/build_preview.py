#!/usr/bin/env python3
"""Genera una vista previa HTML autónoma a partir de los assets de la app."""
import pathlib

root = pathlib.Path(__file__).resolve().parent.parent
ui = root / "app/src/main/assets/ui"
read = lambda p: (ui / p).read_text(encoding="utf-8")
css = read("css/tokens.css") + "\n" + read("css/components.css")
libs = "\n".join(read(f"js/{n}.js") for n in ("icons", "mock", "bridge", "screens"))
app = read("js/app.js")
tpl = (root / "tools/preview_template.html").read_text(encoding="utf-8")
out = tpl.replace("/*__CSS__*/", css).replace("/*__JS__*/", libs).replace("/*__APP__*/", app)
dest = root / "preview/portal-puntano-5-preview.html"
dest.write_text(out, encoding="utf-8")
print(f"OK -> {dest} ({dest.stat().st_size / 1024:.0f} KB)")