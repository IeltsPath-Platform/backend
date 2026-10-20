"""Export the four course Task 1 charts as SVG and PNG. Requires ImageMagick on PATH.

Run: python backend/scripts/generate-course-writing-charts.py
Data matches content-service's V63 course Writing prompts. No API or credentials are used.
"""

from html import escape
from math import cos, sin, pi
from pathlib import Path
import shutil
import subprocess
from zipfile import ZIP_DEFLATED, ZipFile


OUTPUT = Path(__file__).resolve().parents[1] / 'docs' / 'assets' / 'course-writing-charts'
BLUE, GOLD, INK, MUTED, GRID = '#5261cf', '#edaa32', '#1e293b', '#64748b', '#e2e8f0'
WIDTH, HEIGHT = 1600, 1000


def text(x, y, value, size=25, fill=INK, anchor='middle', weight='400'):
    return (f'<text x="{x:.2f}" y="{y:.2f}" font-size="{size}" fill="{fill}" '
            f'text-anchor="{anchor}" font-weight="{weight}">{escape(str(value))}</text>')


def line(x1, y1, x2, y2, color=GRID, width=2, dash=''):
    return (f'<line x1="{x1:.2f}" y1="{y1:.2f}" x2="{x2:.2f}" y2="{y2:.2f}" '
            f'stroke="{color}" stroke-width="{width}" stroke-dasharray="{dash}"/>')


def rect(x, y, width, height, fill, radius=0):
    return (f'<rect x="{x:.2f}" y="{y:.2f}" width="{width:.2f}" height="{height:.2f}" '
            f'rx="{radius}" fill="{fill}"/>')


def canvas(title, subtitle):
    return [f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{HEIGHT}" '
            f'viewBox="0 0 {WIDTH} {HEIGHT}" font-family="Arial,Helvetica,sans-serif">',
            rect(0, 0, WIDTH, HEIGHT, '#ffffff'),
            text(800, 76, title, 40, weight='700'), text(800, 125, subtitle, 26, MUTED)]


def axes(parts, labels, low, high, ticks, formatter=str):
    left, right, top, bottom = 125, 1490, 220, 800

    def y(value):
        return bottom - (value - low) / (high - low) * (bottom - top)

    for value in ticks:
        parts.extend([line(left, y(value), right, y(value)),
                      text(left - 22, y(value) + 9, formatter(value), 24, MUTED, 'end')])
    parts.append(line(left, top, left, bottom, MUTED))
    parts.append(line(left, bottom, right, bottom, MUTED))
    xs = [left + index * (right - left) / (len(labels) - 1) for index in range(len(labels))]
    for x, label in zip(xs, labels):
        parts.append(text(x, bottom + 48, label, 24, MUTED))
    return xs, y


def legend(parts, items):
    start = 800 - len(items) * 150
    for index, (name, color) in enumerate(items):
        x = start + index * 300
        parts.extend([rect(x, 912, 30, 24, color, 3), text(x + 46, 935, name, 25, INK, 'start')])


def car_chart():
    parts = canvas('Households owning a car', 'Percentage of households in four countries, 2000 and 2020')
    left, right, top, bottom = 125, 1490, 220, 800
    for value in range(0, 101, 20):
        y = bottom - value / 100 * (bottom - top)
        parts.extend([line(left, y, right, y), text(left - 22, y + 9, f'{value}%', 24, MUTED, 'end')])
    parts.append(line(left, bottom, right, bottom, MUTED))
    values = [('Country A', 60, 72), ('Country B', 45, 58), ('Country C', 30, 55), ('Country D', 70, 68)]
    group_width = (right - left) / len(values)
    for index, (country, first, second) in enumerate(values):
        center = left + (index + .5) * group_width
        parts.append(text(center, 850, country, 27))
        for x, value, color in [(center - 92, first, BLUE), (center + 12, second, GOLD)]:
            height = value / 100 * (bottom - top)
            parts.extend([rect(x, bottom - height, 80, height, color, 5),
                          text(x + 40, bottom - height - 18, f'{value}%', 27, weight='700')])
    legend(parts, [('2000', BLUE), ('2020', GOLD)])
    return parts


