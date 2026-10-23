# Phase 02: Màn "Đề & gói câu hỏi"

Người làm: **Codex**. Nhánh hiện tại: `feat/content-author-screens` (frontend, worktree `../frontend-claude`, theo chỉ dẫn mới nhất của người dùng).
Trạng thái: In progress — phần detail/draft/section đã triển khai local theo contract nhánh backend; toàn bộ phase và rollout còn chờ merge, API còn thiếu, checklist thật và toàn bộ suite xanh.

Repo: `frontend`. Phụ thuộc rollout: code và contract backend tương ứng đã merge, môi trường thật sẵn sàng. Người dùng đã cho phép triển khai local phần contract nhánh trước khi merge.

## Phạm vi hiện tại theo chỉ dẫn mới nhất 2026-10-23

- Chỉ triển khai trên nhánh `feat/content-author-screens` hiện tại: mở gói bằng UUID đã biết, detail, chọn version, tạo bản nháp rỗng, CRUD section và đổi thứ tự section bằng một PUT toàn bộ danh sách nguyên tử. Chỉ commit local; không push hoặc merge.
- Dùng mục **Implementation status (2026-10-23)** trong `backend/docs/contracts/content-package-authoring-v1.md` làm giới hạn thực tế. Detail câu hỏi hiện chỉ có `{id, questionVersionId, sortOrder, maxScore}`; không giả định có stem, trạng thái, purpose, passage hoặc audio.
- Trang gốc là form nhập UUID, không gọi API trước khi điều hướng bằng UUID hợp lệ. Catalog, tạo gói mới, thao tác câu hỏi, passage, audio và upload hiển thị disabled **Chờ backend**; không dựng API/list/dữ liệu giả hoặc dùng route tạo cũ thay thế.
- Preview, xuất bản và lên lịch không thuộc phạm vi partial được cho phép trong lượt này. Các mục thiết kế đầy đủ bên dưới giữ làm mục tiêu sau này; không dùng chúng để mở rộng implementation hiện tại.
- Chỉ dẫn này thay điều kiện cũ phải tạo nhánh riêng từ main và chờ toàn bộ phase 01 trước khi viết frontend; không bỏ điều kiện merge/kiểm thật trước rollout.

## Mục tiêu

Tác giả tạo gói đề, soạn bản nháp (section, câu hỏi, đoạn văn, audio), xem trước như học viên, xuất bản ngay hoặc lên lịch, và đọc được lỗi xuất bản.

## Đọc trước

- Contract: `content-package-authoring-v1.md`, `content-scheduled-publish-v1.md`, `question-publication-v1.md`.
- Mẫu cần chép theo: `src/features/admin/questions/` (`api/questionApi.ts`, `types.ts`, `pages/QuestionBankPage.tsx`, `pages/QuestionDetailPage.tsx`, `components/VersionCard.tsx`), `src/features/admin/lessons/components/QuestionChooser.tsx`, `src/features/admin/lib/useAdminResource.ts`, `src/features/admin/lib/adminErrors.ts`, `src/features/admin/components/AdminState.tsx`, `src/features/admin/pages/AdminPage.tsx`, `src/features/admin/admin.css`.
- Định tuyến: `src/features/admin/AdminRoutes.tsx`, `src/features/admin/adminMenu.ts`.

## File tạo mới

```text
src/features/admin/packages/
  types.ts                         # Zod schema + type theo contract
  api/packageApi.ts                # mọi call, parse bằng schema
  lib/packageLabels.ts             # nhãn tiếng Việt: loại gói, trạng thái, mã lỗi publish
  pages/PackagesPage.tsx           # danh sách + tạo gói
  pages/PackageEditorPage.tsx      # soạn một gói
  components/VersionPicker.tsx     # chọn version, tạo bản nháp
  components/SectionEditor.tsx     # một section: thông tin, đoạn văn, audio, câu hỏi
  components/PackagePreview.tsx    # hiển thị kết quả preview
  components/PublishPanel.tsx      # xuất bản ngay / lên lịch / hủy lịch / lỗi lịch
  pages/PackagesPage.test.tsx
  pages/PackageEditorPage.test.tsx
```

