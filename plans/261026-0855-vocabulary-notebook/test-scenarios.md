# Kịch bản test chức năng — Sổ từ vựng

Dữ liệu: V4 (300 từ) + V15/V16 (từ bài học) + V20 (16 từ làm giàu) + **V22 (48 từ làm giàu thêm)** = 64 sense có CEFR/chủ đề.
Sau V22: 6 chủ đề (Education 12, Environment 11, Health 10, Society 11, Technology 11, Work & Business 9 từ) và 6 bộ BAND
(A1 1, A2 4, B1 20, B2 25, C1 13, C2 1 từ). Tổng 128 ví dụ, 7 từ AWL (`data`, `method`, `research`, `income`, `network`,
`diversity`, `assignment`). Nội dung demo do AI soạn, chờ giám khảo IELTS duyệt.

## 0. Chuẩn bị

1. `docker compose up -d postgres rabbitmq`, chạy library-service (Flyway tự áp V22, **không cần** xóa DB).
2. Hai tài khoản: học viên `CUSTOMER` (A) và `CUSTOMER` thứ hai (B); một `CONTENT_AUTHOR` hoặc `ADMIN`.
   Đăng nhập qua Gateway `POST /auth/login` lấy token. Khách (không token) dùng cho nhóm công khai.
3. Gán biến: `GW=http://localhost:8080`, `TA`, `TB`, `TAD` (token). Lấy `SENSE_ID` bằng nhóm 1.

Kiểm DB nhanh:

```sql
select count(*) from vocabulary_senses where cefr_level is not null;            -- 64
select t.name, count(*) from vocabulary_senses s join vocabulary_topics t on t.id=s.topic_id group by 1;
```

## 1. Từ điển công khai (không token)

| # | Bước | Kỳ vọng |
| --- | --- | --- |
| 1.1 | `GET $GW/api/content/vocabulary?size=20` | 200, `items` ≤ 20, có `total` > 300 |
| 1.2 | `...?q=income` | 1 từ, `academicWordList=true` ở chi tiết |
| 1.3 | `...?lang=VI&q=thất nghiệp` | tìm ra `unemployment` |
| 1.4 | `...?cefr=C1` | mọi item có sense C1 (vd literacy, syllabus, automation) |
| 1.5 | `...?topicId=<Health>` | chỉ từ chủ đề Health (obesity, vaccine, chronic...) |
| 1.6 | `GET .../vocabulary/{id}` của `tuition` | `ipaUk` `/tjuˈɪʃn/`, `ipaUs` `/tuˈɪʃn/`, `cefrLevel=B2`, `bandRange {5.5,6.5}`, 2 collocation, 1 synonym `fees`, 1 commonMistake, 2 example có `translationVi`, `counts` khớp |
| 1.7 | chi tiết `climate` | `cefrLevel=B1`, `bandRange {4.0,5.0}`, `frequencyRank=null` |
| 1.8 | chi tiết một từ V4 chưa làm giàu (vd `waste`) | 200, `cefrLevel=null`, `bandRange=null`, mảng con rỗng, các trường cũ vẫn có |
| 1.9 | `GET .../vocabulary/suggestions?q=pro` | ≤ 8 gợi ý, đều bắt đầu `pro` (productivity, profit, promotion, poverty...) |
| 1.10 | `.../suggestions?q=` (rỗng) và `q` 51 ký tự | 400 |
| 1.11 | `.../vocabulary/topics` | 6 chủ đề, `wordCount` khớp bảng đầu file |
| 1.12 | `.../vocabulary/sets?kind=BAND`, `?kind=TOPIC` | 6 bộ mỗi loại; `sets/{id}` trả topic summary; `sets/{id}/words?size=5` phân trang |
| 1.13 | `.../mindmap?topicId=<Environment>` và `?itemId=<climate>` | `nodes` ≤ 80, mỗi edge trỏ tới node tồn tại; thiếu cả hai tham số → 400 |
| 1.14 | `.../vocabulary/{id-không-tồn-tại}` / `/abc` | 404 / 400 |
| 1.15 | `.../vocabulary/search?q=a` và `/senses` không token | **401** (không được mở công khai) |
| 1.16 | `POST .../vocabulary` không token | 401 |

## 2. Sổ tay (học viên A, có token)

