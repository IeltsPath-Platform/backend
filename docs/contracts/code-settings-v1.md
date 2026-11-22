# Generated code settings API

These endpoints are available to `CONTENT_AUTHOR` through the API Gateway.

## List connected settings

`GET /api/content/admin/code-settings` returns the configurations connected to authoring flows. Each item has this shape:

```json
{
  "kind": "LESSON",
  "applied": true,
  "pattern": "{TOPIC}_L{SEQ:2}",
  "startNumber": 1,
  "editable": true
}
```

Connected kinds are `COURSE`, `TOPIC`, `LESSON`, `PACKAGE`, `KNOWLEDGE_POINT` and `ERROR_TYPE`.

## Update a setting

`PUT /api/content/admin/code-settings/{kind}` accepts `{ "applied": true, "pattern": "...", "startNumber": 1,
"editable": true }` and returns the updated setting shape above. Unsupported kinds and invalid patterns return a
Vietnamese error response.

## Preview a code

`GET /api/content/admin/code-suggestion` accepts these optional query parameters:

| Parameter | Used by | Meaning |
| --- | --- | --- |
| `kind` | All | Connected kind: `COURSE`, `TOPIC`, `LESSON`, `PACKAGE`, `KNOWLEDGE_POINT` or `ERROR_TYPE`; defaults to `COURSE` |
| `band` | `COURSE` | Course band, such as `6.5` |
| `courseId` | `TOPIC`, `PACKAGE` | Course UUID. For a package, this is the first course source checked. |
| `skill` | `TOPIC` | Topic skill |
| `topicId` | `LESSON`, `PACKAGE` | Parent topic UUID for a lesson, or a topic from which a package course may be inferred |
| `lessonId` | `PACKAGE` | Lesson UUID from which the containing topic and course may be inferred |
| `packageType` | `PACKAGE` | Package type, such as `PRACTICE_SET` or `MOCK_TEST` |
| `kpKind` | `KNOWLEDGE_POINT` | Knowledge point kind, such as `GRAMMAR` or `VOCABULARY` |
| `skill` | `TOPIC`, `KNOWLEDGE_POINT` | Skill used to determine the code prefix |
| `level` | `KNOWLEDGE_POINT` | Code level: `55`, `60`, `65` or `70` |
| `name` | `KNOWLEDGE_POINT`, `ERROR_TYPE` | Name used by `{NAME}` |

For packages, the service resolves a course in this order: `courseId`, the course of `topicId`, then the course of the
topic containing `lessonId`. Without a course it previews the fixed pattern `PKG_{TYPE}{SEQ:3}` and uses a separate
counter scope for each package type. Lesson suggestions use the parent topic code and a counter scoped to that topic.
Previewing a code does not allocate a counter value.

The response shape is:

```json
{ "code": "COURSE_001_PS01", "applied": true, "editable": true }
```

For knowledge points, `{PREFIX}` is resolved from `kpKind` and `skill`, `{LEVEL}` from `level`, and `{NAME}` from `name`.
For error types the default pattern `{NAME}` turns the name into upper-case words joined by `_` (accents removed, `Đ`
read as `D`). The result starts with a letter (`E_` is put in front of a name that starts with a digit), has at most 60
characters (cut at a `_` when possible), and gets `_2`, `_3`, ... when the code is already taken. A name with no letters
or digits gives an empty `code`. `POST /api/content/error-types` accepts a request without `code` when generation is
enabled; with generation off, a missing `code` is refused.
When required context is missing or the rendered value does not satisfy the knowledge point code rules, `code` is an empty string. `applied` reports whether generation is enabled and
`editable` reports whether the author may change the suggestion. Errors use the common error response with a
Vietnamese `message` and stable `details.code` when the client needs to handle a specific case.