## Sửa

- `adminMenu.ts`: thêm `{ path: '/content-author/packages', label: 'Đề & gói câu hỏi', icon: ClipboardList, roles: ['CONTENT_AUTHOR'] }` sau mục "Ngân hàng câu hỏi".
- `AdminRoutes.tsx`: trong nhánh `isAuthorArea`, thêm `packages` (index → `PackagesPage`, `:packageId` → `PackageEditorPage`) theo đúng cách `questions` đang khai báo.
- `lessons/components/QuestionChooser.tsx`: thêm prop tùy chọn `purpose?: 'LEARNING' | 'EXAM' | 'PLACEMENT'` (mặc định `'LEARNING'`, giữ hành vi cũ), dùng trong key của `useAdminResource` và query `listQuestions`. Không đổi gì khác.

## API client (`packageApi.ts`)

Hàm, mỗi hàm `apiRequest(..., { auth: true })` và parse Zod:

`listPackages(filters, signal)`, `getPackage(id, signal)`, `createPackage(body)` (route mới `POST /api/content/admin/packages`, phase 01 mục 7; nhận `topicId`/`courseId`), `createDraftVersion(packageId, rulesJson)`, `addSection`, `updateSection`, `deleteSection`, `reorderSections`, `addSectionQuestion`, `removeSectionQuestion`, `reorderSectionQuestions`, `setPassage`, `clearPassage`, `setAudio`, `clearAudio`, `uploadAudio(file)` (multipart; xem `apiRequest` có nhận `FormData` chưa, nếu chưa thì làm theo cách trang upload ảnh flashcard/community đang gửi file), `previewVersion(packageId, versionId, signal)`, `publishVersion(packageId, versionId)` (route cũ `POST /api/content/packages/{id}/publish` body `{ versionId }`), `scheduleVersion(packageId, versionId, publishAt)`, `cancelSchedule`, `listSchedules(packageId, signal)`, `listPracticeTestSources(signal)` (`GET /api/content/practice-tests/sources`), `listLessonsForPicker` (dùng lại `fetchTopics`/`fetchLessons` của `lessons/api/lessonApi.ts`, không viết lại).

## Màn 1: `PackagesPage` (`/content-author/packages`)

- Bộ lọc: loại gói (select từ `PackageType`), trạng thái (`DRAFT`/`PUBLISHED`), ô tìm `code`/`title` (debounce 300 ms). Lọc gửi lên server qua query của `GET /api/content/admin/packages`.
- Bảng `adm-table`: Mã, Tiêu đề, Loại, Truy cập (FREE/PREMIUM), Trạng thái (badge), Version mới nhất, "Có bản nháp" (badge), Cập nhật lúc (định dạng `vi-VN`). Dòng là link sang editor. Mobile: bảng nằm trong `adm-table-wrap` cuộn ngang được.
- Form "Gói mới" (card thu gọn được): `code` (bắt buộc, ≤100), `title` (bắt buộc, ≤255), `packageType` (bắt buộc; chọn trong `PRACTICE_SET`, `PRACTICE_TEST`, `MOCK_TEST`, `PLACEMENT_TEST`, `QUIZ`, `TOPIC_TEST` "Đề cuối chủ đề", `COURSE_TEST` "Đề cuối khóa"; không có `LESSON`), `accessLevel` (FREE/PREMIUM, mặc định FREE). Ô phụ theo loại:
  - `PRACTICE_SET`: `lessonId` bắt buộc ở UI (chọn chủ đề rồi chọn bài; backend cho phép trống, UI vẫn bắt buộc).
  - `PRACTICE_TEST`: `sourceCode` **tùy chọn**, select từ `listPracticeTestSources` với lựa chọn đầu "Không (đề kiểm tra nội bộ của khóa)".
  - `TOPIC_TEST`: `topicId` bắt buộc (chọn khóa rồi chủ đề, dùng lại API đọc course/topic frontend đang gọi).
  - `COURSE_TEST`: `courseId` bắt buộc.
  Tạo xong: gọi luôn `createDraftVersion(id, "{}")` rồi chuyển sang editor của gói. Lỗi `INVALID_PACKAGE_TOPIC` "Chủ đề không hợp lệ cho đề cuối", `INVALID_PACKAGE_COURSE` "Khóa học không hợp lệ cho đề cuối".