| # | Bước | Kỳ vọng |
| --- | --- | --- |
| 2.1 | `POST .../flashcards/notebook/entries {"vocabularySenseId": SENSE(tuition)}` | **201**, `level=UNKNOWN`, `leitnerBox=1`, `lemma=tuition` |
| 2.2 | Gửi lại đúng request | **200**, cùng `flashcardId` (không tạo bản thứ hai) |
| 2.3 | Gửi 2 request song song | 1 bản ghi; không 500 |
| 2.4 | sense không tồn tại | 404 |
| 2.5 | Lưu thêm 10 từ (mỗi chủ đề vài từ) rồi `GET .../entries` | `total=11`, mặc định sort `RECENT` |
| 2.6 | `?q=tui`, `?sort=ALPHABET`, `?size=5&page=1` | lọc/sắp/phân trang đúng; `size=51` → 400 hoặc kẹp ≤ 50 |
| 2.7 | `?topicId=<Education>`, `?setId=<B2 set>` | chỉ mục thuộc chủ đề/bộ đó; có `cefrLevel`, `topicName` |
| 2.8 | `GET .../summary` | `total=11`, `byLevel.UNKNOWN=11`, `dueNow=11`, `catalogTotal` ≥ 300; `?timezone=Foo` → 400 |
| 2.9 | `PUT .../entries/{id}/level {"level":"MASTERED","requestId":<uuid>}` | 204; entry thành box 4, `nextReviewAt` > hiện tại |
| 2.10 | Lặp đúng `requestId` | 204, không đổi thêm |
| 2.11 | `level=UNKNOWN` | đến hạn ngay (`dueNow` tăng) |
| 2.12 | `?level=MASTERED&level=LEARNING` | đúng tập; `?due=true` chỉ mục đến hạn |
| 2.13 | `GET .../saved?senseIds=<id1>,<id2>` | trả mảng id đã lưu; 101 id → 400 |
| 2.14 | Ghi chú: `POST` note `sourceType=VOCABULARY_SENSE` 300 ký tự / 301 ký tự | 2xx / 400; entry hiện `note` |
| 2.15 | `DELETE .../flashcards/{id}` rồi lưu lại cùng sense | xóa mềm; lưu lại tạo/khôi phục mục hợp lệ, không vi phạm index unique |
| 2.16 | **Cô lập**: B gọi `PUT level` / `DELETE` trên flashcardId của A | 404; B `GET entries` không thấy dữ liệu của A |
| 2.17 | Không token gọi mọi route notebook | 401 |

## 3. Ôn tập và tiến độ

| # | Bước | Kỳ vọng |
| --- | --- | --- |
| 3.1 | `POST .../flashcards/{id}/reviews {"correct":true,"requestId":<uuid>,"sessionId":S}` | box tăng; `reviewCount`+1; có 1 dòng `flashcard_review_events` |
| 3.2 | Lặp `requestId` cuối | không thêm event |
| 3.3 | `correct:false` | box về 1 (UNKNOWN), đến hạn |
| 3.4 | Ôn 5 thẻ cùng `sessionId=S`, 4 đúng 1 sai | `GET .../recent-result` → `total=5, correct=4, percent=80`; chưa có session → **204** |
| 3.5 | `GET .../weekly-goal` | mặc định `target=120`; `days` 7 ngày thứ Hai→Chủ nhật; ngày có ôn `achieved=true` |
| 3.6 | `PUT .../weekly-goal {"targetWords":5}` / `{"targetWords":1001}` / `{"targetWords":200}` | 400 / 400 / 200 và đọc lại thấy 200 |
| 3.7 | `GET .../vocabulary-map?currentCefr=B2` | 6 dòng A1..C2, `total` mỗi band khớp 1.x; `currentBand=B2`; `currentCefr=Z9` → 400 |
| 3.8 | `GET .../topics-progress` | 6 chủ đề; chủ đề vừa ôn có `status=IN_PROGRESS`, `current.nextWords` ≤ 5 |
| 3.9 | `GET .../sets/{setId}/progress`; setId sai | số liệu khớp 3.7; 404 |
| 3.10 | `GET .../achievements` | 3 mục (chuỗi 7 ngày, hoàn thành chủ đề, 300 từ thuộc), `achieved=false`, `progress` đúng |
| 3.11 | Không được cộng XP/điểm | learning-service và access-service không có thay đổi sau các bước 2-3 |

## 4. Admin / tác giả (`CONTENT_AUTHOR` hoặc `ADMIN`)

