 # Thiết kế thống kê dashboard cá nhân — BE và FE

> Nghiên cứu 14/10/2026; cập nhật contract và giới hạn triển khai 15/10/2026. Phạm vi: màn hình P-11 “Dữ liệu chi tiết”. Các phần đề xuất giữ lại bối cảnh thiết kế; trạng thái triển khai/kiểm chứng nằm trong plan và phase. Hai quyết định đã chốt: **tiến độ = hoàn thành bài học; bỏ Mentor**.

## Mục lục

1. [Kết luận và hiện trạng](#1-kết-luận-và-hiện-trạng)
2. [Định nghĩa từng chỉ số](#2-định-nghĩa-từng-chỉ-số)
3. [Dữ liệu phát sinh khi học](#3-dữ-liệu-phát-sinh-khi-học)
4. [Thiết kế BE và API](#4-thiết-kế-be-và-api)
5. [Thiết kế FE](#5-thiết-kế-fe)
6. [Tối ưu, kiểm thử và phát hành](#6-tối-ưu-kiểm-thử-và-phát-hành)
7. [Quyết định còn mở](#7-quyết-định-còn-mở)
8. [Nguồn nghiên cứu](#8-nguồn-nghiên-cứu)

## 1. Kết luận và hiện trạng

Giải pháp phù hợp là **ghi đúng dữ liệu tại thời điểm học, tính tại service sở hữu, FE ghép 4 nhóm response thành 5 khu vực**. Không cần tạo service thống kê riêng. Không lấy dữ liệu mẫu làm công thức nghiệp vụ.

| Khu vực | Đã có | Còn thiếu | Service chịu trách nhiệm |
|---|---|---|---|
| Thời gian thực tế/kế hoạch | Activity có duration do client gửi; goal có `availableMinutesPerDay`; profile có timezone | Đo active time, chống trùng/2 tab, lịch sử mục tiêu theo ngày, API bucket ngày | user |
| Tiến độ 4 kỹ năng | Lesson completion, metadata multi-skill từ Content | Mẫu số lesson đúng scope và query tổng hợp, trạng thái thiếu skill | learning + metadata content |
| Lịch sử band | AssessmentResult có version; SkillScore có band/source | API lịch sử theo user; score snapshot/provenance đầy đủ; tiêu chuẩn đủ điều kiện | assessment |
| Từ vựng cần ôn | Catalog, flashcard, deck cá nhân | Trạng thái ghi nhớ, review event, due time, submit review và aggregate | library |
| Khóa đang học | Course list, số topic passed, kết quả course test | Phân biệt catalog và course đã bắt đầu; đếm lesson; hoạt động gần nhất | learning + metadata content |
| Mentor | Chỉ dữ liệu mẫu ở FE | **Loại khỏi phạm vi theo yêu cầu** | Không bổ sung |

### Những điểm dễ làm thống kê sai

- Activity hiện tại là dữ liệu client khai báo, `verifiedAt=null`, mỗi lần tạo có UUID mới; không thể cộng thẳng rồi gọi là thời gian học đã xác minh.
- `completed_at - started_at` của attempt gồm cả thời gian nghỉ; duration của audio là độ dài tài nguyên, không phải số phút người học tập trung.
- Mastery theo KP là bằng chứng đúng/sai; khác phần trăm lesson và khác IELTS band.
- `GET /api/learning/courses` hiện liệt kê curriculum, không phải danh sách đã đăng ký. `passedTopicCount=0` vẫn có thể đang học lesson đầu tiên.
- Placement Speaking hiện có band cấu hình mặc định 0; Writing có fallback 5.5 khi chấm không thành công. Chưa thể coi tất cả điểm này là tiến bộ đã đo.
- Con số 34% trong mock không khớp các số đếm kèm theo. Không giữ 34% như requirement.
- Các nhãn “Từ mới / Ôn lần 1 / Ôn lần 2 / Ôn lần 3 / Đã thuộc” là mức ghi nhớ cá nhân, **không phải CEFR A1–C2**.

## 2. Định nghĩa từng chỉ số

### 2.1. Biểu đồ thời gian học

**Tên nên dùng:** “Thời gian học được ghi nhận”. Đây là ước lượng dựa trên tín hiệu hoạt động trình duyệt đã qua kiểm soát phía server; không chứng minh tuyệt đối sự tập trung.

```text
actualMinutes(day) = tổng activeMilliseconds được server chấp nhận của ngày / 60_000
plannedMinutes(day) = mục tiêu phút/ngày có hiệu lực trong ngày đó
attainment = actualMinutes / plannedMinutes × 100, chỉ khi plannedMinutes > 0
```

- Lưu số nguyên milliseconds/seconds; chỉ làm tròn sau tổng hợp. Tooltip có thể hiển thị phút/giây; không làm tròn từng heartbeat.
- Tuần: thứ Hai đến Chủ nhật, 7 bucket. Tháng: đúng số ngày của tháng. Không luôn vẽ 7 điểm khi chọn tháng.
- Chế độ “Khóa”: chọn course, hiển thị thời gian được gắn vào course đó trong khoảng ngày; khoảng tối đa đề xuất 366 ngày, tự nhóm tuần khi dài hơn 31 ngày. Query phải luôn có khoảng ngày hữu hạn.
- Mục tiêu hiện tại là toàn cục. Khi lọc course, **không gán toàn bộ mục tiêu/ngày cho từng course**. Trả `plannedMinutes=null`, giải thích chưa có kế hoạch theo course. Chỉ thêm phân bổ course nếu sản phẩm duyệt sau.
- Không goal/goal tạm dừng: target là null, không vẽ đường 0 giả. Ngày tương lai: actual null. Ngày đã được đo nhưng không hoạt động: actual 0. Ngày trước khi bật đo: `NOT_TRACKED`, không chuyển thành 0.
- Đổi goal từ 30 lên 60 phút không sửa lịch sử tuần trước. Thêm bản ghi mục tiêu có hiệu lực theo ngày; đề xuất thay đổi có hiệu lực từ ngày kế tiếp. Khi bắt đầu triển khai, ghi snapshot goal đang active; không dựng lại lịch sử thay đổi đã mất.
- Chuẩn ngày: timezone IANA đã validate, mặc định đề xuất Asia/Ho_Chi_Minh. Lưu timestamp UTC và timezone dùng khi ghi. Không dùng timezone máy server. Tách session qua nửa đêm thành đúng ngày địa phương. Đổi timezone có hiệu lực từ session/ngày mới; lịch sử đã chốt giữ nguyên timezone gốc và response nói rõ `timezonePolicy=RECORDED_LOCAL_DATE`.

**Điều kiện active đề xuất:** trang học thật đang visible và có tương tác gần đây, hoặc audio/video/recording phục vụ bài học đang chạy. Idle đọc/viết đề xuất 120 giây; người đọc lâu không thao tác có thể bị thiếu thời gian, cần công khai điều này và điều chỉnh qua thử nghiệm UX. Khi audio đã chạy liên tục trên tab visible, không buộc click mỗi 120 giây. Tạm dừng media, buffering kéo dài hoặc tab hidden không cộng thời gian. Background listening là quyết định riêng, mặc định chưa tính.

### 2.2. Tiến độ 4 kỹ năng — quyết định đã chốt

```text
eligibleLessons(skill, scope) = tập lesson được publish, thuộc course trong scope,
                              có skill đó trong metadata của chính lesson
completedLessons = eligibleLessons ∩ lesson_progress có completed_at
percent = 100 × count(completedLessons) / count(eligibleLessons)
```

- Một lesson có Reading + Listening + Writing và đã hoàn thành: cộng 1 vào từng kỹ năng có mặt. Tổng 3 số đếm không phải số lesson duy nhất toàn hệ thống.
- Thẻ course đếm `DISTINCT lessonId`, không cộng số lesson của các skill.
- Lesson mới làm một block chưa complete: chưa tăng tử số. Thử lại một lesson đã complete: không tăng thêm.
- Practice thất bại hoặc review còn chờ không làm mất trạng thái lesson đã complete. Hiển thị “còn bài luyện/ôn” riêng nếu cần; không thay đổi quy tắc mở bài.
- Nếu skill không có lesson: `percent=null`, `totalLessons=0`, hiển thị “Chưa có nội dung”; không ghi Speaking 0% rồi đánh giá học viên yếu.
- Không dùng các nhãn “Đúng tiến độ / Nhanh hơn / Chậm hơn” khi chưa có lịch lesson theo ngày. Đề xuất nhãn xác định được: “Chưa bắt đầu / Đang học / Đã hoàn thành / Chưa có nội dung”. Bỏ dòng “AI đánh giá” và đoạn AI nhận xét hardcode.
- Scope đề xuất: mặc định các course đã bắt đầu, có bộ chọn một course. Trả `scope`, `courseIds`, `curriculumRevision` và counts để tooltip giải thích mẫu số.
- Nếu nội dung publish thay đổi, tỷ lệ theo **nội dung hiện hành** có thể giảm. Đề xuất chấp nhận và ghi nhãn revision/thời điểm cập nhật; nếu muốn tỷ lệ không bao giờ giảm thì cần snapshot curriculum lúc tham gia, là scope dữ liệu lớn hơn chưa mặc định thêm.

**Ví dụ:** course có 10 lesson Reading, 8 Listening, 6 Writing, chưa có Speaking; learner complete lần lượt 4/3/1. Radar là 40%, 37.5%, 16.7%, chưa có dữ liệu Speaking. Bảng chữ đi kèm hiển thị chính xác `4/10`, `3/8`, `1/6`.

### 2.3. Biểu đồ tiến bộ band

Nguồn là **kết quả bài thi có đủ thông tin chấm**, không chuyển phần trăm lesson hoặc mastery thành band.

- Chỉ lấy result `COMPLETED`; chọn version COMPLETED mới nhất của mỗi attempt. Version mới đang chấm không xóa điểm hoàn tất trước đó, nhưng trả `regradePending=true`.
- Mỗi điểm chứa attemptId/resultId/resultVersion, ngày nộp bài, ngày chấm, skill, band, loại đề, nguồn AUTO/AI/HUMAN, rubric/scoring version và chất lượng dữ liệu. `gradedAt` mới do chấm lại không trở thành một lần thi mới.
- Trục thời gian dùng thời điểm nộp bài; chấm lại thay điểm của chính attempt tại vị trí cũ. Sắp ổn định bằng timestamp + attemptId.
- Contract ngày lọc: `timezone` là tham số IANA tùy chọn, mặc định `Asia/Ho_Chi_Minh`; timezone không hợp lệ trả 400. Chuyển `from` và `to` thành khoảng UTC nửa mở `[from.atStartOfDay(timezone), to.plusDays(1).atStartOfDay(timezone))`. Như vậy tính cả ngày `to` tại địa phương, kể cả ngày đổi DST; không giả định mọi ngày dài 24 giờ. `dataFrom` chuyển từ timestamp sang ngày trong cùng timezone. FE gửi timezone trình duyệt cùng khoảng ngày band.
- **Không nối tùy tiện** điểm QUIZ, topic gate, lesson Writing và full test vào một chuỗi band. API nhóm theo `seriesKind` và tiêu chuẩn đề; đề thiếu chuẩn scoring thì trả unavailable hoặc hiển thị điểm nội bộ bằng đơn vị riêng.
- Đề xuất series mặc định: mock/full assessment có rubric được xác nhận. Placement có thể là điểm nền riêng, gắn nhãn “ước lượng placement”; không dùng band fallback hoặc Speaking chưa đo.
- Overall chỉ xuất hiện khi 4 kỹ năng của **cùng một lần thi** có điểm đủ điều kiện. Không lấy Reading hôm nay, Writing tháng trước, Speaking mặc định để tính Overall. Công thức trung bình 4 kỹ năng và làm tròn nửa band theo IELTS chỉ là quy tắc tính, không biến điểm nội bộ thành chứng chỉ IELTS. [IELTS scoring](https://ielts.org/take-a-test/your-results/ielts-scoring-in-detail)
- Thiếu Speaking: các tab Reading/Listening/Writing vẫn hiển thị nếu đủ điều kiện; Overall/Speaking báo thiếu dữ liệu, không thay bằng 0.
- `latest`, `baseline`, `delta` dùng cùng skill, series và khoảng chọn. Chỉ một điểm thì delta null; điểm giảm thì hiển thị giảm, không luôn “+”. Chấm lại không làm tăng `attemptCount`.
- Câu “đang tiến tới IELTS 6.5 đúng tốc độ” là dự báo chưa có cơ sở. Thay bằng thông tin xác định được: “Gần nhất 5.5 · Mục tiêu 6.5 · Chênh lệch 1.0”. Không gọi LLM khi tải trang.

**Việc BE phải làm trước:** freeze skill score khi hoàn tất; bổ sung cờ `MEASURED / FALLBACK / NOT_ASSESSED / UNKNOWN_LEGACY` ở dữ liệu kết quả, giữ enum GradingSource hiện có cho nguồn chấm thật. Không suy ra provenance chỉ từ giá trị 0 hoặc 5.5; hai giá trị ấy cũng có thể là điểm thật. Result cũ chỉ backfill khi nguồn đã lưu đủ chứng minh, còn lại gắn UNKNOWN_LEGACY. Không thay thuật toán placement/gate trong task dashboard.

### 2.4. Từ vựng cần ôn

Đây là phần cần xây **luồng học và ôn có lưu trạng thái**, không chỉ API đếm flashcard.

**Đề xuất đơn vị chuẩn:** một `vocabularySenseId` đã được learner đưa vào bộ học. Cùng nghĩa xuất hiện ở 3 deck vẫn đếm 1. Flashcard MANUAL/HIGHLIGHT chưa gắn nghĩa hoặc PRACTICE_QUESTION không tự tính là một từ; muốn bao gồm phải định nghĩa khóa học liệu riêng và đổi nhãn thành “mục ôn tập”. Không gom từ khác nghĩa chỉ vì giống spelling.

```text
total = new + level1 + level2 + level3 + retained
retainedPercent = retained / total × 100
dueCount = count(mục đã học, active, nextReviewAt <= serverNow)
```

Donut đề xuất ghi rõ **“Đã ghi nhớ”**; tiêu đề thẻ “Từ vựng cần ôn” hiển thị thêm dueCount. Đổi legend mock “Đã học/Chưa học” thành “Đã ghi nhớ/Đang học” cho cùng mẫu số. Khi total=0, phần trăm null và CTA “Thêm từ để học”. Mục mới chưa học tách khỏi dueCount; “Ôn ngay” mở queue đã đến hạn, không tự đánh dấu học xong.

**Thang đơn giản để duyệt, không phải thuật toán hiện có:**

| Trạng thái | Điều kiện vào | Lịch lần kế tiếp sau khi nhớ đúng |
|---|---|---|
| NEW | Learner thêm nghĩa vào bộ học | Chưa có lịch ôn |
| LEVEL_1 | Recall đầu tiên thành công | Sau 1 ngày |
| LEVEL_2 | Đạt lượt ôn tới hạn tiếp theo | Sau 3 ngày |
| LEVEL_3 | Đạt lượt ôn tới hạn tiếp theo | Sau 7 ngày |
| RETAINED | Đạt lượt ôn tới hạn tiếp theo | Sau 14 ngày; các lần sau giữ mức, hẹn lại |

- Một lượt quên chuyển về LEVEL_1, hẹn học lại sau 10 phút. Đây là tham số sản phẩm cần thử nghiệm, không khẳng định tối ưu khoa học.
- Click lật xem đáp án không phải nhớ đúng. Review có prompt → trả lời/recall → feedback → submit có `reviewId` và `requestId`; server giữ đáp án khi chấm objective. Nếu tự đánh giá thì ghi rõ `SELF_RATED`, không gọi là đánh giá khách quan.
- Không thăng cấp nhiều lần bằng bấm lại trong một phiên: chỉ lượt đến hạn và đúng version mới thay đổi cấp; lượt luyện sớm lưu lịch sử nhưng không thăng cấp.
- Retained vẫn có thể đến hạn; quên có thể hạ cấp. `dueCount` là tập con có thể giao với retained, không cộng vào total.
- Archive/xóa khỏi bộ đang học loại khỏi mẫu số; lịch sử không bị rewrite. Tái thêm cùng nghĩa khôi phục state có quy tắc, không tạo mục trùng. Nội dung bị sửa ý nghĩa phải tăng version và reset/lập lịch lại theo rule được duyệt.

**Ví dụ số mock:** 12 + 25 + 34 + 20 + 110 = 201; nếu 110 là retained thì tỷ lệ phải là 54.7%, không phải 34%. Không sửa số nguồn bằng tay để vừa biểu đồ.

### 2.5. Các khóa học của tôi

- Hiện không có enrollment theo nghĩa mua/đăng ký course. Đề xuất dùng “Khóa đang học” = có lần bắt đầu học thành công hoặc bằng chứng học của learner; không coi mở danh mục course là tham gia.
- Từ lúc triển khai, lưu `startedAt`, `lastStudiedAt` khi mở lesson được gate cho phép/bắt đầu hoạt động học thật. Không lấy thời điểm refresh curriculum làm lastStudiedAt.
- Backfill khóa đã học từ lesson completion/submission/practice thuộc user; không gán ngày bắt đầu chính xác khi dữ liệu không chứng minh. Phân biệt `UNKNOWN_LEGACY`.
- Tiến độ thẻ = `completedLessonCount / totalPublishedLessonCount`; thêm `passedTopicCount / topicCount` và trạng thái course test bằng trường riêng nếu UI cần.
- Dùng “bài”, không dùng “buổi” khi course chưa có khái niệm buổi học/lịch lớp. Badge bandLevel là cấp course, không phải band hiện tại của người học.
- Đề xuất preview 4 khóa, sort `lastStudiedAt DESC, courseId`, xem tất cả phân trang. Khóa đã hoàn thành không biến mất khỏi lịch sử; có filter riêng.
- `resumeTarget` phải do Learning xác định theo gate hiện hành; FE không tự đoán bài tiếp theo dựa vào phần trăm. 100% lesson không đồng nghĩa đã pass course test.

## 3. Dữ liệu phát sinh khi học

### 3.1. Một buổi học cụ thể

| Hành động | Dữ liệu BE cập nhật | Dashboard thay đổi |
|---|---|---|
| Mở lesson và được gate cho phép | Learning ghi dấu bắt đầu course; User tạo session đo thời gian | Course xuất hiện, chưa tăng % |
| Đọc/nghe/gõ đáp án trong trang học | FE gửi heartbeat; User chấp nhận thời gian hoạt động | Tăng phút ngày hiện tại |
| Chuyển tab/nghỉ | FE flush phần đang có, dừng đo | Không tiếp tục cộng phút |
| Nộp block chưa đủ hoàn thành lesson | Learning lưu submission/evidence theo rule hiện có | Phút tăng, % chưa tăng |
| Lesson chuyển sang COMPLETED | Learning lưu completion một lần | % lesson và skill cập nhật |
| Làm Practice/Review | Learning lưu attempt/review, thực thi gate | Thời gian tăng; không cộng lesson lần hai |
| Nộp bài thi đang chờ chấm | Assessment lưu submit/processing | Chưa thêm điểm band chưa chấm |
| Chấm xong | Assessment lưu result/skill snapshot cùng provenance | Thêm điểm đủ điều kiện; Learning nhận event hiện có cho gate |
| Chấm lại | Assessment tạo version mới, finalize | Thay điểm attempt cũ, không cộng một bài thi mới |
| Thêm từ vào bộ học | Library tạo personal vocabulary state NEW | Tăng mẫu số, không tăng retained |
| Trả lời lượt ôn tới hạn | Library lưu review event + state mới trong một transaction | Đổi bucket, dueCount, retained% |
| Quay về dashboard | FE refetch từng nhóm đã bị thay đổi | Hiển thị số đã commit, kèm generatedAt |

### 3.2. Đo thời gian đủ tin cậy, chi phí vừa phải

Đề xuất heartbeat **30 giây**, idle 120 giây, lease hết hạn sau **90 giây**. Các con số là cấu hình đề xuất để duyệt.

1. Sau khi API mở nội dung thành công, FE gọi start session với `clientSessionId`, sourceType/sourceId. Server xác nhận nguồn thuộc user hoặc nội dung được phép học, xác định course từ nguồn; không tin courseId do client tự khai.
2. Mỗi user chỉ có một session đang được quyền ghi thời gian. Tab/device khác nhận `SESSION_IN_USE`, không tự giật quyền mỗi heartbeat. Cho phép “Tiếp tục đo tại đây” để takeover rõ ràng hoặc đợi lease hết hạn.
3. Session lưu `serverStartedAt`, lease epoch, last accepted sequence/receipt, tổng đã chấp nhận. FE dùng `performance.now()` đo khoảng, không dùng đồng hồ máy làm nguồn chân lý.
4. Payload gồm sessionId/epoch/sequence và các đoạn active trong cửa sổ vừa đo, với offset tính từ mốc session. Server ánh xạ vào mốc UTC, giới hạn không vượt thời gian đã trôi, lease, kích thước payload và khoảng trễ tối đa 90 giây. Không nhận backlog offline dài ở bản đầu.
5. Trong transaction khóa theo user: kiểm lease → replay cùng sequence trả receipt cũ → payload khác cùng sequence trả conflict → loại interval trùng/đã ghi → chia interval qua ranh giới ngày → ghi ledger và cộng daily aggregate → lưu receipt. Thời gian accepted toàn user không vượt hợp các interval; không chỉ kiểm cap riêng từng tab.
6. Client gửi tuần tự, giữ nguyên payload khi retry mất ACK; không gửi sequence mới trước khi sequence cũ được xử lý hoặc session hết hạn. Server lưu accepted sequence lâu ít nhất bằng vòng đời session/retry; không xóa dedupe sớm rồi cho replay cộng lại.
7. Hidden, route change, submit, pause, logout: flush phần hiện có rồi đóng/dừng. Khi mất mạng/đóng máy đột ngột, chỉ phần đã ACK là chắc chắn; không cộng toàn bộ gap khi quay lại. Nếu vẫn online nhưng tab chết, thường thiếu tối đa khoảng heartbeat; outage dài có thể thiếu nhiều hơn, không hứa giới hạn 30 giây cho mọi lỗi.
8. Cuối trang dùng `fetch(..., {keepalive:true})` nhỏ, giữ Bearer trong header và credentials theo client hiện có; không dùng sendBeacon rồi giả định auth vẫn hoạt động. Flush cuối là best effort; không lưu token vào payload/localStorage và không dựa vào unload duy nhất. [MDN sendBeacon](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/sendBeacon), [MDN keepalive](https://developer.mozilla.org/en-US/docs/Web/API/Request/keepalive)

**Giới hạn tin cậy:** client có thể giả tương tác; server chỉ giảm lỗi đo/đếm trùng và chặn vượt thời gian thực. Không dùng chỉ số này cho tiền, điểm thưởng, chứng nhận hoặc kết luận chống gian lận. Telemetry lỗi không làm nộp bài thất bại.

**Giới hạn course của Assessment đã xác nhận:** `AssessmentAttempt`, JPA schema, `ContentPackageProvider` và `AttemptCreator` lưu `packageVersionId`, loại đề, câu hỏi và knowledge-point IDs, nhưng không lưu liên kết topic/course đủ tin cậy. Vì vậy `StudyContext.courseId` vẫn là null, kể cả topic/course test: thời gian hợp lệ vào tổng ALL nhưng chưa vào scope course. Không suy đoán course từ knowledge-point IDs hoặc dữ liệu client; không đọc database service khác, thêm schema hay đổi `AssessmentCompleted.v2` trong lượt sửa này. Muốn phân bổ theo course cần contract liên kết do owner xác nhận ở phạm vi riêng.

### 3.3. Sơ đồ trách nhiệm

```text
Trang học FE ── heartbeat ──> User: session / interval / daily time / goal history
Nộp lesson ───────────────> Learning: completion / practice / review / course activity
Làm bài thi ──────────────> Assessment: versioned result / skill score / provenance
Ôn từ vựng ───────────────> Library: personal vocabulary / recall event / due state

Dashboard FE ── song song qua Gateway ──> 4 API đọc tổng hợp của các owner
Learning ── một API metadata theo tập ──> Content (lesson/course/skill, không đáp án)
```

Không thêm event cho mỗi heartbeat. Không nhân bản toàn bộ kết quả chấm vào User chỉ để dashboard đọc một endpoint. Event AssessmentCompleted.v2 hiện phục vụ Learning; không giả định nó chứa skill band đầy đủ hoặc sửa contract đó trong scope này.

## 4. Thiết kế BE và API

### 4.1. So sánh cách ghép dashboard

| Phương án | Lợi ích | Chi phí/rủi ro | Đề xuất |
|---|---|---|---|
| FE gọi 4 API aggregate theo owner | Dễ triển khai, dữ liệu đúng nguồn, lỗi từng thẻ độc lập | 4 request, thời điểm snapshot khác nhau | **Chọn cho giai đoạn này** |
| Một endpoint composer trong service hiện có | Một request FE | Service đó phụ thuộc cả 4 owner, thêm timeout và fan-out | Chỉ cân nhắc sau đo đạc |
| Analytics service/read model riêng qua event | Tốt cho nhiều báo cáo/volume lớn | Contract, backfill, reconciliation, lag, vận hành mới | Chưa cần và chưa được duyệt |

Mỗi widget có `generatedAt`, `availability` và `dataFrom`; không giả vờ 4 response là một snapshot transaction toàn hệ thống. Nếu endpoint cần Content không sẵn sàng, trả unavailable cho nhóm đó thay vì tỷ lệ 0.

### 4.2. Bảng dữ liệu đề xuất — migration mới

| Owner | Bảng/extension đề xuất | Trường và ràng buộc chính |
|---|---|---|
| User | `study_sessions` | user/source/course đã xác nhận, clientSessionId unique theo user, lease epoch, mốc UTC, trạng thái, sequence/receipt |
| User | `study_activity_intervals` | session/sequence/segment unique; UTC start/end, active time, recorded local date/timezone; không chứa nội dung câu trả lời |
| User | `study_daily_totals` | `(user_id, local_date, timezone, scope_key)` unique; active_ms; `scope_key=ALL` hoặc course cụ thể; ALL chỉ cộng một lần |
| User | `study_goal_history` | user, effective local date/timezone, minutes, active/paused, goalId; bản ghi ngày không bị cập nhật ngược |
| Learning | extension course activity riêng | user/course unique; startedAt/lastStudiedAt + provenance; không lạm dụng course test passedAt |
| Assessment | extension snapshot skill/result | scoringVersion, eligibility/quality, frozen band; tận dụng `skill_scores` và `assessment_results`, không tạo kho điểm song song |
| Library | `learner_vocabulary_progress` | `(user_id,vocabulary_sense_id)` unique; state, nextReviewAt, stateVersion, active, lastReviewedAt |
| Library | `vocabulary_review_sessions/items` | reviewId/user; danh sách item được giao, stateVersion, thời hạn; sở hữu và độ lớn queue hữu hạn |
| Library | `vocabulary_review_events` | requestId unique theo user, item/version/outcome/source/reviewedAt; append lịch sử và update state cùng transaction |

Tên bảng là đề xuất, số version Flyway chọn lúc triển khai sau khi xem migration mới nhất. Không rewrite migration cũ. Receipt/interval retention, giới hạn payload và rate limit cần quyết định trước release; đề xuất interval 90 ngày, daily totals lâu hơn theo chính sách sản phẩm. Chỉ purge khi có quy tắc dữ liệu được duyệt, đồng thời giữ watermark chống replay và ghi rõ reconciliation chỉ đầy đủ trong cửa sổ raw data còn lưu.

### 4.3. API bổ sung đề xuất

Đây là contract mới cần duyệt cùng plan, **không phải endpoint đã có**. Identity lấy từ CurrentUserProvider; không nhận userId tùy ý cho dashboard cá nhân.

| Method/path đề xuất | Owner | Dữ liệu/trách nhiệm |
|---|---|---|
| POST `/api/users/me/study-sessions` | User | Start/resume với idempotency; trả server anchor, epoch, heartbeat/lease policy |
| POST `/api/users/me/study-sessions/{id}/heartbeats` | User | Nhận interval bounded; trả sequence/acceptedMs/totals/lease |
| POST `/api/users/me/study-sessions/{id}/close` | User | Close idempotent, last flush; hết hạn không tự nhận gap |
| GET `/api/users/me/statistics/study-time` | User | `from,to,grain,courseId?`; bucket có actual/plan/status/timezone |
| GET `/api/learning/statistics/progress` | Learning | Course scope, 4 skill counts/percent + preview 4 khóa; cursor khi xem tất cả |
| GET `/api/assessments/me/statistics/bands` | Assessment | `from,to,seriesKind,skill?,timezone?`; timezone IANA mặc định `Asia/Ho_Chi_Minh`, khoảng ngày địa phương nửa mở như §2.3; tối đa 100 điểm/lượt, cursor, latest/baseline/delta tính toàn khoảng; `dataFrom` cùng timezone |
| GET `/api/learning-support/flashcards/statistics/vocabulary` | Library | Các bucket ghi nhớ, total/retained/due/asOf; chỉ vocabulary-sense scope |
| POST `/api/learning-support/flashcards/vocabulary-review-sessions` | Library | Giao queue hữu hạn, mặc định 20, tối đa 50 |
| POST `/api/learning-support/flashcards/vocabulary-review-sessions/{id}/answers` | Library | Recall outcome, requestId, item/stateVersion; update atomic |

Library cần bổ sung thao tác “đưa nghĩa vào bộ học” trong use case lưu flashcard hoặc endpoint chuyên biệt nếu FE không tạo flashcard. Không thêm write ngầm trong GET statistics. Cách nhận self-rating/khách quan sẽ được chốt với luồng ôn trước implementation.

Prefix mới nằm trong route hiện có `/api/users/**`, `/api/learning/**`, `/api/assessments/**`, `/api/learning-support/flashcards/**`. Kiểm tra matching tĩnh trước `/{id}` và security config từng service. Không sửa Gateway/shared security chỉ vì có dashboard nếu các prefix đã đáp ứng.

Internal metadata mới ở Content: trả tập course → lesson → skill và revision theo tối đa số course/lesson đã giới hạn trong contract; không trả `answerSpec`, transcript hoặc chartFacts. Extend `LearningContentClient` bằng batch method, không lặp `getTopicLessons()` trên từng topic. Nếu giới hạn sản phẩm bị vượt, báo rõ/cho phân trang; không tính % từ tập bị cắt mà coi là toàn bộ.

Start session cần xác nhận ownership/context qua **application port → HTTP adapter** tới owner của source, tối đa một lần mỗi lần start; không gọi owner mỗi 30 giây. Không giữ transaction/row lock User trong lúc chờ HTTP. Learning/Assessment/Library chỉ xác nhận context thuộc mình, không quyết định luật time tracking. Resource bị xóa/revoke sau start xử lý bằng lease ngắn, kiểm lại khi resume; số phút không trao quyền truy cập nội dung.

### 4.4. Response mẫu tối thiểu

```json
{
  "availability": "AVAILABLE",
  "generatedAt": "2026-10-14T09:00:00Z",
  "dataFrom": "2026-10-14",
  "timezonePolicy": "RECORDED_LOCAL_DATE",
  "scope": "ALL",
  "days": [
    {"date": "2026-10-14", "timezone": "Asia/Ho_Chi_Minh", "actualSeconds": 1500,
     "plannedSeconds": 1800, "status": "PARTIAL_DAY"}
  ]
}
```

Ví dụ chỉ minh họa contract, không seed dữ liệu thật. Skill response cần `skill,totalLessons,completedLessons,percent,status`; course response cần các count theo đúng đơn vị và resumeTarget. Band response phải có `eligibilityReason` khi không hiển thị được; vocab response có `countingUnit=VOCABULARY_SENSE`, `scheduleVersion`.

### 4.5. Query và chi phí

- User: range query daily totals bằng index user + date + scope; không tải toàn bộ activities về Java để group. Dùng truy vấn khoảng UTC/index trước khi xử lý timezone; PostgreSQL hỗ trợ date/time theo timezone. [PostgreSQL date/time](https://www.postgresql.org/docs/15/functions-datetime.html)
- Learning: một batch Content metadata + query progress của user theo tập lesson/course; ghép bằng Map/Set O(n). Aggregate distinct lesson theo skill; không dùng denominator từ số row learner đã chạm tới.
- Assessment: lọc owner và khoảng trước, chọn latest completed version/attempt bằng query set-based; index user/submittedAt ở attempt và attempt/resultVersion/status ở result. Không lấy latest toàn cục rồi mới kiểm tra ownership.
- Library: GROUP BY state + count due bằng predicate; index `(user_id,active,next_review_at)` cho queue; cursor sort dueAt/id, không offset quét sâu.
- Unique key và transaction lock là nền tảng chống race. PostgreSQL advisory transaction lock tự giải phóng khi transaction kết thúc; giữ transaction ngắn, thứ tự khóa nhất quán. [PostgreSQL locking](https://www.postgresql.org/docs/15/explicit-locking.html)
- 1.000 learner đang active với heartbeat 30s tạo khoảng 33 request/s; đây là phép tính tải, không phải benchmark. Không cần gửi event mỗi click/giây.
- Giai đoạn đầu chỉ time dùng daily aggregate vì volume cao. Completion, band, vocab state query trực tiếp với index. Chỉ thêm cache/projection khác sau khi đo SQL và latency thực.

## 5. Thiết kế FE

### 5.1. Tệp sẽ tác động

| Khu vực | Tệp hiện có / đề xuất | Việc cần làm |
|---|---|---|
| Ghép trang | `src/features/overview/pages/OverviewPage.tsx` | Dữ liệu thật cho tab chi tiết, state filter/deep link; không tự mở rộng sang toàn bộ tab tổng quan |
| Chi tiết | `src/features/overview/components/OverviewDetailedStats.tsx` | Bỏ hardcode, Mentor, AI nhận xét giả; tách 5 card khi giảm độ phức tạp |
| Style/type | `src/features/overview/overview.css`, `src/types/overview.ts` | Layout sau bỏ Mentor, nullable/count/source status, chart accessible |
| API mới | `src/features/overview/api/overviewApi.ts` | 4 endpoint aggregate qua httpClient |
| Hook mới | `src/features/overview/hooks/useDetailedStatistics.ts` | Tải độc lập/abort/stale guard/refetch từng nhóm; cùng convention resource hiện có |
| Đo thời gian mới | `src/features/study-activity/useStudySession.ts` và `studyActivityApi.ts` | Dùng chung lesson/practice/review/exam/vocab, xử lý visibility/media/lease/retry |
| HTTP | `src/lib/httpClient.ts` | Nếu cần thêm tùy chọn keepalive cho flush, giữ refresh single-flight và memory-only token |
| Trang học thật | `src/features/learning-path/pages/{LessonPage,PracticePage,ReviewPage,PlacementPage,TopicTestPage}.tsx` | Start sau API success, tín hiệu active, flush và invalidate sau mutation thành công |
| Media | `src/features/learning-path/components/blocks/AudioBlock.tsx`, `placement/ExamAudioPlayer.tsx` | Phát tín hiệu playing/pause/waiting/ended; không gửi text/answer vào telemetry |
| Vocabulary | `src/features/vocabulary/pages/VocabularyPage.tsx` và luồng ôn mới | Nối Library thật, thêm queue/submit recall để số liệu có nguồn |

Các page dưới `features/practice` hoặc classroom còn có mock: chỉ bật tracking khi đã có hoạt động thật và source được BE xác nhận. Không đếm giờ thao tác trên màn hình demo như học thật.

### 5.2. Trạng thái và cách hiển thị

- Mỗi card: loading, ready, empty, unavailable, error; lỗi Library không che time/skill.
- Khi filter đổi: hủy/ignore response cũ bằng key/request generation, không để tuần trước ghi đè tháng mới. Bỏ dữ liệu user cũ ngay logout/chuyển account.
- FE không tự tính nghiệp vụ tiến độ/retained/band. FE có thể đổi seconds sang minutes, tính tọa độ SVG, format số và nhãn dựa trên counts/status BE.
- Với band, FE gửi timezone trình duyệt cùng ngày lọc và hiển thị band/delta một chữ số thập phân. Tuần/tháng thời gian học vẫn được FE chọn theo ngày trình duyệt trong khi BE ghi theo timezone hồ sơ; đây là giới hạn còn lại, không được coi bản sửa band đã đồng bộ hai quy ước này.
- SVG time chart có trục Y/đơn vị và số điểm đúng range; tooltip dùng số thật. Radar dùng cùng 4 counts với thanh progress; có bảng số đọc được bằng screen reader. Không tạo polygon giả cho skill null.
- Khi quay lại dashboard hoặc mutation hoàn tất: refetch phần liên quan. Không polling toàn dashboard mỗi giây; chỉ polling chấm bài đang pending theo backoff có giới hạn, dừng khi hidden/unmount.
- “Chi tiết”, “Ôn ngay”, “Xem tất cả” phải dẫn đến route có thật; khi luồng ôn chưa phát hành, show unavailable/disabled có lý do, không điều hướng sang mock để giả hoàn tất.
- Deep link đề xuất `/overview?tab=detailed-stats&period=week&courseId=...`; parse/validate query, giữ back/forward. Không bắt buộc đổi toàn bộ navigation để triển khai số liệu.
- Wireframe repo docs mới có menu dashboard thay sidebar, còn FE đang có sidebar. Ghi nhận lệch UI; chỉ làm navigation cần để vào chi tiết. Nếu đồng bộ toàn bộ shell thì tách scope riêng, không để chặn API thống kê.
- Sau bỏ Mentor: thẻ vocab chiếm hàng riêng hoặc chia cùng course theo responsive; không tạo placeholder Mentor/AI Mentor.
- Giữ React/TypeScript, httpClient, SVG và token hiện có; không thêm chart/query library mặc định. FE chưa có script test; nếu bổ sung Vitest/Playwright phải là quyết định dependency riêng, không tuyên bố đang có.

## 6. Tối ưu, kiểm thử và phát hành

### 6.1. Lộ trình

1. **Thu thời gian và lịch sử mục tiêu:** triển khai telemetry sớm để có dữ liệu tích lũy; API + hook và test concurrency/lifecycle.
2. **Tổng hợp số liệu nghiệp vụ:** Content batch metadata/Learning completion; Assessment snapshot/band; Library recall state + luồng ôn tối thiểu. Ba nhánh này có thể làm độc lập khi đã khóa contract.
3. **Ghép FE và nghiệm thu:** thay mock của tab chi tiết, bỏ Mentor, kiểm tra các widget và một buổi học từ đầu đến cuối.

Có thể phát hành từng card với availability rõ ràng; điều đó là mốc trung gian. Hoàn thành đầy đủ màn hình cần Library review và band eligibility, không đánh dấu done chỉ vì đã vẽ được chart.

### 6.2. Bộ tình huống nghiệm thu trọng yếu

| Tình huống | Kỳ vọng |
|---|---|
| Learner mới | Time chưa đo; progress/vocab rỗng; band không có; không tự hiện 5.5 |
| Lesson nhiều skill | Complete một lần cập nhật từng skill tương ứng, course chỉ +1 lesson |
| Retry submit/refresh | Counts không tăng đôi |
| 2 tab/2 device | Một lease nhận thời gian, tổng không vượt thời gian thực |
| Mất response sau commit | Retry cùng sequence trả receipt, không cộng lại |
| Idle/hidden/audio paused | Ngừng cộng theo policy; listening visible đang chạy vẫn được tính |
| Qua 00:00/đổi timezone | Chia đúng ngày, không sửa lịch sử đã chốt |
| Đổi goal | Target ngày cũ giữ nguyên; course filter không copy target toàn cục |
| Regrade v2 pending/completed | Pending giữ v1 có nhãn; completed thay v1, attempt count không tăng |
| Band fallback hoặc Speaking chưa đo | Không vẽ như measured; overall thiếu dữ liệu |
| 2 lần ôn đồng thời cùng nghĩa | Một update hợp lệ theo stateVersion; không thăng 2 cấp |
| Trùng nghĩa ở nhiều deck | Total tăng 1, không nhân số deck |
| User khác truy cập session/review | Bị từ chối trước đọc/ghi; không lộ nội dung |
| Content/Library lỗi | Card liên quan unavailable, các card còn lại hoạt động |
| Xóa/archive/publish thêm nội dung | Mẫu số và revision thay đổi theo policy, không lấy catalog cũ giả làm hiện tại |

Unit test công thức/state machine; MVC test validation/ownership; Testcontainers test migration, unique/lock/replay, SQL aggregate và query plan. FE chạy lint/typecheck/build, kiểm thử lifecycle/browser thực và bàn phím; nếu chưa có framework tự động thì ghi nhận manual, không tạo báo cáo test giả.

### 6.3. SLO đề xuất và kiểm tra tải

- Mục tiêu ban đầu để đo: 4 request aggregate, mỗi API p95 <500ms với dataset đại diện; dashboard usable <1.5s ở môi trường đã nêu rõ. Đây là acceptance target cần benchmark, không phải kết quả đã đạt.
- Dashboard không thực hiện chuỗi HTTP theo số lesson; response/chart tối đa 366 bucket hoặc 100 điểm/lượt. Latest/baseline/delta phải được tính trên toàn range kể cả timeline phân trang.
- Telemetry metrics: accepted/rejected/replayed/expired, lag, lastSuccessfulCapture; chỉ ID/code, không log đáp án, essay, transcript hay token.
- Nếu dùng outbox cho tính năng khác về sau, consumer phải idempotent vì retry/redelivery có thể trùng; không mặc định exactly-once. [RabbitMQ reliability](https://www.rabbitmq.com/docs/reliability)

### 6.4. Backfill và rollback

- Có thể tính current lesson counts từ dữ liệu hiện có; không dựng lịch sử thời gian hoạt động từ completedAt.
- Có thể truy xuất kết quả cũ với provenance đủ chứng minh; không tái chấm LLM hàng loạt hoặc lấy config hiện tại để giả snapshot quá khứ.
- Flashcard cũ có vocabularySenseId chỉ vào NEW sau khi learner thực sự chọn học hoặc quy tắc import được duyệt; không mặc định “đã thuộc”.
- Migration additive; bật ghi trước, quan sát, bật đọc sau. Rollback FE/endpoint bằng khả năng cấu hình hiện có hoặc thay đổi được duyệt, giữ bảng/dữ liệu mới, không chạy destructive down migration.
- Nếu phase chưa xong thì UI dùng empty/unavailable. Không fallback sang số mẫu trong production.

## 7. Quyết định còn mở

Hai quyết định đầu đã chốt với người dùng; các dòng còn lại là **khuyến nghị để duyệt khi triển khai**, không ngăn hoàn thành bản plan này.

| Vấn đề | Khuyến nghị | Đổi lựa chọn sẽ ảnh hưởng |
|---|---|---|
| Tiến độ kỹ năng | **Đã chốt: tỷ lệ lesson hoàn thành** | Không tự đổi sang mastery |
| Mentor | **Đã chốt: bỏ hoàn toàn** | Không tạo API/bảng/AI thay thế |
| Scope radar | Course đã bắt đầu; chọn riêng course được | Nếu toàn catalog, mẫu số lớn và nhiều 0 không hữu ích |
| Đơn vị vocab | Nghĩa từ canonical đã đưa vào học; dedupe cross-deck | Bao gồm card tự tạo cần định nghĩa item identity và đổi tên nhãn |
| Donut | Retained/total; dueCount riêng | Nếu muốn due/total phải đổi label, không dùng cùng tên |
| Lịch ôn | Thang 1/3/7/14 ngày, quên 10 phút; version hóa | Chưa có bằng chứng là tối ưu cho learner, cần thử nghiệm |
| Recall | Chọn objective nếu có câu trả lời; self-rating ghi rõ nguồn | Tự đánh giá dễ làm nhưng độ tin cậy thấp hơn |
| Active time | 30s heartbeat, 120s idle, hidden không tính; 1 lease/user | Người đọc lâu/background audio có thể bị thiếu thời gian |
| Band đủ điều kiện | Series theo loại đề, không dùng fallback; Overall cùng 4 skill | Có thể ban đầu Overall trống vì chưa có Speaking thật |
| Retention | Đề xuất raw 90 ngày, totals lâu hơn | Cần thống nhất quyền xóa và khả năng đối soát trước release |

## 8. Nguồn nghiên cứu

- [Bằng chứng source và đường dẫn liên quan](./reports/source-evidence.md).
- [MDN Page Visibility](https://developer.mozilla.org/en-US/docs/Web/API/Page_Visibility_API): browser báo visible/hidden; đây là tín hiệu kỹ thuật, không chứng minh sự chú ý.
- [MDN sendBeacon](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/sendBeacon), [fetch keepalive](https://developer.mozilla.org/en-US/docs/Web/API/Request/keepalive): lựa chọn flush cần custom Authorization và giới hạn lifecycle.
- [PostgreSQL 15 date/time](https://www.postgresql.org/docs/15/functions-datetime.html), [locking](https://www.postgresql.org/docs/15/explicit-locking.html): đối chiếu timezone và transaction lock của stack hiện có.
- [IELTS scoring](https://ielts.org/take-a-test/your-results/ielts-scoring-in-detail): đối chiếu cách tính Overall, không phải xác nhận chất lượng đề nội bộ.
- [RabbitMQ reliability](https://www.rabbitmq.com/docs/reliability): nguyên tắc replay/idempotency nếu mở rộng event sau này; không thêm broker hoặc streaming framework.