- Trạng thái: đang tải, lỗi kèm "Thử lại", rỗng ("Chưa có gói nào. Tạo gói đầu tiên ở form trên.").

## Màn 2: `PackageEditorPage` (`/content-author/packages/:packageId`)

Bố cục (desktop 2 cột, mobile 1 cột):

1. **Đầu trang**: tiêu đề, mã, loại, badge trạng thái; link "Quay lại danh sách".
2. **`VersionPicker`**: danh sách version (số, trạng thái, `publishAt` nếu có). Mặc định chọn bản nháp nếu có, không thì version đang xuất bản. Nút "Tạo bản nháp mới" chỉ hiện khi không có bản nháp; lỗi `DRAFT_EXISTS` hiển thị "Gói đã có bản nháp". Version đang chọn lưu vào query `?version=` để tải lại giữ nguyên.
3. **Danh sách section** của version đang chọn, mỗi section một `SectionEditor`:
   - Thông tin: tiêu đề, kỹ năng, phần (`partNo` 1..4), thời gian (phút ở UI, đổi sang giây khi gửi), hướng dẫn. Nút "Lưu section".
   - Đoạn văn (Reading): `textarea`, nút "Lưu đoạn văn" và "Gỡ đoạn văn" (hỏi xác nhận).
   - Audio (Listening): nút "Tải file audio" (`input type="file" accept="audio/mpeg,audio/mp4,audio/wav,audio/ogg,.mp3,.m4a,.wav,.ogg"`), kiểm phía client ≤ 25 MB trước khi gửi. Gọi `uploadAudio(file)` (`POST /api/content/admin/media/audio`, `FormData`), hiện trạng thái "Đang tải lên…". Nhận `mediaUrl`, đặt vào một `<audio>` ẩn để đọc `duration` ở sự kiện `loadedmetadata` (làm tròn lên giây), rồi tự điền thời lượng. Ô transcript (tùy chọn). Nút "Lưu audio" gửi `PUT …/audio` với `mediaReference` vừa nhận. Đường phụ thu gọn trong `<details>` "Dùng file có sẵn": ô nhập key và thời lượng tay (cho file seed cũ). Sau khi lưu, trình `<audio controls>` phát `mediaUrl`. Nút "Gỡ audio". Lỗi: `UNSUPPORTED_AUDIO` "File không phải audio hợp lệ (mp3, m4a, wav, ogg)", `413` "File lớn hơn 25 MB", `STORAGE_UNAVAILABLE` "Kho lưu trữ chưa sẵn sàng, thử lại sau".
   - Câu hỏi: danh sách theo thứ tự, mỗi dòng có stem rút gọn, badge cảnh báo nếu câu `DRAFT` ("Câu chưa xuất bản") hoặc purpose sai loại gói ("Sai mục đích: cần EXAM"), nút lên/xuống (reorder), nút gỡ. Thêm câu bằng `QuestionChooser` với `purpose` = `EXAM` cho `MOCK_TEST` **và `PRACTICE_TEST`**, `PLACEMENT` cho `PLACEMENT_TEST`, `LEARNING` cho loại khác (khớp `PublishContentPackageUseCase`). Lỗi `QUESTION_ALREADY_IN_VERSION` → "Câu này đã có trong đề".
   - Nút "Xóa section" (hỏi xác nhận).
   - Lên/xuống thứ tự section.
   - Form "Thêm section" ở cuối.
   - Version không phải `DRAFT`: mọi control ghi ẩn hoặc `disabled`, hiện dòng "Bản đã xuất bản chỉ xem. Tạo bản nháp mới để sửa."
