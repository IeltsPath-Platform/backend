// Merges "Vietnamese|English" lines into a word list (case matters).
// Run: node add-words.mjs <lines.txt> [words-<group>.json]
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
const file = new URL(`./${process.argv[3] ?? 'words.json'}`, import.meta.url)
const words = existsSync(file) ? JSON.parse(readFileSync(file, 'utf8').replace(/^[^{]+/, '')) : {}
for (const line of readFileSync(process.argv[2], 'utf8').split(/\r?\n/)) {
  const at = line.indexOf('|')
  if (at > 0) words[line.slice(0, at).replace(/^[^\p{L}\p{N}\p{P}\p{S}]+/u, '').trim()] = line.slice(at + 1).trim()
}
writeFileSync(file, JSON.stringify(words, null, 1) + '\n')
console.log(Object.keys(words).length, 'words')