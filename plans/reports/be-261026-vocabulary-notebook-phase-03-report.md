# Vocabulary catalog enrichment report

Date: 2026-10-26
Branch: `feat/vocabulary-notebook`

## Delivered

- Added dictionary metadata, library-owned topics, word sets, translated examples, collocations, thesaurus relations, common mistakes, and author endpoints.
- Public catalog GET routes remain method-scoped in Gateway and Library. `/search`, `/senses`, writes, admin routes, and notebook routes remain authenticated.
- Existing detail response fields are preserved. New metadata is batch-loaded by sense ID, one query per child table. Browse, topic/set list counts, filters, ordering and pagination execute in SQL.
- IELTS band ranges are derived from CEFR in the domain. Topic taxonomy belongs to Library and is independent of Content curriculum topics.
- Topic and set images reuse the existing `flashcards` bucket and image upload route.

## Final HTTP contract

Common responses use the existing `ErrorResponse`. `400` means invalid query/body/UUID/enum/limit; public catalog resources return `404` when missing or inactive; admin paths return `401` without valid authentication and `403` to roles other than `CONTENT_AUTHOR` or `ADMIN`. Admin writes return `404` for missing resources and `400` for invalid fields or limits. No submitted text is logged.

### Public catalog

All paths below are public GETs at Gateway and Library and return catalog data only. List sizes are at most 50; public responses are cacheable for five minutes.

| Path | Query | Success response |
| --- | --- | --- |
| `GET /api/content/vocabulary` | `q` optional, ≤100 chars; `lang=EN\|VI` default `EN`; `cefr=A1..C2`; `topicId`; `page` default 0; `size` default 20, 1–50 | `{items,page,size,total}`. Active items and senses; `VI` searches Vietnamese meaning. Existing fields remain; item and sense enrichment includes UK/US pronunciation/audio, frequency/AWL/word-family, CEFR and derived band range, topic, notes, collocations, synonyms, antonyms, mistakes, translated examples and counts. |
| `GET /api/content/vocabulary/{id}` | UUID | Existing item response plus the enrichment above; active senses only. Missing/inactive item: `404`. |
| `GET /api/content/vocabulary/suggestions` | required `q`, 1–50 chars | Up to 8 active `{id,lemma}` prefix matches. |
| `GET /api/content/vocabulary/topics` | — | Active `{id,slug,name,nameVi,coverKey,coverUrl,sortOrder,wordCount}` list. |
| `GET /api/content/vocabulary/sets` | optional `kind=BAND\|TOPIC`; `page`; `size` 1–50 | `{items,page,size,total}` with set metadata, derived band range, cover URL and counts. |
| `GET /api/content/vocabulary/sets/{id}` | UUID | Active set metadata, `bandRange`, `coverUrl`, `wordCount`, topic summaries. Missing/inactive: `404`. |
| `GET /api/content/vocabulary/sets/{id}/words` | optional `topicId`; `page`; `size` 1–50 | `{items,page,size,total}` of active members; missing/inactive set or parent topic: `404`. |
| `GET /api/content/vocabulary/mindmap` | exactly one of `topicId` or `itemId` | `{nodes,edges}`, at most 80 nodes. Invalid combination: `400`; missing/inactive source: `404`. |

`/api/content/vocabulary/search` and `/senses` remain authenticated. Malformed catalog UUIDs and invalid query limits return `400`.

### Authenticated author API

All routes below require `CONTENT_AUTHOR` or `ADMIN`.

