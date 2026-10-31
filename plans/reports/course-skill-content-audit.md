# Course skill content audit

The content database must be audited before the one-skill paywall is enabled. Run [course-skill-content-audit.sql](./course-skill-content-audit.sql) against a database migrated through content migration V88. It reports course topics without a skill, knowledge points with a different skill, and published lessons that teach multiple skills or conflict with their topic's skill.

## Seed data finding

The checked-in demo seed `V23__seed_multi_skill_topic.sql` creates the `TREES_MULTI_SKILL` topic in IELTS 5.5 with no topic skill. It attaches two READING knowledge points, one LISTENING knowledge point, and one WRITING knowledge point to each of two published lessons (`T1` and `T2`). Those lessons also include reading, listening, and writing exercises. The V84 trap knowledge point `TRAP_KEYWORD_MATCH_ONLY` is also attached to a course topic whose skill is null. The topic therefore appears in all three audit sections. Its content owner must split or reassign the topic, knowledge points, lessons, and exercises before the one-skill paywall ships.

The audit is diagnostic only. It does not update or seed content.
