---
title: "Thả cảm xúc cho bình luận (community), dùng chung FE và mobile"
description: "Thêm cảm xúc LIKE/LOVE/HELPFUL/ACCURATE cho bình luận trong community-service; FE và mobile dùng cùng API"
status: completed
priority: P2
branch: "feat/comment-reactions"
tags: [backend, community-service, comments, reactions]
blockedBy: []
blocks: []
created: "2026-11-06T17:50:00.000Z"
createdBy: "ck:plan"
source: skill
---

# Thả cảm xúc cho bình luận

Người đọc: dev backend (community-service). Một phase, làm một lần, FE và mobile dùng chung API.

## 1. Quyết định (07/11/2026)

- Gắn vào **bình luận có sẵn** của community (cả bình luận gốc và trả lời, vì cùng bảng `comments`). Không làm hệ thống bình luận mới.
- Dùng **4 loại cảm xúc hiện có**: `LIKE`, `LOVE`, `HELPFUL`, `ACCURATE`. Không thêm emoji mới.
- Mỗi người một cảm xúc trên một bình luận, đúng như bài viết: chọn loại khác thì **thay thế**, chọn lại cùng loại không đổi gì.

## 2. Hiện trạng (đã đọc code)

- Cảm xúc chỉ có ở bài viết: bảng `post_reactions` (khóa chính `post_id, user_id`), API `PUT/DELETE /api/community/posts/{postId}/reactions/{type}` (`CommunityController` dòng 301–310), use case `AddReactionUseCase`/`RemoveReactionUseCase`, `ReactionRepositoryAdapter.add` dùng `PostReactionJpaRepository.insertOrReplace`.
- Bài viết trả `reactionCounts` (map loại → số) và `myReaction` (từ `PostInteractions`). Mobile (`src/features/community/api.ts`) và FE (`features/community/api/communityApi.ts`) đã có `react`/`removeReaction` cho bài viết.
- Bình luận: bảng `comments` (V1), `CommentResult`/`CommentResponse` hiện **không** có cảm xúc. `CommunityQueriesUseCase.enrichComments(List<CommentResult>)` **không nhận người xem**, nên chưa tính được `myReaction`.
- Migration community-service hiện dừng ở `V10`. Dùng `V11` (kiểm tra `main` và PR đang mở xem còn trống không).

## 3. Migration `V11__comment_reactions.sql`

```sql
CREATE TABLE comment_reactions (
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    reaction_type VARCHAR(20) NOT NULL CHECK (reaction_type IN ('LIKE','LOVE','HELPFUL','ACCURATE')),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (comment_id, user_id)
);
CREATE INDEX idx_comment_reactions_comment_type ON comment_reactions (comment_id, reaction_type);
```

## 4. API

Giống API cảm xúc bài viết, đường dẫn đổi sang bình luận (cùng tiền tố `/api/community` nên gateway không cần sửa):

| API | Hành vi |
|---|---|
| `PUT /api/community/comments/{commentId}/reactions/{type}` | Thả hoặc đổi cảm xúc. Cùng mã trạng thái và không có body như API của bài viết |
| `DELETE /api/community/comments/{commentId}/reactions/{type}` | Bỏ cảm xúc. Người đó không có loại đó thì không làm gì và không lỗi |

Quy tắc:

- Bình luận phải `ACTIVE` và bài viết chứa nó phải xem được (dùng `CommunityApplicationSupport.requireVisiblePost` như `AddReactionUseCase`). Bình luận `HIDDEN`/`DELETED` hoặc không tồn tại: 404 `CommunityNotFoundException`, trừ quản trị viên theo đúng cách bài viết đang xử lý.
- Được thả cảm xúc cho bình luận của chính mình.
- `type` sai: 400 (enum `ReactionType` đã có sẵn).

**Thay đổi response** (thêm trường, không phá client cũ). `CommentResponse` thêm hai trường **cùng dạng với bài viết**:

```json
{
  "id": "…", "postId": "…", "authorId": "…", "parentCommentId": null,
  "body": "…", "status": "ACTIVE", "createdAt": "…", "updatedAt": "…",
  "author": { "…": "…" }, "replyCount": 2,
  "reactionCounts": { "LIKE": 3, "LOVE": 1, "HELPFUL": 0, "ACCURATE": 0 },
  "myReaction": "LIKE"
}
```

`myReaction` là `null` khi người xem chưa thả. Trường mới có mặt ở mọi nơi trả `CommentResponse`: `GET /posts/{postId}/comments` (cả `threaded=true`), `GET /comments/{commentId}/replies`, `POST /posts/{postId}/comments`, `PUT /comments/{id}`.

## 5. Việc cần làm trong code (community-service)

1. `infrastructure/persistence/entity`: `CommentReactionJpaEntity`, `CommentReactionId`; `repository/CommentReactionJpaRepository` có `insertOrReplace`, `remove`, đếm theo danh sách bình luận (mẫu: `PostReactionJpaRepository`).
2. `domain/repository/CommentReactionRepository` (`add`, `remove`, `counts(Set<UUID>)`, `mine(Set<UUID>, viewer)`) và `infrastructure/adapter/CommentReactionRepositoryAdapter`.
3. Use case `AddCommentReactionUseCase`, `RemoveCommentReactionUseCase` (`@Transactional`, kiểm tra như mục 4); command `AddCommentReactionCommand`, `RemoveCommentReactionCommand`.
4. `CommunityController`: hai endpoint mới.
5. `CommentResult`/`CommentResponse`: thêm `reactionCounts` (`Map<ReactionType, Long>`, đủ 4 khóa, thiếu thì 0) và `myReaction` (`ReactionType` hoặc null); cập nhật hàm `withDetails` và `from`.
6. `CommunityQueriesUseCase.enrichComments`: đổi chữ ký thành `enrichComments(CommunityActor actor, List<CommentResult>)`, và lấy **đếm cảm xúc cùng cảm xúc của người xem bằng một truy vấn gộp cho cả trang** (không truy vấn từng bình luận), giống `reads.interactions` của bài viết. Cập nhật `thread(...)` và **ba chỗ gọi trong controller** (dòng 168, 195, 282).
7. Khi bình luận bị ẩn hoặc xóa, các hàng cảm xúc giữ nguyên (xóa mềm); bình luận không còn hiện nên không ảnh hưởng.