| # | Bước | Kỳ vọng |
| --- | --- | --- |
| 4.1 | `PUT .../admin/vocabulary/senses/{id} {"cefrLevel":"C1","topicId":...}` bằng `CUSTOMER` | 403; không token 401 |
| 4.2 | Cùng request bằng admin | 200; chi tiết công khai đổi `bandRange {7.0,8.0}` |
| 4.3 | `cefrLevel":"D9"` | 400 |
| 4.4 | `PUT .../senses/{id}/collocations` mảng 31 phần tử | 400; 2 phần tử → 200 và thay hoàn toàn danh sách cũ |
| 4.5 | `PUT .../items/{id}` `frequencyRank:-1` | 400 |
| 4.6 | `POST /topics` slug trùng | 409 hoặc 400 theo handler; slug mới → 201 |
| 4.7 | `POST /sets` `kind=BAND` thiếu `cefrLevel`; `kind=TOPIC` thiếu `topicId` | 400 |
| 4.8 | `PUT /sets/{id}/items` có id trùng / 501 phần tử / sense không tồn tại | 400 |
| 4.9 | `DELETE /topics/{id}` | 204, chủ đề biến mất khỏi `/vocabulary/topics` công khai (INACTIVE) |

## 5. Dữ liệu và migration

| # | Bước | Kỳ vọng |
| --- | --- | --- |
| 5.1 | DB mới: chạy toàn bộ V1..V22 | không lỗi (đã chạy thử trên DB tạm: 64 sense làm giàu, 128 ví dụ) |
| 5.2 | DB đã có V21: khởi động lại service | chỉ V22 được áp |
| 5.3 | Áp lại thân V22 thủ công | không nhân đôi collocation/ví dụ (các sense đã có CEFR bị bỏ qua) |
| 5.4 | Admin sửa CEFR một từ V22 rồi áp lại V22 | giá trị admin không bị ghi đè |
| 5.5 | `select lemma from vocabulary_items where frequency_rank is not null` | rỗng |
| 5.6 | Mỗi sense của V22 có đúng 2 collocation, 1 relation, 1 mistake, 2 example | `group by sense_id` không lệch |

## 6. Giao diện (FE)

| # | Màn | Kỳ vọng |
| --- | --- | --- |
| 6.1 | `/vocabulary` (khách) | danh sách từ thật, không còn 6 từ mock; lọc CEFR/chủ đề, tìm EN/VI, gợi ý dưới ô tìm |
| 6.2 | Chi tiết từ | tab Definitions/Collocations/Thesaurus/Common Mistakes/Examples có số đếm khớp API; IPA UK/US; nhãn AWL cho `income`; ẩn tần suất khi null |
| 6.3 | Bấm "Lưu vào sổ tay" khi chưa đăng nhập | chuyển đăng nhập, quay lại giữ từ cần lưu |
| 6.4 | Sổ tay | thống kê, lọc mức, tìm, sắp xếp, phân trang; đổi mức cập nhật ngay; trạng thái chờ/rỗng/lỗi |
| 6.5 | Phiên ôn nhiều chế độ | chạy bằng API thật, kết quả khớp `recent-result` |
| 6.6 | Bản đồ từ vựng / Overview | **không còn crash `band.toFixed`**; hiện "IELTS Band ~x.x" khi có `estimatedBand.value`, "Dự kiến" khi chưa có |
| 6.7 | Bàn phím, mobile 375px, `prefers-reduced-motion` | thao tác được bằng Tab/Enter; không tràn ngang; animation tắt |

## 7. Lệnh tự động

```powershell
mvn -q -pl services/library-service -am test      # cần Docker (Testcontainers)
mvn -q -pl infra/api-gateway test                 # path công khai mới + /search vẫn 401
```

```powershell
cd "d:\Fall2026\New folder\FE\frontend"
npm run lint; npm run typecheck; npm run build; npm test
```

## Câu hỏi mở

- Người duyệt nội dung demo V20/V22 (CEFR, ví dụ, lỗi thường gặp) là ai, trước production?
- Có nguồn tần suất COCA hợp lệ không? Chưa có thì `frequency_rank` giữ null.
- Mục 2.15 (lưu lại sau khi xóa mềm) và 4.6 (mã lỗi slug trùng) chưa kiểm hành vi thực tế trong code — xác nhận khi chạy.
