#!/usr/bin/env python3
"""Builds the hosted legal pages from the Markdown sources in this folder.

    python3 legal/build_html.py
    firebase deploy --only hosting

Output goes to ../hosting/public/{privacy,terms,delete-account}.html, served by
Firebase Hosting (cleanUrls) at https://colormagic-555.web.app/<name>.
Handles only the Markdown subset the legal docs use: headings, paragraphs,
bullet/numbered lists, tables, horizontal rules, **bold**, and [links](url).
"""
import html
import pathlib
import re

HERE = pathlib.Path(__file__).parent
OUT = HERE.parent / "hosting" / "public"
PAGES = {
    "PRIVACY_POLICY.md": "privacy.html",
    "TERMS_AND_CONDITIONS.md": "terms.html",
    "DELETE_ACCOUNT.md": "delete-account.html",
}

CSS = """
:root{color-scheme:light dark;--ink:#1d1b20;--muted:#5f5b66;--line:#e4e0ea;--bg:#fbf9fd;--accent:#6b4fa0}
@media (prefers-color-scheme:dark){:root{--ink:#ece8f2;--muted:#b3adbd;--line:#3a3542;--bg:#17151b;--accent:#c4a8f5}}
body{margin:0;background:var(--bg);color:var(--ink);font:16px/1.65 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
main{max-width:760px;margin:0 auto;padding:32px 20px 64px}
h1{font-size:1.9rem;line-height:1.25;margin:0 0 8px}
h2{font-size:1.3rem;margin:36px 0 8px}
h3{font-size:1.05rem;margin:24px 0 6px}
a{color:var(--accent)}
hr{border:0;border-top:1px solid var(--line);margin:28px 0}
.table{overflow-x:auto}
table{border-collapse:collapse;width:100%;font-size:.94rem;margin:12px 0}
th,td{border:1px solid var(--line);padding:8px 10px;text-align:left;vertical-align:top}
th{background:color-mix(in srgb,var(--accent) 10%,transparent)}
li{margin:4px 0}
"""


def inline(text: str) -> str:
    text = html.escape(text, quote=False)
    text = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", text)
    text = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', text)
    text = re.sub(r"(?<![\"=])(https://[^\s<·]+)", r'<a href="\1">\1</a>', text)
    return text


def convert(md: str) -> tuple[str, str]:
    lines = md.splitlines()
    out, title, i = [], "ColorMagic Kids", 0
    while i < len(lines):
        line = lines[i]
        if not line.strip():
            i += 1
        elif line.startswith("#"):
            level = len(line) - len(line.lstrip("#"))
            text = line[level:].strip()
            if level == 1:
                title = text
            out.append(f"<h{level}>{inline(text)}</h{level}>")
            i += 1
        elif line.strip() == "---":
            out.append("<hr>")
            i += 1
        elif line.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].startswith("|"):
                cells = [c.strip() for c in lines[i].strip().strip("|").split("|")]
                if not all(re.fullmatch(r":?-+:?", c) for c in cells):
                    rows.append(cells)
                i += 1
            head, *body = rows
            t = "<tr>" + "".join(f"<th>{inline(c)}</th>" for c in head) + "</tr>"
            t += "".join("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in r) + "</tr>" for r in body)
            out.append(f'<div class="table"><table>{t}</table></div>')
        elif re.match(r"(- |\d+\. )", line):
            tag = "ol" if line[0].isdigit() else "ul"
            items = []
            while i < len(lines) and (re.match(r"(- |\d+\. )", lines[i]) or lines[i].startswith("  ")):
                if lines[i].startswith("  "):
                    items[-1] += " " + lines[i].strip()
                else:
                    items.append(re.sub(r"^(- |\d+\. )", "", lines[i]))
                i += 1
            out.append(f"<{tag}>" + "".join(f"<li>{inline(x)}</li>" for x in items) + f"</{tag}>")
        else:
            para = []
            while i < len(lines) and lines[i].strip() and not re.match(r"(#|\||- |\d+\. |---)", lines[i]):
                para.append(lines[i].strip())
                i += 1
            out.append(f"<p>{inline(' '.join(para))}</p>")
    return title, "\n".join(out)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for src, dest in PAGES.items():
        title, body = convert((HERE / src).read_text())
        page = (
            '<!doctype html><html lang="en"><head><meta charset="utf-8">'
            '<meta name="viewport" content="width=device-width,initial-scale=1">'
            f"<title>{html.escape(title)}</title><style>{CSS}</style></head>"
            f"<body><main>{body}</main></body></html>\n"
        )
        (OUT / dest).write_text(page)
        print(f"wrote {OUT / dest}")


if __name__ == "__main__":
    main()