| Method and path | Request | Success |
| --- | --- | --- |
| `PUT /api/content/admin/vocabulary/items/{id}` | JSON fields `ipaUk`, `ipaUs`, `audioUkReference`, `audioUsReference`, `frequencyRank`, `academicWordList`, `wordFamily` | `200`, updated item |
| `PUT /api/content/admin/vocabulary/senses/{id}` | `cefrLevel`, `topicId`, `register`, `grammarNote`, `usageNote`, `exampleTranslation` | `200`, updated sense |
| `PUT /api/content/admin/vocabulary/senses/{id}/collocations` | Array `{phrase,meaningVi,example}`, ≤30 | `200`, replacement list |
| `PUT /api/content/admin/vocabulary/senses/{id}/relations` | Array `{relatedLemma,relatedItemId,kind}`, kind `SYNONYM\|ANTONYM\|RELATED`, ≤30 | `200`, replacement list |
| `PUT /api/content/admin/vocabulary/senses/{id}/common-mistakes` | Array `{wrongUsage,correctUsage,explanation}`, ≤30 | `200`, replacement list |
| `PUT /api/content/admin/vocabulary/senses/{id}/examples` | Array `{sentence,translationVi}`, ≤30 | `200`, replacement list |
| `GET /api/content/admin/vocabulary/topics` | — | `200`, topic list |
| `POST /api/content/admin/vocabulary/topics` | `{slug,name,nameVi,coverKey,sortOrder,status}` | `201`, topic |
| `PUT /api/content/admin/vocabulary/topics/{id}` | Same fields | `200`, topic |
| `DELETE /api/content/admin/vocabulary/topics/{id}` | — | `204`, deactivated |
| `GET /api/content/admin/vocabulary/sets` | — | `200`, set list |
| `POST /api/content/admin/vocabulary/sets` | `{title,description,kind,cefrLevel,topicId,coverKey,sortOrder,status}`; BAND needs CEFR, TOPIC needs topicId | `201`, set |
| `PUT /api/content/admin/vocabulary/sets/{id}` | Same fields | `200`, set |
| `DELETE /api/content/admin/vocabulary/sets/{id}` | — | `204`, deactivated |
| `PUT /api/content/admin/vocabulary/sets/{id}/items` | Ordered unique sense UUID array, ≤500 | `204` |

Invalid requests return `400`; missing IDs return `404`; customer-role access returns `403`.

## Schema and seed

- V19 is additive: item columns `ipa_uk`, `ipa_us`, `audio_uk_reference`, `audio_us_reference`, nullable `frequency_rank`, `academic_word_list`, and `word_family`; sense columns `cefr_level`, library `topic_id`, `register`, `grammar_note`, `usage_note`, and `example_translation`; plus `vocabulary_topics`, `vocabulary_collocations`, `vocabulary_relations`, `vocabulary_common_mistakes`, `vocabulary_examples`, `vocabulary_sets`, and `vocabulary_set_items` with indexes and constraints.
- V20 adds six topics, six CEFR band sets and six topic sets; it enriches 16 existing V4/V15 lemmas only. Each gets two translated examples, collocations, a relation and a common-mistake row. `frequency_rank` stays null. Only `data`, `method`, and `research` are marked as Coxhead AWL terms. The migration and README state that demo content needs IELTS examiner review.
- CEFR mapping: A1 1.0–2.5, A2 3.0–3.5, B1 4.0–5.0, B2 5.5–6.5, C1 7.0–8.0, C2 8.5–9.0. No band column is stored.

## Verification

- `mvn -q -pl services/library-service -am test`: exit 0; 208 tests, 0 failures, 0 errors, 0 skipped. Docker/Testcontainers ran; Flyway applied through V20. Coverage includes public/security validation, catalog SQL filters and batch-enriched detail, migration/seed, mindmap cap, set reads and large page offsets.
- `mvn -q -pl infra/api-gateway test`: exit 0; 20 tests, 0 failures, 0 errors, 0 skipped. Covers public catalog GET paths and unauthenticated `/search`, `/senses`, and admin paths.
- `git diff --check`: clean.
- `graphify update .`: Java extraction completed. Graphify reports SQL AST extraction unavailable because `tree_sitter_sql` is not installed.

## Deviations and open questions

- No scope deviation. Dynamic SQL predicates are used for optional filters because PostgreSQL cannot infer a nullable placeholder's type in `:value IS NULL` predicates; admin replacement writes lock the parent before updating bounded lists.
- Before production, an IELTS examiner must review demo-authored content. A licensed/approved COCA frequency source is still needed before frequency ranks can be populated.

Status: DONE