## 6. Kiểm thử (viết trước)

- Thả lần đầu tạo một dòng; thả loại khác thay thế (vẫn một dòng); thả lại cùng loại không đổi; bỏ loại không có là no-op.
- Hai người thả cùng bình luận: `reactionCounts` đúng; mỗi người thấy `myReaction` của chính mình.
- Bình luận `HIDDEN`/`DELETED`, bình luận của bài viết ẩn, bình luận không tồn tại: 404.
- `type` không hợp lệ: 400. Chưa đăng nhập: 401.
- Danh sách 20 bình luận chỉ thêm **một** truy vấn cảm xúc (đếm câu lệnh).
- Cả bốn nơi trả `CommentResponse` đều có `reactionCounts` và `myReaction` (kể cả `threaded=true` và trả lời).
- Test thả hai request đồng thời cùng người, cùng bình luận, khác loại: kết quả cuối còn đúng một dòng.
- Test migration V11 trên dữ liệu có sẵn (bảng mới nên không ảnh hưởng dữ liệu cũ).

## 7. FE và mobile (việc nhỏ, sau khi BE xong)

- **FE** (`frontend/src/features/community/api/communityApi.ts`): thêm `reactComment(id, type)` và `removeCommentReaction(id, type)`; thêm `reactionCounts`, `myReaction` vào kiểu bình luận; dùng lại thành phần nút cảm xúc của bài viết ở dòng bình luận.
- **Mobile** (`src/features/community/api.ts`, `reaction-meta.ts`, `components/community-reaction-control.tsx`): cùng việc; dùng lại `getReactions()` để biểu tượng và màu khớp bài viết.
- Hai bên tự cập nhật bộ nhớ đệm bình luận sau khi thả (cảm xúc là dữ liệu của server, không tự tính).

## 8. Ngoài phạm vi (cố ý)

- **Thông báo cho tác giả bình luận khi có người thả cảm xúc.** Bài viết có sự kiện `POST_REACTED`; bình luận cần thêm kiểu sự kiện trong `common-events` và xử lý ở notification-service. Để sau nếu cần.
- Danh sách ai đã thả cảm xúc, emoji tự chọn, cảm xúc cho nội dung ngoài community (unit từ vựng, bài học).

## 9. Xong khi

- Hai API mới chạy qua gateway; mọi `CommentResponse` có `reactionCounts` và `myReaction`.
- Test community-service xanh (`mvn -pl services/community-service -o clean test`).
- FE và mobile thả và bỏ được cảm xúc trên bình luận mà không cần sửa BE thêm.

## 10. Ghi nhận thực hiện (07/11/2026, Asia/Bangkok)

Các mục 1–9 giữ nguyên yêu cầu và bối cảnh trước khi triển khai. Backend đã hoàn tất trên nhánh `feat/comment-reactions`, không lệch thiết kế; enrichment cho response kiểm duyệt bảo đảm mọi `CommentResponse` có dữ liệu cảm xúc. Lượt test sạch cuối cùng đã qua; kiểm tra E2E runtime qua gateway là giới hạn xác minh còn lại, chưa thực hiện.

- [x] Ghi nhận TDD: test mới được thêm trước, lượt compile RED do các kiểu/chữ ký chưa được triển khai; sau đó bổ sung implementation và cập nhật các caller bị ảnh hưởng.
- [x] `AddCommentReactionUseCase`/`RemoveCommentReactionUseCase`, `CommentReactionJpaRepository` và migration `V11__comment_reactions.sql` thực hiện thêm/thay thế/no-op, kiểm tra quyền xem và khóa duy nhất theo bình luận/người dùng.
- [x] `CommunityQueriesUseCase.enrichComments(actor, content)` và `CommentResponse` trả đủ bốn số đếm cùng cảm xúc của người xem ở danh sách phẳng, luồng, trả lời, tạo, sửa và kiểm duyệt.
- [x] Tester xác nhận cả 13 test của `CommunityOutboxUseCaseIntegrationTest` và test nâng cấp dữ liệu V10 của `CommentReactionMigrationTest` đã qua. Test `twentyCommentsUseOneSqlForCountsAndViewerReaction` đo đúng một prepared statement cho 20 bình luận; test đồng thời xác nhận còn đúng một hàng.
- [x] Review source không phát hiện blocker; không thêm thông báo cho tác giả hay thay đổi module khác.
- [x] Lượt `clean test` cuối cùng: **244 tests, 0 failures, 0 errors, 0 skipped**, exit 0, 2 phút 07 giây; chi tiết trong [báo cáo xác minh](reports/comment-reactions-verification.md).
- [ ] Giới hạn xác minh: chưa chạy E2E runtime qua gateway; route `/api/community/**` hiện có được giữ nguyên. Không ghi nhận E2E hoàn tất.

Chi tiết bằng chứng và phần còn chờ: [comment-reactions-verification.md](reports/comment-reactions-verification.md). Hợp đồng API: [comment-reactions-v1.md](../../docs/contracts/comment-reactions-v1.md).