4. **`PublishPanel`** (cột phải trên desktop, cuối trang trên mobile), chỉ cho bản nháp:
   - "Xem trước" mở `PackagePreview` (tải `previewVersion`): mỗi section hiện đoạn văn, audio player, từng câu (stem, lựa chọn), và phần "Đáp án" (answerSpec, giải thích, transcript) trong `<details>` để không lộ khi trình chiếu.
   - "Xuất bản ngay" (hỏi xác nhận). Với `TOPIC_TEST`/`COURSE_TEST`, hộp xác nhận nói thêm: "Đề sẽ vào nhóm đề thi của chủ đề/khóa này ngay khi xuất bản; học viên thi lại sẽ có thể nhận đề này."
   - Lỗi `INVALID_CONTENT_STATE` hiện cả message server trả (ví dụ essay thiếu `passBand`, câu không tự chấm được) dưới nhãn chung.
   - "Lên lịch": `input type="datetime-local"` (giờ địa phương), đổi sang ISO UTC khi gửi; phải ở tương lai (kiểm phía client, server cũng kiểm). "Hủy lịch" khi đã có lịch.
   - Hiện `lastPublishError` từ `listSchedules` bằng nhãn tiếng Việt.
   - Ghi chú cố định: "Gói tạo ở đây chưa có nhóm dạng đề nên không xuất hiện khi học viên lọc theo dạng đề."

Nhãn lỗi (`packageLabels.ts`, dùng `adminErrorCode`): `QUESTION_ALREADY_USED` "Có câu đã thuộc một đề khác đã xuất bản", `QUESTION_PURPOSE_MISMATCH` "Có câu sai mục đích với loại đề", `INVALID_PACKAGE_LESSON` "Bài học của bộ Practice không hợp lệ", `INVALID_CONTENT_STATE` "Nội dung chưa đủ điều kiện xuất bản", `VERSION_NOT_DRAFT` "Bản này đã xuất bản, không sửa được", `DRAFT_EXISTS`, `QUESTION_ALREADY_IN_VERSION`, `PUBLISH_FAILED` "Xuất bản theo lịch thất bại, sẽ thử lại". Mã khác: dùng `adminErrorMessage`.

Sau mỗi thao tác ghi thành công: tải lại `getPackage` (gọi `reload` của `useAdminResource`), không tự vá state cục bộ. Nút thao tác `disabled` khi đang gửi; lỗi hiện ngay dưới khối liên quan (`adm-alert adm-alert--error`, `role="alert"`).

## Test (Vitest + Testing Library, mock `../api/packageApi`)

- `PackagesPage`: hiện bảng từ dữ liệu mock; lọc theo loại gọi API với query đúng; form chỉ hiện ô bài học khi chọn `PRACTICE_SET`, ô chủ đề khi `TOPIC_TEST`, ô khóa khi `COURSE_TEST`, ô nguồn (tùy chọn) khi `PRACTICE_TEST`; tạo `TOPIC_TEST` gửi `topicId`; tạo gói gọi `createPackage` rồi `createDraftVersion` và điều hướng; trạng thái rỗng và lỗi.
- Upload audio: file > 25 MB bị chặn không gọi API; upload thành công rồi `loadedmetadata` (giả lập bằng `fireEvent`) điền thời lượng; lỗi `UNSUPPORTED_AUDIO` hiện nhãn tiếng Việt.
- `PackageEditorPage`: mặc định chọn bản nháp; version đã xuất bản thì không có nút ghi; thêm câu gửi đúng `purpose` theo loại gói, gồm `EXAM` cho `PRACTICE_TEST` (kiểm prop truyền vào `QuestionChooser` hoặc query); xác nhận xuất bản đề cuối có câu cảnh báo; badge cảnh báo câu `DRAFT`; lỗi `QUESTION_PURPOSE_MISMATCH` khi publish hiện nhãn tiếng Việt; lên lịch gửi ISO UTC; nút "Tạo bản nháp mới" ẩn khi đã có bản nháp.
- `adminMenu`: CONTENT_AUTHOR thấy mục mới, ADMIN không thấy (thêm vào test menu hiện có nếu có, không thì tạo `adminMenu.test.ts`).
- `QuestionChooser`: test hiện có vẫn xanh; thêm một test cho `purpose="EXAM"`.

## Kiểm thủ công

