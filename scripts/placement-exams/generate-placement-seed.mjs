#!/usr/bin/env node
// Dev-only generator for the placement exams added after the first seeded exam. It reads one JSON file per exam from
// ./exams and writes the Flyway migrations and the recording and review documents. Ids are deterministic (UUID v5 of
// the package code and the entity path), so a rerun produces byte-identical files.
//
//   node scripts/placement-exams/generate-placement-seed.mjs             write migrations and documents
//   node scripts/placement-exams/generate-placement-seed.mjs --check     fail when the committed files differ
//   node scripts/placement-exams/generate-placement-seed.mjs --audio URL fail unless every recording is reachable
//   node scripts/placement-exams/generate-placement-seed.mjs --keys      print the twelve bucket keys
//
// No npm dependency: Node 20 or later.
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const BACKEND = resolve(HERE, '..', '..');
export const EXAMS_DIR = join(HERE, 'exams');
export const MIGRATION_DIR = join(BACKEND, 'services/content-service/src/main/resources/db/migration');
export const DOCS_DIR = join(BACKEND, 'docs/placement-exams');
const NAMESPACE = '8c0f2d6e-5b1a-4e7c-9d3f-2a6b4c8d0e11';

/** Pool settings: the migration file, the reading pace used to estimate recording length, and the authoring level. */
export const POOLS = {
  BAND_0_3: { migration: 'V138__seed_placement_exams_band_0_3.sql', wpm: 95, shape: 'short', level: 'A1-A2' },
  BAND_3_5: { migration: 'V139__seed_placement_exams_band_3_5.sql', wpm: 110, shape: 'short', level: 'A2-B1' },
  BAND_5_9: { migration: 'V140__seed_placement_exams_band_5_9.sql', wpm: 120, shape: 'full', level: 'B1-C2' },
};
const CODE_PATTERN = { BAND_0_3: /^PLACEMENT-B03-[A-Z]$/, BAND_3_5: /^PLACEMENT-B35-[A-Z]$/, BAND_5_9: /^PLACEMENT-B59-[A-Z]$/ };

