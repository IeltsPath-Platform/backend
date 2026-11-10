# Notification learning-mail queues

Notification declares these durable queues in `LearningMailTopology` when the learning-mail gate is enabled. Each queue has a `.retry` queue and `.dlq` dead-letter queue, configured through `RetryTopology` with a 30-second retry delay and five attempts. All are consumers of existing event streams; adding these bindings does not change the source event or its other consumers.

| Queue | Source exchange and routing key | Purpose |
|---|---|---|
| `notification.user-contact.v1` | `user.events` / `user.email-verification-state.v1` | Keep the local recipient contact current; latest `occurredAt` wins. |
| `notification.learning-grading-completed.v1` | `assessment.events` / `assessment.grading-completed.v1` | Learner completion email. |
| `notification.learning-grading-overdue.v1` | `assessment.events` / `assessment.grading-overdue.v1` | Learner overdue email. |
| `notification.learning-grading-due-soon.v1` | `assessment.events` / `assessment.grading-due-soon.v1` | Examiner due-soon email. |
| `notification.learning-grading-requested.v1` | `assessment.events` / `assessment.grading-requested.v1` | Queue new examiner jobs for an hourly digest. |
| `notification.learning-assessment-completed.v1` | `assessment.events` / `AssessmentCompleted.v2` | Learner placement/mock result email. |
| `notification.learning-subscription-granted.v1` | `access.events` / `access.subscription-granted.v1` | Premium grant email using the locally stored contact. |

The mail pipeline deduplicates by `(eventId, template)`. Learning emails honor the single `learning` mail preference and the per-user daily cap; billing emails are exempt from both. Sensitive event data such as tokens and mail bodies is not logged.