def spending_chart():
    parts = canvas('Monthly household spending', 'Share of income by category (%), 1990 and 2020')
    colors = [BLUE, GOLD, '#37a89a', '#dd798b', '#8291a5']
    names = ['Housing', 'Food', 'Transport', 'Leisure', 'Other']
    radius, cy = 235, 455
    for cx, year, values in [(420, '1990', [25, 30, 10, 15, 20]), (1180, '2020', [35, 18, 15, 20, 12])]:
        parts.append(text(cx, 205, year, 34, weight='700'))
        start = -pi / 2
        for value, color in zip(values, colors):
            end = start + value / 100 * 2 * pi
            x1, y1 = cx + radius * cos(start), cy + radius * sin(start)
            x2, y2 = cx + radius * cos(end), cy + radius * sin(end)
            parts.append(f'<path d="M {cx} {cy} L {x1:.3f} {y1:.3f} A {radius} {radius} 0 '
                         f'{int(value > 50)} 1 {x2:.3f} {y2:.3f} Z" fill="{color}" stroke="white" stroke-width="4"/>')
            middle = (start + end) / 2
            parts.append(text(cx + radius * .67 * cos(middle), cy + radius * .67 * sin(middle) + 11,
                              f'{value}%', 32, '#ffffff', weight='700'))
            start = end
    for index, (name, color) in enumerate(zip(names, colors)):
        x = 155 + index * 270
        parts.extend([rect(x, 810, 30, 26, color, 3), text(x + 45, 834, name, 25, anchor='start')])
    parts.append(text(800, 928, 'Each chart represents 100% of monthly income.', 25, MUTED))
    return parts


def park_chart():
    parts = canvas('Visitors to a national park', 'Monthly visitor numbers, in thousands')
    months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
    values = [20, 22, 35, 50, 70, 90, 110, 105, 60, 40, 25, 30]
    xs, y = axes(parts, months, 0, 120, range(0, 121, 20))
    points = ' '.join(f'{x:.3f},{y(value):.3f}' for x, value in zip(xs, values))
    parts.append(f'<polyline points="{points}" fill="none" stroke="{BLUE}" stroke-width="5" stroke-linejoin="round"/>')
    for x, value in zip(xs, values):
        parts.extend([f'<circle cx="{x:.3f}" cy="{y(value):.3f}" r="8" fill="{BLUE}"/>',
                      text(x, y(value) - 21, value, 25, weight='700')])
    parts.append(text(800, 935, 'Unit: thousands of visitors', 25, MUTED))
    return parts


def fuel_chart():
    parts = canvas('Average fuel prices', 'Petrol and diesel, 2015–2020 · dollars per litre')
    petrol = [1.10, 1.05, 1.20, 1.35, 1.30, 1.00]
    diesel = [1.00, .95, 1.15, 1.40, 1.38, 1.05]
    xs, y = axes(parts, [str(year) for year in range(2015, 2021)], .8, 1.5,
                 [value / 10 for value in range(8, 16)], lambda value: f'${value:.2f}')
    for values, other, color, dash in [(petrol, diesel, BLUE, ''), (diesel, petrol, GOLD, '12 8')]:
        points = ' '.join(f'{x:.3f},{y(value):.3f}' for x, value in zip(xs, values))
        parts.append(f'<polyline points="{points}" fill="none" stroke="{color}" stroke-width="5" stroke-dasharray="{dash}"/>')
        for index, (x, value) in enumerate(zip(xs, values)):
            offset = -20 if value > other[index] else 35
            parts.extend([f'<circle cx="{x:.3f}" cy="{y(value):.3f}" r="8" fill="{color}"/>',
                          text(x, y(value) + offset, f'${value:.2f}', 25, weight='700')])
    legend(parts, [('Petrol (solid)', BLUE), ('Diesel (dashed)', GOLD)])
    return parts


def main():
    magick = shutil.which('magick')
    if not magick:
        raise SystemExit('ImageMagick (magick) must be installed and available on PATH to export PNGs.')
    OUTPUT.mkdir(parents=True, exist_ok=True)
    charts = {
        'ps-w1-wr-1-car-ownership': car_chart,
        'ps-w1-wr-2-family-spending': spending_chart,
        'ps-w5-wr-1-park-visitors': park_chart,
        'ps-w5-wr-2-fuel-prices': fuel_chart,
    }
    for filename, render in charts.items():
        svg = OUTPUT / f'{filename}.svg'
        png = OUTPUT / f'{filename}.png'
        svg.write_text('\n'.join([*render(), '</svg>']), encoding='utf-8')
        subprocess.run([magick, '-background', 'white', str(svg), str(png)], check=True)
        print(png)

    with ZipFile(OUTPUT / 'course-writing-charts.zip', 'w', ZIP_DEFLATED) as archive:
        for filename in [*(f'{name}.png' for name in charts), 'README.md', 'upload-urls.csv']:
            path = OUTPUT / filename
            if path.exists():
                archive.write(path, arcname=filename)


if __name__ == '__main__':
    main()
