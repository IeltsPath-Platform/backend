// Render the SQL-derived conceptual model as PNG and editable draw.io pages.
const fs = require('node:fs');
const path = require('node:path');
const { createRequire } = require('node:module');
const runtimeRequire = createRequire(path.join(process.env.CODEX_ARTIFACT_MODULES, 'package.json'));
const { instance } = runtimeRequire('@viz-js/viz');
const sharp = runtimeRequire('sharp');
const dir = __dirname;
const model = JSON.parse(fs.readFileSync(path.join(dir, 'erd-model.json'), 'utf8'));
const esc = s => String(s).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const quote = s => JSON.stringify(s);

(async () => {
  const viz = await instance();
  const pages = [], manifest = [];
  for (const [index, [service, title, names]] of model.groups.entries()) {
    const primary = names.split(' ').map(n => `${service}.${n}`);
    const shown = new Set(primary);
    // Repeated boundary entities make otherwise disconnected reference-only views readable.
    const extras = {
      'Points':['user.users'],
      'Personal library':['library.learning_videos','library.video_segments','library.vocabulary_senses'],
      'Lesson progress and practice':['user.users','content.lessons'],
      'Review and evidence':['content.knowledge_points','learning.practice_attempts','content.content_package_versions'],
      'Submissions and grading':['user.users','assessment.attempt_items'],
      'Quizzes and leaderboards':['user.users','assessment.assessment_attempts']
    }[title] || [];
    extras.forEach(k => shown.add(k));
    const edges = model.edges.filter(e => shown.has(e.parent) && shown.has(e.child) && primary.includes(e.child));
    const bridges = model.bridges.filter(e => shown.has(e.parent) && shown.has(e.child));
    const direction = ['Lesson progress and practice','Personal library'].includes(title) ? 'LR' : 'TB';
    const graph = ['digraph G {', `graph [rankdir=${direction}, bgcolor="white", pad=0.18, nodesep=0.45, ranksep=0.8, splines=polyline, outputorder=edgesfirst];`,
      'node [shape=plain, fontname="Arial", fontsize=14];',
      'edge [fontname="Arial", fontsize=10, color="#405369", penwidth=1.2, arrowsize=0.8, dir=both];'];
    const labels = {};
    for (const k of shown) {
      const n = model.nodes[k], repeat = !primary.includes(k);
      const fields = [`PK  ${n.pk.join(', ')}`];
      for (const e of edges.filter(e => e.child===k)) fields.push(`${e.kind}  ${e.cols.join(', ')}`);
      // No types or full attribute lists: reference keys only, as required by template.
      const header = `${n.label}${repeat ? ' ['+n.service+']' : ''}`;
      const rows = [header, ...[...new Set(fields)]];
      labels[k] = rows;
      const html = `<TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="7" COLOR="#52667A" BGCOLOR="${repeat?'#F1F3F5':'#FFFFFF'}"><TR><TD BGCOLOR="${repeat?'#E3E7EC':'#DBE7F3'}"><B>${esc(header)}</B></TD></TR>${rows.slice(1).map(t=>`<TR><TD ALIGN="LEFT"><FONT POINT-SIZE="12">${esc(t)}</FONT></TD></TR>`).join('')}</TABLE>`;
      graph.push(`${quote(k)} [label=<${html}>];`);
    }
    for (const [i,e] of edges.entries()) graph.push(`${quote(e.parent)} -> ${quote(e.child)} [id="${i}", arrowtail="${e.optional?'teeodot':'teetee'}", arrowhead="${e.unique?'teeodot':'crowodot'}", style="${e.kind==='Ref'?'dashed':'solid'}"];`);
    for (const [i,e] of bridges.entries()) graph.push(`${quote(e.parent)} -> ${quote(e.child)} [id="${edges.length+i}", arrowtail="crowodot", arrowhead="crowodot", style="${e.kind.includes('Ref')?'dashed':'solid'}", label=${quote(e.via.replaceAll('_',' ')+'\n(association)')}];`);
    graph.push('}');
    const dot = graph.join('\n');
    const basename = `IELTSPath_ERD_${String(index+1).padStart(2,'0')}`;
    fs.writeFileSync(path.join(dir,basename+'.dot'),dot);
    const layout = viz.renderJSON(dot);
    const svg = viz.renderString(dot,{format:'svg'});
    fs.writeFileSync(path.join(dir,basename+'.svg'),svg);
    await sharp(Buffer.from(svg),{density:220}).png().toFile(path.join(dir,basename+'.png'));
    const bb = layout.bb.split(',').map(Number), H=bb[3], cells=['<mxCell id="0"/>','<mxCell id="1" parent="0"/>'];
    const graphIds = {};
    for (const obj of layout.objects) {
      const [cx,cy] = obj.pos.split(',').map(Number), w=Number(obj.width)*72, h=Number(obj.height)*72;
      graphIds[obj._gvid]=obj.name;
      const value = `<b>${esc(labels[obj.name][0])}</b><hr/>${labels[obj.name].slice(1).map(esc).join('<br/>')}`;
      cells.push(`<mxCell id="${esc(obj.name)}" value="${esc(value)}" style="rounded=0;whiteSpace=wrap;html=1;align=left;verticalAlign=top;spacing=7;fillColor=${primary.includes(obj.name)?'#DBE7F3':'#F1F3F5'};strokeColor=#52667A;fontFamily=Arial;fontSize=12;" vertex="1" parent="1"><mxGeometry x="${cx-w/2+20}" y="${H-cy-h/2+20}" width="${w}" height="${h}" as="geometry"/></mxCell>`);
    }
    for (const [ei, edge] of (layout.edges||[]).entries()) {
      const e = [...edges,...bridges][Number(edge.id)];
      const many = e.kind.startsWith('M:N');
      const start=many?'ERzeroToMany':e.optional?'ERzeroToOne':'ERone';
      const end=many?'ERzeroToMany':e.unique?'ERzeroToOne':'ERzeroToMany';
      const points = (edge._draw_||[]).find(d=>d.op==='b')?.points || [];
      const pts=points.slice(1,-1).map(([x,y])=>`<mxPoint x="${x+20}" y="${H-y+20}"/>`).join('');
      const label=many?e.via:'';
      cells.push(`<mxCell id="edge-${ei}" value="${esc(label)}" style="edgeStyle=none;rounded=0;html=1;startArrow=${start};endArrow=${end};startFill=0;endFill=0;startSize=14;endSize=14;strokeColor=#405369;fontFamily=Arial;fontSize=10;labelBackgroundColor=#FFFFFF;dashed=${e.kind.includes('Ref')?1:0};" edge="1" parent="1" source="${esc(graphIds[edge.tail])}" target="${esc(graphIds[edge.head])}"><mxGeometry relative="1" as="geometry"><Array as="points">${pts}</Array></mxGeometry></mxCell>`);
    }
    pages.push(`<diagram id="erd-${index+1}" name="${index+1}. ${esc(title)}"><mxGraphModel dx="1000" dy="1000" grid="1" gridSize="10" page="1" pageScale="1" pageWidth="${bb[2]+40}" pageHeight="${H+40}"><root>${cells.join('')}</root></mxGraphModel></diagram>`);
    manifest.push({index:index+1,service,title,primary,shown:[...shown],png:basename+'.png',width:bb[2],height:H});
  }
  fs.writeFileSync(path.join(dir,'IELTSPath_ERD.drawio'),`<mxfile host="app.diagrams.net">${pages.join('')}</mxfile>`);
  fs.writeFileSync(path.join(dir,'diagram-manifest.json'),JSON.stringify(manifest,null,2));
  console.log(`Rendered ${pages.length} conceptual ERD views.`);
})().catch(e=>{console.error(e);process.exitCode=1;});
