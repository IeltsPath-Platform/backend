// Run with: node --test scripts/placement-exams
import assert from 'node:assert/strict';
import test from 'node:test';
import {
  audioFiles, buildExam, buildOutputs, checkAudio, differences, loadExams, mp3Seconds, normalizeQuestion, POOLS, uuid5,
} from './generate-placement-seed.mjs';

const exams = loadExams();

test('five exams: two shortened per low pool and one full-length exam in the high pool', () => {
  const byPool = Object.groupBy(exams, (exam) => exam.pool);
  assert.deepEqual(Object.keys(byPool).sort(), Object.keys(POOLS).sort());
  assert.equal(byPool.BAND_0_3.length, 2);
  assert.equal(byPool.BAND_3_5.length, 2);
  assert.equal(byPool.BAND_5_9.length, 1);
  assert.deepEqual(exams.map((exam) => exam.code).sort(),
    ['PLACEMENT-B03-A', 'PLACEMENT-B03-B', 'PLACEMENT-B35-A', 'PLACEMENT-B35-B', 'PLACEMENT-B59-B']);
});

test('twelve recordings with the key pattern of the package code', () => {
  const files = audioFiles(exams);
  assert.equal(files.length, 12);
  for (const file of files) {
    assert.match(file.key, /^listening\/placement\/placement-b(03|35|59)-[a-z]\/part[1-4]\.mp3$/);
    assert.ok(file.seconds > 0);
  }
  assert.equal(new Set(files.map((file) => file.key)).size, 12);
});

test('output is deterministic', () => {
  assert.deepEqual(buildOutputs(exams), buildOutputs(loadExams()));
  assert.equal(uuid5('a|b'), uuid5('a|b'));
  assert.notEqual(uuid5('a|b'), uuid5('a|c'));
});

test('the committed migrations and documents are what the sources generate', () => {
  assert.deepEqual(differences(buildOutputs(exams)), []);
});

test('a changed source is reported as out of date until regenerated', () => {
  const changed = structuredClone(exams);
  changed[0].listening[0].transcript += '\nCaller: One more line.';
  assert.ok(differences(buildOutputs(changed)).length > 0);
});

test('every question is published, unique and used by one package only', () => {
  const sql = Object.entries(buildOutputs(exams)).filter(([path]) => path.endsWith('.sql')).map(([, content]) => content).join('\n');
  const ids = [...sql.matchAll(/INSERT INTO questions[^;]*;/g)].flatMap((m) => [...m[0].matchAll(/^\s+\('([0-9a-f-]{36})'/gm)].map((x) => x[1]));
  // 4 shortened exams x (20 + 20 + 1 + 2) and one full-length exam x (40 + 40 + 2 + 8)
  assert.equal(ids.length, 4 * 43 + 90);
  assert.equal(new Set(ids).size, ids.length);
  assert.ok(!/'LEARNING'|'EXAM'/.test(sql));
});

test('shape: shortened exams have 20 + 20 questions and the full exam 40 + 40', () => {
  for (const exam of exams) {
    const { sections } = buildExam(exam);
    const total = (kind) => sections.filter((s) => s.kind === kind).reduce((sum, s) => sum + s.questions.length, 0);
    const full = exam.pool === 'BAND_5_9';
    assert.equal(total('LISTENING'), full ? 40 : 20, exam.code);
    assert.equal(total('READING'), full ? 40 : 20, exam.code);
    assert.equal(total('WRITING'), full ? 2 : 1, exam.code);
    assert.equal(total('SPEAKING'), full ? 8 : 2, exam.code);
  }
});

test('Task 1 carries its table and chart facts so the grader can check the figures', () => {
  for (const exam of exams) {
    for (const section of buildExam(exam).sections.filter((s) => s.kind === 'WRITING')) {
      const spec = section.questions[0].spec;
      if (spec.task === 'TASK_1') {
        assert.ok(section.table && spec.chartFacts, exam.code);
      }
    }
  }
});

test('invalid sources are rejected', () => {
  const exam = structuredClone(exams[0]);
  exam.listening[0].questions[0].why = '';
  assert.throws(() => buildExam(exam), /no explanation/);
  const short = structuredClone(exams[0]);
  short.reading[0].questions.pop();
  assert.throws(() => buildExam(short), /2 passages of 10/);
  assert.throws(() => normalizeQuestion(exams[0], 'READING', { q: 'q', why: 'w', options: ['a', 'b'], answer: 'C' }), /not one of its options/);
  assert.throws(() => normalizeQuestion(exams[0], 'READING', { q: 'q', why: 'w', fill: [] }), /accepted answers/);
  const wrongCode = structuredClone(exams[0]);
  wrongCode.code = 'PLACEMENT-B35-A';
  assert.throws(() => buildExam(wrongCode), /does not match the pool/);
});

test('mp3 duration from a constant bit rate stream', () => {
  const frame = Buffer.from([0xff, 0xfb, 0x90, 0x00]); // MPEG 1 layer III, 128 kbps, 44.1 kHz, stereo
  const stream = Buffer.concat([frame, Buffer.alloc(160000)]);
  assert.ok(Math.abs(mp3Seconds(stream) - 10) < 0.05);
  assert.equal(mp3Seconds(Buffer.from('not audio')), null);
});

test('audio check flags a missing file, a wrong content type and a wrong duration', async () => {
  const files = [{ key: 'a.mp3', seconds: 10 }, { key: 'b.mp3', seconds: 10 }, { key: 'c.mp3', seconds: 10 }, { key: 'd.mp3', seconds: 10 }];
  const mp3 = (seconds) => Buffer.concat([Buffer.from([0xff, 0xfb, 0x90, 0x00]), Buffer.alloc(16000 * seconds)]);
  const bodies = { 'a.mp3': [200, 'audio/mpeg', mp3(10)], 'b.mp3': [404, 'text/html', Buffer.alloc(1)],
    'c.mp3': [200, 'text/html', Buffer.alloc(1)], 'd.mp3': [200, 'audio/mpeg', mp3(20)] };
  const fakeFetch = async (url) => {
    const [status, type, body] = bodies[url.split('/').pop()];
    return { status, headers: { get: () => type }, arrayBuffer: async () => body };
  };
  const problems = await checkAudio('https://media.example.test/', files, fakeFetch);
  assert.equal(problems.length, 3);
  assert.match(problems[0], /b\.mp3: HTTP 404/);
  assert.match(problems[1], /c\.mp3: content type/);
  assert.match(problems[2], /d\.mp3: 20 s recorded/);
});
