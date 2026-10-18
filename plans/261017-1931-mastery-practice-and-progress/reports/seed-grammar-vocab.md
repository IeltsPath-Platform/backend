# Proposed Grammar and Vocabulary knowledge points

**Review status: approved by the project owner on 2026-10-18.** Seeded by Content migration
`V45__seed_grammar_vocabulary_knowledge_points.sql` (commit 649f623) with every point `INACTIVE`: 13 points and 13
prerequisite edges, checked on PostgreSQL by `GrammarVocabularySeedMigrationTest` (counts, CEFR, strategy targets, no
cycle). Points stay hidden from learners until Content activates them.

The proposed entries use `skill = ALL`; their CEFR labels are initial suggestions and may be revised by the reviewer.
`scopeIn` and `scopeOut` describe the boundary of each learning point.

## Grammar

| Code | Name | CEFR | Can do | Scope in | Scope out |
|---|---|---|---|---|---|
| `G55_COMPLEX_SENTENCES` | Complex sentences | B1 | Combine an independent clause with a subordinate clause to express a clear relationship. | Basic cause, contrast, time, and condition clauses. | Multi-clause punctuation edge cases and highly compressed academic syntax. |
| `G55_RELATIVE_CLAUSES` | Relative clauses | B1 | Add defining information with an appropriate relative pronoun. | Defining clauses with who, which, that, where, and whose. | Reduced clauses and complex non-defining punctuation. |
| `G55_PASSIVE_VOICE` | Passive voice | B1 | Describe a process or result while keeping the affected object in focus. | Present and past simple passive; common process instructions. | Perfect continuous passive and stylistic voice shifts within long texts. |
| `G55_COMPARISON` | Comparisons | B1 | Compare two or more people, places, quantities, or changes accurately. | Comparative and superlative forms; `as ... as`; `more/less`. | Advanced hedging and multi-variable statistical comparisons. |
| `G60_PERFECT_TENSES` | Perfect tenses | B2 | Relate a past event or trend to a reference time using perfect forms. | Present perfect and past perfect in common contexts. | Rare aspect combinations and literary tense shifts. |
| `G55_ARTICLES` | Articles and countability | B1 | Choose a, an, the, or no article for common count and uncountable nouns. | First/known mention, general plural nouns, and common uncountable nouns. | Fixed institutional names and advanced generic-reference exceptions. |
| `G55_SUBJECT_VERB_AGREEMENT` | Subject–verb agreement | B1 | Match a verb to its grammatical subject in common sentence patterns. | Singular/plural subjects, collective quantifiers, and intervening phrases. | Inversion, complex notional agreement, and rare collective-noun variants. |
| `G55_LINKING_WORDS` | Linking words | B1 | Connect ideas with a linker that expresses the intended logical relationship. | Addition, contrast, cause, result, example, and sequence. | Cohesion across long arguments and subtle register differences. |

## Vocabulary

| Code | Name | CEFR | Can do | Scope in | Scope out |
|---|---|---|---|---|---|
| `V55_ACADEMIC_COLLOCATIONS` | Academic collocations | B1 | Use frequent academic word combinations naturally and precisely. | Common verb–noun and adjective–noun pairs used across topics. | Specialist terminology and low-frequency idioms. |
| `V60_DATA_TREND_WORDS` | Describing data trends | B2 | Describe direction, size, stability, and comparison in a chart. | Common verbs, nouns, and modifiers for rises, falls, and plateaus. | Forecasting, causal claims, and complex uncertainty descriptions. |
| `V55_PARAPHRASE_WORDS` | Paraphrase vocabulary | B1 | Replace common words with context-appropriate alternatives without changing meaning. | Frequent topic vocabulary and safe grammatical transformations. | Forced synonym substitution and changes to claims or degree. |
| `V55_WORD_FORMS` | Word forms | B1 | Select a noun, verb, adjective, or adverb form that fits a sentence. | Common derivational families and predictable suffixes. | Rare derivations and spelling exceptions not supported by a pattern. |
| `V55_COMMON_TOPICS` | Common IELTS topics | B1 | Understand and use high-frequency vocabulary about everyday academic topics. | Education, work, health, environment, transport, and technology. | Subject-specific terminology and memorised essay templates. |

## Proposed prerequisite relationships

The strategy knowledge point is the dependent node; the Grammar or Vocabulary knowledge point is its prerequisite.
The following strategy codes are illustrative and must be checked against the active knowledge tree before a seed is
approved.

| Prerequisite | Dependent strategy | Reason |
|---|---|---|
| `G55_COMPLEX_SENTENCES` | `W70_COMPLEX_STRUCTURES` | Supports expressing reasons, conditions, and contrasts in an argument. |
| `G55_RELATIVE_CLAUSES` | `W70_IDEA_DEVELOPMENT` | Supports adding precise, relevant detail to claims. |
| `G55_PASSIVE_VOICE` | `W60_T1_KEY_FEATURES` | Supports describing process stages and actions. |
| `G55_COMPARISON` | `W60_T1_KEY_FEATURES` | Supports comparing categories and values. |
| `G60_PERFECT_TENSES` | `W60_T1_KEY_FEATURES` | Supports describing changes over a time range. |
| `G55_ARTICLES` | `W65_DISCUSS_BOTH_VIEWS` | Supports accurate noun phrases in written responses. |
| `G55_SUBJECT_VERB_AGREEMENT` | `W65_DISCUSS_BOTH_VIEWS` | Supports accurate sentence construction. |
| `G55_LINKING_WORDS` | `W60_PARAGRAPH_COHESION` | Supports clear logical progression between ideas. |
| `V60_DATA_TREND_WORDS` | `W60_T1_KEY_FEATURES` | Supports accurate descriptions of chart movement. |
| `V55_ACADEMIC_COLLOCATIONS` | `W70_LEXICAL_PRECISION` | Supports natural academic phrasing. |
| `V55_PARAPHRASE_WORDS` | `W70_LEXICAL_PRECISION` | Supports accurate paraphrase of the prompt. |
| `V55_WORD_FORMS` | `W70_LEXICAL_PRECISION` | Supports grammatical word choice. |
| `V55_COMMON_TOPICS` | `W70_IDEA_DEVELOPMENT` | Supports precise expression across frequent prompt themes. |

These target strategy codes appear in existing Content seed migrations. Before any migration is written, reviewers
should confirm the wording, CEFR labels, target bands, code names, and pedagogical relationship for each edge. The
eventual seed migration will be separate and all proposed points will remain inactive until approval.