Đăng nhập `content@gmail.com`: tạo gói `TOPIC_TEST` (chọn một chủ đề có sẵn) → bản nháp có sẵn → thêm section Reading, dán đoạn văn, gắn 3 câu `PUBLISHED` `LEARNING` → xem trước → xuất bản → tạo bản nháp v2 → lên lịch 2 phút sau → chờ, tải lại thấy v2 `PUBLISHED`. Thử gắn câu `EXAM` vào gói `TOPIC_TEST` rồi publish → thấy lỗi sai mục đích. Tạo section Listening, tải một file mp3 thật, nghe lại được, xem trước thấy audio; tải file ảnh đổi đuôi .mp3 → bị từ chối. Đo trên 390 px không cuộn ngang trang (bảng được cuộn trong khung riêng).

## Hoàn thành khi

`npm run typecheck`, `npm run lint`, `npm test`, `npm run build` xanh; checklist thủ công đã chạy hoặc ghi rõ bước nào không chạy được.

## Kết quả

- 2026-10-23: đã fetch backend origin/main (`c600b3f3`). Phase 01 vẫn Pending; `git cat-file -e origin/main:docs/contracts/content-package-authoring-v1.md` xác nhận contract mới chưa có trên main (bản local viết dở không được dùng làm contract).
- Chưa tạo nhánh `feat/content-package-screens`, chưa viết API/schema/UI, chưa chạy test/checklist của phase này. Chờ phase 01 hoàn tất và merge trước.

### Cập nhật hiện tại 2026-10-23 — thay trạng thái chờ toàn bộ trước đó

- Người dùng đã cho phép triển khai partial contract ngay trên `feat/content-author-screens`. Phạm vi ở đầu file là chỉ dẫn hiện hành; lịch sử kiểm main `c600b3f3` ở trên không còn là điều kiện chặn việc viết local phần được cho phép.
- Đã triển khai frontend detail/version picker/bản nháp rỗng/section CRUD/PUT thứ tự toàn bộ theo contract nhánh. Không tuyên bố các API catalog, câu hỏi, passage, audio, upload hoặc tạo gói mới đã có; không thêm chức năng xuất bản/lên lịch trong lượt này.
- Đã sửa tương thích DTO thực tế khi rà soát: request `sortOrder` và `timeLimitSeconds` chấp nhận signed int32 như backend; sửa metadata giữ được thứ tự âm hoặc thời gian 0/âm đã tồn tại. Ô nhập thời lượng phút mới vẫn yêu cầu số dương. Có test hồi quy cho các giá trị hiện hữu này.
- Kiểm chứng cuối lượt: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua (exit 0). Nhóm package API/index/editor/SectionEditor, AdminRoutes và CoursesPage: 6 file / 84 test qua. Toàn bộ `npm.cmd test` exit 1: 102/103 file qua, 670/672 test qua; 2 lỗi ngẫu nhiên đã biết ở `flashcards/lib/owl-reactions.test.ts:87` (confused) và `:109` (tired), ngoài phạm vi. Không sửa/bỏ test, không chạy lại để đổi kết quả.
- Build Vite qua, còn cảnh báo kích thước chunk sẵn có. Không tuyên bố tất cả cổng kiểm chứng xanh hoặc phase hoàn thành. Commit local: `e8fc46a feat(packages): edit package versions and sections`.
- Gateway 8080/18080 timeout trong lượt này; checklist với backend thật và màn 390px chưa chạy. Không tuyên bố partial đã rollout hoặc toàn bộ phase hoàn thành. [PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29) đang nháp; thay đổi mới chỉ được phép commit local.

## Việc cho bên kia

- Merge phần backend/contract detail, tạo draft, CRUD section và đổi thứ tự nguyên tử đã có trên nhánh, rồi kiểm live trước rollout; không yêu cầu triển khai lại các API này.
- Phần đầy đủ còn thiếu theo Implementation status: catalog `GET /api/content/admin/packages`, section questions, passage/audio, audio upload và tạo `TOPIC_TEST`/`COURSE_TEST`. Chỉ mở tiếp UI khi API tương ứng có contract triển khai được xác nhận và được phép mở rộng phạm vi.
