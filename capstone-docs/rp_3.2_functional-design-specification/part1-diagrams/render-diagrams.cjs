// Render the FDS Part 1 Graphviz sources to PNG (and SVG) next to them.
// Needs @viz-js/viz and sharp; set VIZ_MODULES to a folder whose node_modules holds both.
const fs = require('node:fs');
const path = require('node:path');
const { createRequire } = require('node:module');

const modules = process.env.VIZ_MODULES || __dirname;
const runtimeRequire = createRequire(path.join(modules, 'package.json'));
const { instance } = runtimeRequire('@viz-js/viz');
const sharp = runtimeRequire('sharp');

(async () => {
  const viz = await instance();
  for (const file of fs.readdirSync(__dirname).filter(f => f.endsWith('.dot'))) {
    const base = path.join(__dirname, file.replace(/\.dot$/, ''));
    const svg = viz.renderString(fs.readFileSync(path.join(__dirname, file), 'utf8'), { format: 'svg' });
    fs.writeFileSync(`${base}.svg`, svg);
    await sharp(Buffer.from(svg), { density: 144 }).resize({ width: 2400, withoutEnlargement: true })
      .flatten({ background: '#ffffff' }).png().toFile(`${base}.png`);
    console.log(`rendered ${path.basename(base)}.png`);
  }
})();