export function uuid5(name) {
  const hash = createHash('sha1').update(Buffer.from(NAMESPACE.replace(/-/g, ''), 'hex')).update(name).digest();
  hash[6] = (hash[6] & 0x0f) | 0x50;
  hash[8] = (hash[8] & 0x3f) | 0x80;
  const hex = hash.subarray(0, 16).toString('hex');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

// ---------------------------------------------------------------------------------------------------------------
// Loading and validation

export function loadExams(dir = EXAMS_DIR) {
  return readdirSync(dir).filter((name) => name.endsWith('.json')).sort()
    .map((name) => JSON.parse(readFileSync(join(dir, name), 'utf8')));
}

function fail(exam, message) {
  throw new Error(`${exam.code ?? '?'}: ${message}`);
}

function require(exam, condition, message) {
  if (!condition) fail(exam, message);
}

const words = (text) => text.trim().split(/\s+/).filter(Boolean).length;

/** Converts the compact question notation of the sources into the rows the migration needs. */
export function normalizeQuestion(exam, skill, source) {
  if (source.list) {
    const shared = exam.lists?.[source.list];
    require(exam, shared && shared.keys && shared.options, `question "${source.q}" uses the unknown list ${source.list}`);
    source = { ...source, options: shared.options, keys: shared.keys };
  }
  require(exam, typeof source.q === 'string' && source.q.trim(), 'a question has no text');
  require(exam, typeof source.why === 'string' && source.why.trim(), `question "${source.q}" has no explanation`);
  const base = { stem: source.q, explanation: source.why, skill };
  if (source.fill) {
    require(exam, Array.isArray(source.fill) && source.fill.length > 0 && source.fill.every((a) => typeof a === 'string' && a.trim()),
      `question "${source.q}" needs a non-empty list of accepted answers`);
    return { ...base, questionType: 'FILL_IN_BLANK', options: null,
      spec: { type: 'FILL', accepted: source.fill.map((answer) => answer.toLowerCase()) } };
  }
  if (source.tfng || source.yn) {
    const keys = source.tfng ? ['TRUE', 'FALSE', 'NOT_GIVEN'] : ['YES', 'NO', 'NOT_GIVEN'];
    const answer = source.tfng ?? source.yn;
    require(exam, keys.includes(answer), `question "${source.q}" has an invalid answer ${answer}`);
    return { ...base, questionType: source.tfng ? 'TRUE_FALSE_NOT_GIVEN' : 'MULTIPLE_CHOICE',
      options: keys.map((key, index) => ({ optionKey: key, content: key.replace('_', ' '), sortOrder: index + 1 })),
      spec: { type: 'CHOICE', correct: answer } };
  }
  if (source.options) {
    const keys = source.keys ?? source.options.map((_, index) => String.fromCharCode(65 + index));
    require(exam, keys.length === source.options.length, `question "${source.q}" has a different number of keys and options`);
    require(exam, keys.includes(source.answer), `question "${source.q}" has an answer that is not one of its options`);
    return { ...base, questionType: 'MULTIPLE_CHOICE',
      options: source.options.map((content, index) => ({ optionKey: keys[index], content, sortOrder: index + 1 })),
      spec: { type: 'CHOICE', correct: source.answer } };
  }
  return fail(exam, `question "${source.q}" is neither fill, tfng, yn nor choice`);
}

/** Builds the section list in the order of the test, with every question normalised. Throws on any invalid source. */
export function buildExam(exam) {
  const pool = POOLS[exam.pool];
  require(exam, pool, `unknown pool ${exam.pool}`);
  require(exam, CODE_PATTERN[exam.pool].test(exam.code), `code does not match the pool ${exam.pool}`);
  require(exam, exam.title && exam.title.trim(), 'no title');
  const sections = [];
  let audioNumber = 0;

  for (const part of exam.listening ?? []) {
    require(exam, part.transcript && part.transcript.trim(), `${part.title}: no transcript`);
    require(exam, part.voices && part.voices.trim(), `${part.title}: no voice notes`);
    audioNumber += 1;
    const turns = part.transcript.split('\n').filter((line) => line.trim()).length;
    // Reading time at the pool's pace, a short pause at every change of line, and the silence at both ends.
    const seconds = Math.max(20, Math.round(words(part.transcript.replace(/^[A-Za-z ]+: /gm, '')) / pool.wpm * 60 + turns * 0.7 + 2));
    sections.push({ kind: 'LISTENING', title: part.title, instructions: part.instructions, limit: null,
      audio: { number: audioNumber, key: `listening/placement/${exam.code.toLowerCase()}/part${audioNumber}.mp3`,
        seconds, transcript: part.transcript, voices: part.voices },
      questions: part.questions.map((q) => normalizeQuestion(exam, 'LISTENING', q)) });
  }
  for (const passage of exam.reading ?? []) {
    require(exam, passage.passage && passage.passage.trim(), `${passage.title}: no passage`);
    sections.push({ kind: 'READING', title: passage.title, instructions: passage.instructions,
      limit: passage.minutes * 60, passage: passage.passage,
      questions: passage.questions.map((q) => normalizeQuestion(exam, 'READING', q)) });
  }
  for (const task of exam.writing ?? []) {
    require(exam, task.task === 'TASK_1' || task.task === 'TASK_2', `${task.title}: task must be TASK_1 or TASK_2`);
    require(exam, Number.isInteger(task.minWords) && task.minWords > 0, `${task.title}: minWords`);
    require(exam, Number.isInteger(task.passBand * 2) && task.passBand >= 0 && task.passBand <= 9, `${task.title}: passBand`);
    if (task.task === 'TASK_1') {
      require(exam, task.table && task.chartFacts, `${task.title}: Task 1 needs a table and chartFacts`);
    }
    const spec = { type: 'ESSAY', task: task.task, minWords: task.minWords, passBand: task.passBand };
    if (task.chartFacts) spec.chartFacts = task.chartFacts;
    sections.push({ kind: 'WRITING', title: task.title, instructions: task.instructions, limit: task.minutes * 60,
      table: task.table ?? null,
      questions: [{ questionType: 'ESSAY', skill: 'WRITING', stem: task.stem, options: null, spec,
        explanation: 'Graded by the placement essay grader.' }] });
  }
  for (const part of exam.speaking ?? []) {
    sections.push({ kind: 'SPEAKING', title: part.title, instructions: part.instructions, limit: null,
      questions: part.questions.map((q) => ({ questionType: 'SPEAKING', skill: 'SPEAKING', stem: q.q, options: null,
        spec: { type: 'SPEAKING', durationSeconds: q.seconds },
        explanation: 'Recorded answer; counted only when a placement speaking band is configured.' })) });
  }
  validateShape(exam, pool.shape, sections);
  return { exam, sections };
}

function validateShape(exam, shape, sections) {
  const count = (kind) => sections.filter((s) => s.kind === kind);
  const sizes = (kind) => count(kind).map((s) => s.questions.length);
  const same = (actual, expected) => JSON.stringify(actual) === JSON.stringify(expected);
  if (shape === 'short') {
    require(exam, same(sizes('LISTENING'), [10, 10]), `Listening must be 2 parts of 10 questions, found ${sizes('LISTENING')}`);
    require(exam, same(sizes('READING'), [10, 10]), `Reading must be 2 passages of 10 questions, found ${sizes('READING')}`);
    require(exam, same(sizes('WRITING'), [1]), 'Writing must be one task');
    require(exam, count('SPEAKING').reduce((sum, s) => sum + s.questions.length, 0) === 2, 'Speaking must be 2 prompts');
  } else {
    require(exam, same(sizes('LISTENING'), [10, 10, 10, 10]), `Listening must be 4 parts of 10 questions, found ${sizes('LISTENING')}`);
    require(exam, same(sizes('READING'), [13, 13, 14]), `Reading must be passages of 13, 13 and 14 questions, found ${sizes('READING')}`);
    require(exam, same(count('WRITING').map((s) => s.questions[0].spec.task), ['TASK_1', 'TASK_2']), 'Writing must be Task 1 then Task 2');
    require(exam, same(sizes('SPEAKING'), [4, 1, 3]), `Speaking must be parts of 4, 1 and 3 prompts, found ${sizes('SPEAKING')}`);
  }
}

// ---------------------------------------------------------------------------------------------------------------
// SQL

const lit = (value) => `'${String(value).replaceAll("'", "''")}'`;
const jsonb = (value) => `${lit(JSON.stringify(value))}::jsonb`;
const nullable = (value) => (value === null || value === undefined ? 'NULL' : lit(value));

export function examSql({ exam, sections }) {
  const id = (...parts) => uuid5([exam.code, ...parts].join('|'));
  const packageId = id('package');
  const versionId = id('version');
  const assets = [];
  const questions = [];
  const versions = [];
  const sectionRows = [];
  const links = [];
  const items = [];

  sections.forEach((section, sectionIndex) => {
    const sectionId = id('section', sectionIndex);
    sectionRows.push(`    (${lit(sectionId)}, ${lit(versionId)}, ${lit(section.title)}, ${lit(section.kind)}, ${sectionIndex + 1}, ${section.limit ?? 'NULL'}, ${nullable(section.instructions)})`);
    if (section.audio) {
      const assetId = id('asset', sectionIndex, 'audio');
      assets.push({ kind: 'AUDIO', row: `    (${lit(assetId)}, 'AUDIO', ${lit(section.audio.transcript)}, ${lit(section.audio.key)}, ${section.audio.seconds}, 'VALID')` });
      links.push(`    (${lit(id('link', sectionIndex, 'audio'))}, ${lit(assetId)}, ${lit(sectionId)}, 0)`);
    }
    for (const [text, name] of [[section.passage, 'passage'], [section.table, 'table']]) {
      if (!text) continue;
      const assetId = id('asset', sectionIndex, name);
      assets.push({ kind: 'TEXT', row: `    (${lit(assetId)}, 'PASSAGE', ${lit(text)}, 'VALID')` });
      links.push(`    (${lit(id('link', sectionIndex, name))}, ${lit(assetId)}, ${lit(sectionId)}, 0)`);
    }
    section.questions.forEach((question, questionIndex) => {
      const questionId = id('question', sectionIndex, questionIndex);
      const questionVersionId = id('question-version', sectionIndex, questionIndex);
      questions.push(`    (${lit(questionId)}, ${lit(question.questionType)}, ${lit(question.skill)}, 'PLACEMENT', 'PUBLISHED')`);
      versions.push(`    (${lit(questionVersionId)}, ${lit(questionId)}, 1, ${lit(question.stem)}, ${question.options ? jsonb(question.options) : 'NULL'},\n     ${jsonb(question.spec)}, 1, ${lit(question.explanation)}, 'PUBLISHED')`);
      items.push(`    (${lit(id('section-question', sectionIndex, questionIndex))}, ${lit(sectionId)}, ${lit(questionVersionId)}, ${questionIndex + 1}, 1.0)`);
    });
  });

  const audio = assets.filter((a) => a.kind === 'AUDIO').map((a) => a.row);
  const text = assets.filter((a) => a.kind === 'TEXT').map((a) => a.row);
  const out = [`-- ${exam.code}: ${exam.title}`];
  if (audio.length) out.push(`INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES\n${audio.join(',\n')};`);
  if (text.length) out.push(`INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES\n${text.join(',\n')};`);
  out.push(`INSERT INTO questions (id, question_type, skill, purpose, status) VALUES\n${questions.join(',\n')};`);
  out.push(`INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES\n${versions.join(',\n')};`);
  const versionIds = sections.flatMap((s, si) => s.questions.map((_, qi) => `    ${lit(id('question-version', si, qi))}`));
  out.push(`UPDATE questions q SET current_published_version_id = qv.id\nFROM question_versions qv\nWHERE qv.question_id = q.id AND qv.id IN (\n${versionIds.join(',\n')});`);
  out.push(`INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, course_id, status, required_feature_key, placement_pool)\nVALUES (${lit(packageId)}, ${lit(exam.code)}, ${lit(exam.title)}, 'PLACEMENT_TEST', NULL, NULL, NULL, 'PUBLISHED', NULL, ${lit(exam.pool)});`);
  out.push(`INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)\nVALUES (${lit(versionId)}, ${lit(packageId)}, 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP);`);
  out.push(`INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions) VALUES\n${sectionRows.join(',\n')};`);
  if (links.length) out.push(`INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES\n${links.join(',\n')};`);
  out.push(`INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES\n${items.join(',\n')};`);
  out.push(`UPDATE content_packages SET current_published_version_id = ${lit(versionId)}, updated_at = CURRENT_TIMESTAMP\nWHERE id = ${lit(packageId)};`);
  return out.join('\n\n') + '\n';
}

function migrationFile(pool, built) {
  const header = `-- Placement exams of the ${pool} pool, published when this migration runs. Generated from\n`
    + `-- scripts/placement-exams/exams by scripts/placement-exams/generate-placement-seed.mjs; edit the sources and\n`
    + `-- regenerate, never this file. Listening audio keys (listening/placement/<package code>/part<N>.mp3) must be\n`
    + `-- uploaded to the media bucket before the migration is deployed.\n\n`;
  return header + built.map(examSql).join('\n');
}

// ---------------------------------------------------------------------------------------------------------------
// Documents (Vietnamese instructions, English exam content)

const mmss = (seconds) => `${Math.floor(seconds / 60)} phút ${String(seconds % 60).padStart(2, '0')} giây`;

function transcriptBlock(transcript) {
  return transcript.split('\n').filter((line) => line.trim())
    .map((line) => line.replace(/^([A-Za-z ]+): /, '**$1:** ')).join('\n\n');
}

export function recordingDoc({ exam, sections }) {
  const pool = POOLS[exam.pool];
  const parts = sections.filter((s) => s.audio);
  const out = [
    `# Kịch bản thu âm Listening: ${exam.code}`,
    '',
    `${exam.title}. Kho ${exam.pool}, trình độ khoảng ${pool.level}. Tài liệu này được sinh tự động từ \`scripts/placement-exams/exams/\`; lời thoại khớp từng chữ với transcript trong migration \`${pool.migration}\`.`,
    '',
    'Đọc **đúng nguyên văn**: câu hỏi và đáp án được chấm theo đúng lời này. Muốn sửa lời thì sửa file nguồn JSON rồi chạy lại generator, không sửa file này. Yêu cầu chung (định dạng, giọng, cách đặt tên, upload) xem [README](README.md).',
    '',
    `Tốc độ đọc: khoảng ${pool.wpm} từ mỗi phút, ngắt khoảng nửa giây đến một giây giữa các lượt thoại. Thời lượng bên dưới là **thời lượng khai báo trong seed** (đã tính cả các chỗ ngắt và khoảng im lặng đầu cuối); file thật phải nằm trong khoảng ±15% (kiểm bằng \`--audio\`).`,
    '',
    '| File (key trong bucket `media`) | Thời lượng khai báo | Giọng |',
    '| --- | --- | --- |',
    ...parts.map((s) => `| \`${s.audio.key}\` | ${s.audio.seconds} giây (${mmss(s.audio.seconds)}) | ${s.audio.voices} |`),
    '',
  ];
  for (const section of parts) {
    out.push('---', '', `## ${section.title}`, '',
      `- File: \`part${section.audio.number}.mp3\`, key \`${section.audio.key}\``,
      `- Thời lượng khai báo: ${section.audio.seconds} giây (${mmss(section.audio.seconds)}), cho phép lệch ±15%`,
      `- Giọng: ${section.audio.voices}`,
      '- Tên người nói in đậm chỉ để biết ai nói, **không đọc ra**. Không đọc số part hay hướng dẫn câu hỏi.',
      '- Đầu file để 1 giây im lặng, cuối file im lặng dưới 1 giây.',
      '', transcriptBlock(section.audio.transcript), '');
  }
  return out.join('\n');
}

function optionLine(question) {
  return question.options ? question.options.map((o) => `${o.optionKey}. ${o.content}`).join(' / ') : '';
}

export function reviewDoc({ exam, sections }) {
  const pool = POOLS[exam.pool];
  const out = [
    `# Bản duyệt nội dung: ${exam.code}`,
    '',
    `${exam.title}. Kho ${exam.pool} (trần band ${{ BAND_0_3: '3.0', BAND_3_5: '5.0', BAND_5_9: '9.0' }[exam.pool]}), trình độ mục tiêu ${pool.level}. Sinh tự động từ \`scripts/placement-exams/exams/\`; đối chiếu với thang độ khó trong [README](README.md).`,
    '',
    '**Người duyệt (IELTS):** ______  **Ngày:** ______  **Kết quả:** ☐ Đạt ☐ Cần sửa',
    '',
    'Kiểm: độ khó so với kho, đáp án đúng và duy nhất, câu hỏi không mơ hồ, đề Writing và Speaking phù hợp trình độ.',
    '',
  ];
  let number = 0;
  for (const section of sections) {
    out.push(`## ${section.title}`, '');
    if (section.instructions) out.push(`_${section.instructions}_`, '');
    if (section.audio) out.push(`Audio \`${section.audio.key}\`, ${section.audio.seconds} giây. Transcript:`, '', transcriptBlock(section.audio.transcript), '');
    if (section.passage) out.push(section.passage, '');
    if (section.table) out.push('Bảng dữ liệu:', '', '```', section.table, '```', '');
    for (const question of section.questions) {
      number += 1;
      out.push(`${number}. ${question.stem}`);
      if (question.options) out.push(`   - Lựa chọn: ${optionLine(question)}`);
      if (question.spec.type === 'FILL') out.push(`   - Đáp án chấp nhận: ${question.spec.accepted.join(' | ')}`);
      if (question.spec.type === 'CHOICE') out.push(`   - Đáp án: **${question.spec.correct}**`);
      if (question.spec.type === 'ESSAY') out.push(`   - Task ${question.spec.task}, tối thiểu ${question.spec.minWords} từ, passBand ${question.spec.passBand}${question.spec.chartFacts ? `\n   - chartFacts: ${question.spec.chartFacts}` : ''}`);
      if (question.spec.type === 'SPEAKING') out.push(`   - Thời lượng trả lời: ${question.spec.durationSeconds} giây`);
      if (question.spec.type === 'FILL' || question.spec.type === 'CHOICE') out.push(`   - Giải thích: ${question.explanation}`);
    }
    out.push('');
  }
  return out.join('\n');
}

export function readmeDoc(builtExams) {
  const rows = builtExams.map(({ exam, sections }) => {
    const audio = sections.filter((s) => s.audio);
    return `| \`${exam.code}\` | ${exam.pool} | ${POOLS[exam.pool].shape === 'full' ? 'đủ dài' : 'rút gọn'} | ${audio.length} | ${audio.reduce((sum, s) => sum + s.audio.seconds, 0)} giây | [Kịch bản thu âm](${exam.code}-recording.md) · [Bản duyệt](${exam.code}-review.md) |`;
  });
  const keys = builtExams.flatMap(({ sections }) => sections.filter((s) => s.audio).map((s) => s.audio));
  return [
    '# Đề placement theo kho: hướng dẫn thu âm, duyệt và upload',
    '',
    'Năm đề mới (cùng đề `PLACEMENT-4SKILLS` có sẵn ở kho `BAND_5_9`) giúp mỗi kho có hai đề. Các file trong thư mục này được sinh từ `scripts/placement-exams/exams/` bằng `node scripts/placement-exams/generate-placement-seed.mjs`; **sửa file nguồn JSON rồi chạy lại generator**, đừng sửa tay (`--check` sẽ báo lệch).',
    '',
    '## Các đề',
    '',
    '| Mã gói | Kho | Dạng | Số file audio | Tổng thời lượng audio | Tài liệu |',
    '| --- | --- | --- | --- | --- | --- |',
    ...rows,
    '',
    `Tổng cộng ${keys.length} file audio.`,
    '',
    '## Thang độ khó theo kho (hướng dẫn soạn đề, chưa hiệu chỉnh)',
    '',
    '| Kho | Trần band | Trình độ | Lời nói | Văn bản |',
    '| --- | --- | --- | --- | --- |',
    '| `BAND_0_3` | 3.0 | CEFR A1-A2 | Câu ngắn, chủ đề đời sống hằng ngày, nói chậm và rõ | Câu ngắn, từ vựng thông dụng |',
    '| `BAND_3_5` | 5.0 | CEFR A2-B1 | Chủ đề quen thuộc, tốc độ vừa | Đoạn ngắn, có vài từ mới suy ra được từ ngữ cảnh |',
    '| `BAND_5_9` | 9.0 | CEFR B1-C2 | Tốc độ tự nhiên | Văn bản học thuật, ý trừu tượng |',
    '',
    'Đây chỉ là thang hướng dẫn cho người soạn và người duyệt, không phải kết quả hiệu chỉnh: chưa có dữ liệu học viên thật. Hai tuần đầu nên theo dõi phân bố band theo kho.',
    '',
    '## Yêu cầu thu âm',
    '',
    '- Định dạng **mp3**, 44.1 kHz trở lên, **128 kbps trở lên**; mono hoặc stereo đều được.',
    '- Phòng yên tĩnh, không nhạc nền, không tạp âm. Đầu file 1 giây im lặng, cuối file dưới 1 giây.',
    '- Mỗi nhân vật một giọng khác nhau (xem cột Giọng trong kịch bản của từng đề). Độc thoại thì một người.',
    '- Tên người nói in đậm chỉ để biết ai nói, **không đọc ra**. Không đọc số part hay hướng dẫn câu hỏi: file chỉ chứa lời thoại.',
    '- Đọc **đúng nguyên văn** từng chữ; đánh vần từng chữ cái khi lời có đánh vần (ví dụ "H, A, R, P, E, R"), ngắt rõ. Số và giờ phải nghe rõ.',
    '- Tốc độ: theo số từ mỗi phút ghi trong kịch bản của từng đề. Thời lượng thật phải nằm trong ±15% thời lượng khai báo.',
    '',
    '## Upload',
    '',
    '1. **Quy ước đặt tên.** Mỗi đề có một thư mục riêng tên là mã gói viết thường; trong đó là các file `part1.mp3`, `part2.mp3`, ... theo thứ tự part trong kịch bản. Tên file nằm trong thư mục, nên 5 đề không bị trùng tên khi lưu cùng một chỗ. Thư mục và tên file **chính là phần cuối của key** trên bucket `media` (biến `CONTENT_MEDIA_BUCKET`): giữ nguyên thư mục khi upload (key đầy đủ có tiền tố `listening/placement/`). Key sai thì Listening báo `INVALID_MEDIA_REFERENCE` cho mọi học viên được giao đề đó. Chỉ chữ thường, không dấu cách, đuôi `.mp3`.',
    '',
    '   ```',
    '   placement-b03-a/part1.mp3   placement-b03-a/part2.mp3',
    '   placement-b03-b/part1.mp3   placement-b03-b/part2.mp3',
    '   placement-b35-a/part1.mp3   placement-b35-a/part2.mp3',
    '   placement-b35-b/part1.mp3   placement-b35-b/part2.mp3',
    '   placement-b59-b/part1.mp3   part2.mp3   part3.mp3   part4.mp3',
    '   ```',
    '',
    '   **Để file ở đâu:** trong `services/content-service/media/listening/placement/`, giữ nguyên thư mục con như trên (thư mục `media/` nằm trong `.gitignore`, không bị commit). Ví dụ `services/content-service/media/listening/placement/placement-b03-a/part1.mp3`. Thư mục con đã được tạo sẵn.',
    '',
    '2. **Upload** bằng script (cần `SUPABASE_URL`, `SUPABASE_SECRET_KEY`, và `CONTENT_MEDIA_BUCKET` nếu bucket không phải `media`; script tự đọc từ `.env` ở thư mục gốc nếu biến chưa được đặt, không in ra):',
    '',
    '   ```',
    '   powershell -File scripts/placement-exams/upload-placement-audio.ps1 -DryRun   # liệt kê file có và thiếu, không gọi Supabase',
    '   powershell -File scripts/placement-exams/upload-placement-audio.ps1           # upload; không ghi đè object đã có',
    '   ```',
    '',
    '   Script chỉ upload khi đủ 12 file, mỗi file dài hơn 1 KB. Thu lại một file đã upload thì thêm `-Overwrite`. **Không cần chèn gì vào database**: các migration `V138` đến `V140` đã ghi sẵn key vào `content_assets.media_reference`; Content ghép key với `CONTENT_MEDIA_BASE_URL` để ra URL, và database không bao giờ lưu URL hay file.',
    '',
    '3. Kiểm bằng generator (cần `CONTENT_MEDIA_BASE_URL`, là https của bucket):',
    '',
    '   ```',
    '   node scripts/placement-exams/generate-placement-seed.mjs --audio "$CONTENT_MEDIA_BASE_URL"',
    '   ```',
    '',
    '   Lệnh kiểm từng key: HTTP 200, content type `audio/*`, thời lượng trong ±15% khai báo. Lệnh thoát khác 0 nếu có file lỗi.',
    '4. Chỉ deploy migration Content khi lệnh ở bước 3 báo đủ 12 file đạt.',
    '',
    '| File khi thu (thư mục/tên file) | Key trên bucket | Thời lượng khai báo |',
    '| --- | --- | --- |',
    ...keys.map((a) => `| \`${a.key.replace('listening/placement/', '')}\` | \`${a.key}\` | ${a.seconds} giây |`),
    '',
    '## Duyệt nội dung (trước khi merge)',
    '',
    'Mỗi đề cần một người có chuyên môn IELTS ký duyệt bản duyệt (`<mã gói>-review.md`): độ khó đúng kho, đáp án đúng và duy nhất, không mơ hồ, đề Writing và Speaking phù hợp. Đề được xuất bản ngay khi migration chạy nên không có vòng duyệt sau deploy. Ghi người duyệt và ngày trong mô tả PR.',
    '',
  ].join('\n');
}

// ---------------------------------------------------------------------------------------------------------------
// Outputs

/** Every generated file as {absolute path: content}. */
export function buildOutputs(exams = loadExams()) {
  const built = exams.map(buildExam).sort((a, b) => a.exam.code.localeCompare(b.exam.code));
  const codes = built.map((b) => b.exam.code);
  if (new Set(codes).size !== codes.length) throw new Error('duplicate package code');
  const outputs = {};
  for (const [pool, settings] of Object.entries(POOLS)) {
    const group = built.filter((b) => b.exam.pool === pool);
    if (group.length) outputs[join(MIGRATION_DIR, settings.migration)] = migrationFile(pool, group);
  }
  for (const item of built) {
    outputs[join(DOCS_DIR, `${item.exam.code}-recording.md`)] = recordingDoc(item);
    outputs[join(DOCS_DIR, `${item.exam.code}-review.md`)] = reviewDoc(item);
  }
  outputs[join(DOCS_DIR, 'README.md')] = readmeDoc(built);
  return outputs;
}

export function audioFiles(exams = loadExams()) {
  return exams.map(buildExam).flatMap(({ sections }) => sections.filter((s) => s.audio).map((s) => s.audio));
}

/** Paths whose committed content differs from what the sources generate. */
export function differences(outputs) {
  return Object.entries(outputs)
    .filter(([path, content]) => !existsSync(path) || readFileSync(path, 'utf8').replaceAll('\r\n', '\n') !== content)
    .map(([path]) => path);
}

// ---------------------------------------------------------------------------------------------------------------
// Audio check

/** Duration of an MP3 in seconds from its first frame and, when present, the Xing or Info frame count. */
export function mp3Seconds(buffer) {
  let position = 0;
  if (buffer.length > 10 && buffer.toString('latin1', 0, 3) === 'ID3') {
    position = 10 + (((buffer[6] & 0x7f) << 21) | ((buffer[7] & 0x7f) << 14) | ((buffer[8] & 0x7f) << 7) | (buffer[9] & 0x7f));
  }
  while (position + 4 < buffer.length && !(buffer[position] === 0xff && (buffer[position + 1] & 0xe0) === 0xe0)) position += 1;
  if (position + 4 >= buffer.length) return null;
  const versionBits = (buffer[position + 1] >> 3) & 3;
  const layerBits = (buffer[position + 1] >> 1) & 3;
  if (versionBits === 1 || layerBits !== 1) return null;
  const mpeg1 = versionBits === 3;
  const bitrates = mpeg1
    ? [0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0]
    : [0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0];
  const sampleRate = { 3: [44100, 48000, 32000], 2: [22050, 24000, 16000], 0: [11025, 12000, 8000] }[versionBits][(buffer[position + 2] >> 2) & 3];
  const bitrate = bitrates[(buffer[position + 2] >> 4) & 15] * 1000;
  if (!bitrate || !sampleRate) return null;
  const mono = ((buffer[position + 3] >> 6) & 3) === 3;
  const sideInfo = mpeg1 ? (mono ? 17 : 32) : (mono ? 9 : 17);
  const tagAt = position + 4 + sideInfo;
  const tag = buffer.toString('latin1', tagAt, tagAt + 4);
  if ((tag === 'Xing' || tag === 'Info') && (buffer.readUInt32BE(tagAt + 4) & 1)) {
    return (buffer.readUInt32BE(tagAt + 8) * (mpeg1 ? 1152 : 576)) / sampleRate;
  }
  return ((buffer.length - position) * 8) / bitrate;
}

export async function checkAudio(baseUrl, files = audioFiles(), fetchFile = fetch) {
  const problems = [];
  for (const file of files) {
    const url = `${baseUrl.replace(/\/+$/, '')}/${file.key}`;
    try {
      const response = await fetchFile(url);
      if (response.status !== 200) { problems.push(`${file.key}: HTTP ${response.status}`); continue; }
      const type = response.headers.get('content-type') ?? '';
      if (!type.startsWith('audio/')) { problems.push(`${file.key}: content type ${type || 'missing'}`); continue; }
      const seconds = mp3Seconds(Buffer.from(await response.arrayBuffer()));
      if (seconds === null) { problems.push(`${file.key}: not a readable mp3`); continue; }
      if (Math.abs(seconds - file.seconds) / file.seconds > 0.15) {
        problems.push(`${file.key}: ${Math.round(seconds)} s recorded, ${file.seconds} s declared (limit 15%)`);
      }
    } catch (error) {
      problems.push(`${file.key}: ${error.message}`);
    }
  }
  return problems;
}

// ---------------------------------------------------------------------------------------------------------------

async function main(argv) {
  if (argv[0] === '--keys') {
    // One bucket key per line, for the upload script.
    console.log(audioFiles().map((file) => file.key).join('\n'));
    return;
  }
  if (argv[0] === '--audio') {
    if (!argv[1]) throw new Error('--audio needs the media base URL');
    const files = audioFiles();
    const problems = await checkAudio(argv[1], files);
    if (problems.length) { console.error(problems.join('\n')); console.error(`${problems.length} of ${files.length} recordings failed`); process.exit(1); }
    console.log(`${files.length} recordings reachable and within 15% of the declared duration`);
    return;
  }
  const outputs = buildOutputs();
  if (argv[0] === '--check') {
    const stale = differences(outputs);
    if (stale.length) { console.error(`Out of date, run the generator:\n${stale.join('\n')}`); process.exit(1); }
    console.log(`${Object.keys(outputs).length} generated files are up to date`);
    return;
  }
  mkdirSync(DOCS_DIR, { recursive: true });
  for (const [path, content] of Object.entries(outputs)) writeFileSync(path, content, 'utf8');
  console.log(`wrote ${Object.keys(outputs).length} files`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  main(process.argv.slice(2)).catch((error) => { console.error(error.message); process.exit(1); });
}
