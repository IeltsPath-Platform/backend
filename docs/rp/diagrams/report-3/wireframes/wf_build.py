"""Render every wireframe of Part III to PNG and write wf_index.json for the document builder.

Run: python wf_build.py   (all wf_data_*.py; the PNG files are taken by wf_shoot.mjs, which needs Node 22+)
Each wf_data_*.py has FEATURE and SCREENS (see wf_render.py). Output goes to screens/.
"""
import importlib
import json
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from wf_render import render  # noqa: E402

OUT = Path(__file__).parent / 'screens'


def slug(name):
    return re.sub(r'[^a-z0-9]+', '-', name.lower()).strip('-')


def main():
    OUT.mkdir(exist_ok=True)
    index = []
    for module_name in sorted(p.stem for p in Path(__file__).parent.glob('wf_data_*.py')):
        module = importlib.import_module(module_name)
        groups = module.GROUPS if hasattr(module, 'GROUPS') else [(module.FEATURE, module.SCREENS)]
        for feature, screens in groups:
            for screen in screens:
                svg, width, height, elements = render(screen)
                name = slug(feature + '-' + screen['name'])
                html = (f'<!doctype html><meta charset="utf-8"><body style="margin:0;background:#fff;overflow:hidden">'
                        f'<div style="line-height:0">{svg}</div></body>')
                (OUT / f'{name}.html').write_text(html, encoding='utf-8')
                (OUT / f'{name}.svg').write_text(svg, encoding='utf-8')
                index.append(dict(file=name, feature=feature, name=screen['name'], width=width, height=height,
                                  ucs=screen.get('ucs', ''), note=screen.get('note', ''),
                                  shell=screen.get('shell', 'site'), elements=elements, source=module_name))
    (Path(__file__).parent / 'wf_index.json').write_text(json.dumps(index, indent=1), encoding='utf-8')
    subprocess.run(['node', str(Path(__file__).with_name('wf_shoot.mjs'))], check=True, timeout=600)
    print('screens:', len(index))


main()
