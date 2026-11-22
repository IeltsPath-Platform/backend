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

Connected kinds are `COURSE`, `TOPIC`, `LESSON` and `PACKAGE`.

## Update a setting

`PUT /api/content/admin/code-settings/{kind}` accepts `{ "applied": true, "pattern": "...", "startNumber": 1,
"editable": true }` and returns the updated setting shape above. Unsupported kinds and invalid patterns return a
Vietnamese error response.

## Preview a code

`GET /api/content/admin/code-suggestion` accepts these optional query parameters:

| Parameter | Used by | Meaning |
| --- | --- | --- |
| `kind` | All | `COURSE`, `TOPIC`, `LESSON` or `PACKAGE`; defaults to `COURSE` |
| `band` | `COURSE` | Course band, such as `6.5` |
| `courseId` | `TOPIC`, `PACKAGE` | Course UUID. For a package, this is the first course source checked. |
| `skill` | `TOPIC` | Topic skill |
| `topicId` | `LESSON`, `PACKAGE` | Parent topic UUID for a lesson, or a topic from which a package course may be inferred |
| `lessonId` | `PACKAGE` | Lesson UUID from which the containing topic and course may be inferred |
| `packageType` | `PACKAGE` | Package type, such as `PRACTICE_SET` or `MOCK_TEST` |

For packages, the service resolves a course in this order: `courseId`, the course of `topicId`, then the course of the
topic containing `lessonId`. Without a course it previews the fixed pattern `PKG_{TYPE}{SEQ:3}` and uses a separate
counter scope for each package type. Lesson suggestions use the parent topic code and a counter scoped to that topic.
Previewing a code does not allocate a counter value.

The response shape is:

```json
{ "code": "COURSE_001_PS01", "applied": true, "editable": true }
```

When required context is missing, `code` is an empty string. `applied` reports whether generation is enabled and
`editable` reports whether the author may change the suggestion. Errors use the common error response with a
Vietnamese `message` and stable `details.code` when the client needs to handle a specific case.
