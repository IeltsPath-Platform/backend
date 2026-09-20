---
type: feature-spec-merged
status: approved-design
created: 2026-09-18
sources:
  - 01-dac-ta-tinh-nang.md
  - 260913-ielts-platform-feature-tree.md
conflict-policy: dac-ta-wins-except-mobile-exam
taxonomy: option-2-domain
scope: mvp-plus-explicit-deferred
---

# Đặc tả tổng hợp — Nền tảng luyện IELTS

> **SSOT scope** · Gộp đặc tả chi tiết + cây tính năng A+C · Taxonomy domain mới (Option 2)

Người học làm được gì · màn hình có gì · hệ thống xử lý ra sao · client nào sở hữu · MVP hay hoãn.

**Nguồn ưu tiên nội dung:** `01-dac-ta-tinh-nang.md` (kể cả cập nhật 14·09).  
**Nguồn bổ sung ownership / admin / nền tảng:** `260913-ielts-platform-feature-tree.md`.  
**Ngoại lệ đã chốt 18·09:** Mobile **không** làm thi (theo cây; ghi đè đặc tả §6.4 cũ).

---

## Cách đọc mỗi leaf

| Trường | Ý nghĩa |
| ------ | -------- |
| **Trạng thái** | Đã có — giữ nguyên · Đã có — cần mở rộng · Làm mới hoàn toàn |
| **Client** | Web người học · Mobile · Admin · Shared (backend/dịch vụ) |
| **Phạm vi** | MVP · Sau MVP / cần kiểm chứng |
| **Nguồn** | `Đ§x.y` = đặc tả · `T§x` = feature tree |

---

## Conflict đã resolve

| Chủ đề | Quyết định | Lý do |
| ------ | ---------- | ----- |
| Mobile làm thi thử / mô phỏng kỳ thi | **Không** — chỉ Web | User chốt 18·09; UX thi dày phù hợp web |
| Mobile xem kết quả / phản hồi / nộp ghi âm Speaking ngoài ngữ cảnh thi đầy đủ | Có (vòng học hằng ngày + xem lại) | Tree: mobile chính daily loop; nộp W/S bất đồng bộ |
| Gia sư & lớp học | MVP mở rộng (theo đặc tả) | Đặc tả thắng; tree từng hoãn marketplace |
| Lộ trình | FSRS + cây chủ đề + AI sinh bài + duyệt | Đặc tả 14·09; Adaptive “theo luật” = lớp hỗ trợ kế hoạch ngày |
| Game / phát âm / sổ tay / flashcard | Giữ đủ từ đặc tả | Tree thiếu |
| Admin chi tiết (audit, retry chấm, chi phí AI…) | Lấy từ tree, gắn trạng thái Làm mới | Đặc tả gọn quá |

---

## Mười domain

1. [Danh tính & quyền lợi](#1--danh-tính--quyền-lợi)
2. [Học & game hằng ngày](#2--học--game-hằng-ngày)
3. [Luyện thi IELTS (Web)](#3--luyện-thi-ielts-web)
4. [Lộ trình & thành thạo](#4--lộ-trình--thành-thạo)
5. [Thư viện & sổ tay](#5--thư-viện--sổ-tay)
6. [Cộng đồng & thi đua](#6--cộng-đồng--thi-đua)
7. [Kho nội dung & nhãn kiến thức](#7--kho-nội-dung--nhãn-kiến-thức)
8. [Phản hồi AI Writing/Speaking](#8--phản-hồi-ai-writingspeaking)
9. [Admin & vận hành nền tảng](#9--admin--vận-hành-nền-tảng)
10. [Ngoài MVP / cần kiểm chứng](#10--ngoài-mvp--cần-kiểm-chứng)

---

## 1 · Danh tính & quyền lợi

### 1.1 · Tài khoản & đồng bộ tiến độ — Đã có — cần mở rộng

| | |
| -- | -- |
| **Mô tả** | Đăng ký · đăng nhập · quên mật khẩu · hồ sơ (tên, ảnh, band hiện tại, mục tiêu). Chuyển lưu local → server; **bắt buộc** nhập tiến độ cũ từ máy lên tài khoản |
| **Client** | Web hỗ trợ · Mobile chính (onboarding) · Admin hỗ trợ · Shared chính |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§6.1 · T§1 |

### 1.2 · Auth nâng cao (OAuth) — Làm mới

| | |
| -- | -- |
| **Mô tả** | Email/mật khẩu + OAuth; tùy chọn quyền riêng tư và đồng ý |
| **Client** | Shared chính · Web/Mobile hỗ trợ |
| **Phạm vi** | MVP |
| **Nguồn** | T§1 |

### 1.3 · Thiết lập mục tiêu học — Làm mới

| | |
| -- | -- |
| **Mô tả** | Band mục tiêu · ngày thi · thời gian học có thể dành ra |
| **Client** | Mobile chính · Web hỗ trợ · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | T§1 · T mindmap Mobile |

### 1.4 · Quyền lợi Free / Pro / Pro+ — Làm mới

| | |
| -- | -- |
| **Mô tả** | Hạn mức (lượt thi, lượt AI sinh bài, trợ lý RAG, v.v.) theo gói |
| **Client** | Shared chính · Admin cấu hình · Web/Mobile enforce |
| **Phạm vi** | MVP |
| **Nguồn** | T§1 · T§6 quản trị gói |

### 1.5 · Ứng dụng di động (shell) — Làm mới

| | |
| -- | -- |
| **Mô tả** | App native/hybrid dùng chung tài khoản & tiến độ. **Phạm vi màn hình:** học chính · game · lộ trình · tra cứu · cộng đồng · nhắc · xem phản hồi. **Không** gồm làm đề thi thử / mô phỏng kỳ thi đầy đủ |
| **Client** | Mobile chính |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§6.4 (đã chỉnh) · T ma trận |
| **Lợi thế native** | Micro luyện nói · push · offline nhẹ |

---

## 2 · Học & game hằng ngày

Phần người học dùng mỗi ngày. **Mobile chính**; Web hỗ trợ xem/tiếp tục khi phù hợp.

### 2.1 · Luyện phát âm — Đã có — giữ nguyên (mở rộng micro sau)

| | |
| -- | -- |
| **Mô tả** | Lộ trình âm đơn → nối âm → ngữ điệu; dạng tương tác. Hiện: nghe & chọn; chưa chấm qua micro |
| **Client** | Web/Mobile |
| **Phạm vi** | MVP giữ; chấm micro = sau nếu cần |
| **Nguồn** | Đ§1.1 |

### 2.2 · Game Box (4 game) — Đã có — giữ nguyên

| | |
| -- | -- |
| **Mô tả** | Mưa từ · Ghép từ · Nghe chọn · Bắt từ. Chơi xong chưa lưu điểm / xếp hạng → phụ thuộc domain 6 BXH |
| **Client** | Web/Mobile |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§1.2 |

### 2.3 · Game gõ từ vựng — Làm mới

| | |
| -- | -- |
| **Mô tả** | Gõ đúng từ/cụm để “bắn”; chế độ 60s / qua màn. Nguồn từ: sổ tay · vừa tra · band IELTS |
| **Client** | Web/Mobile |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§1.3 |

### 2.4 · Kế hoạch học hằng ngày — Làm mới

| | |
| -- | -- |
| **Mô tả** | Bài luyện ngắn theo điểm yếu · khớp thời gian rảnh · trạng thái hoàn thành. Đầu vào từ domain 4 + luật nhẹ (thời gian, streak) |
| **Client** | Mobile chính · Web hỗ trợ · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | T§4 · Đ§3 |

### 2.5 · Nghe 10 phút mỗi ngày — Làm mới

| | |
| -- | -- |
| **Mô tả** | Bài nghe tuyển chọn ~10 phút |
| **Client** | Mobile chính (Web không bắt buộc MVP) |
| **Phạm vi** | MVP |
| **Nguồn** | T§4 |

### 2.6 · Sổ tay lỗi sai (từ chấm bài) — Làm mới

| | |
| -- | -- |
| **Mô tả** | Lỗi ngữ pháp · từ vựng · mục phản hồi Speaking; xem lại & luyện tiếp |
| **Client** | Mobile chính · Web hỗ trợ |
| **Phạm vi** | MVP |
| **Nguồn** | T§4 |

### 2.7 · Streak & nhắc học — Làm mới

| | |
| -- | -- |
| **Mô tả** | Chuỗi ngày học; nhắc khi kiến thức đến hạn ôn hoặc bỏ học quá N ngày. Kênh: in-app · email · push. User chọn giờ / tắt kênh |
| **Client** | Mobile chính (push) · Web in-app/email · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§3.5 · T§4 |

---

## 3 · Luyện thi IELTS (Web)

Dữ liệu đề do thầy cung cấp. **Web người học = chính. Mobile = không làm thi.**

### 3.1 · Bài kiểm tra đầu vào 4 kỹ năng — Làm mới / mở rộng

| | |
| -- | -- |
| **Mô tả** | Đánh giá R/L; nộp W/S chấm bất đồng bộ; band ước tính + hồ sơ kỹ năng |
| **Client** | Web chính · Mobile chỉ xem kết quả · Admin theo dõi · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | T§2 |

### 3.2 · Làm đề thi thử (thư giãn / thi thật) — Đã có — cần mở rộng

| | |
| -- | -- |
| **Mô tả** | L/R/W/S format IELTS; đề rút gọn + đầy đủ. Thư giãn: không giờ, hiện đáp án ngay. Thi thật: đếm giờ chuẩn, nộp mới chấm. Mở rộng: thêm đề thầy · nhãn kiến thức · lưu server |
| **UI thi** | Timer · autosave/khôi phục · điều hướng phần · highlight · đếm từ · split view · xác nhận nộp |
| **Client** | **Web chính · Mobile không bao gồm** · Admin hỗ trợ nội dung · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§2.1 · T§2 trình làm bài |

### 3.3 · Luyện theo dạng câu hỏi — Đã có — cần mở rộng

| | |
| -- | -- |
| **Mô tả** | Luyện một dạng (MCQ, Matching Headings, Gap Fill…). Điểm đến khi lộ trình (4.x) đẩy bài |
| **Client** | Web chính · Mobile có thể bài luyện ngắn cùng dạng (không phải full exam) · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§2.2 |

### 3.4 · Phân tích lỗi sai sau bài — Làm mới

| | |
| -- | -- |
| **Mô tả** | Điểm tổng/kỹ năng · thời gian · so lần trước · từng câu sai (đáp án, giải thích, nhãn KT, nút luyện) · top 3–5 điểm yếu. Phụ thuộc nhãn kiến thức (7.2) |
| **Client** | Web chính · Mobile xem kết quả/tóm tắt |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§2.3 · T§2 Kết quả |

### 3.5 · Chấm R/L tự động; nộp W/S — Đã có — cần mở rộng

| | |
| -- | -- |
| **Mô tả** | Auto-grade Reading/Listening; Writing/Speaking → hàng đợi AI (domain 8) |
| **Client** | Web chính cho ngữ cảnh thi · Shared chấm |
| **Phạm vi** | MVP |
| **Nguồn** | T§2 · Đ§2 |

---

## 4 · Lộ trình & thành thạo

Điểm khác biệt sản phẩm. Shared engine; bề mặt Mobile chính cho vòng ngày, Web cho tiến độ sâu.

### 4.1 · Đo mức thành thạo (FSRS) — Đã có — cần mở rộng

| | |
| -- | -- |
| **Mô tả** | Đơn vị = điểm kiến thức (từ · ngữ pháp · dạng câu · kỹ năng). Lưu: mức · đúng/sai · lần ôn · hạn ôn. Cập nhật từ thi · luyện · game · quiz. Tech: `ts-fsrs` thay Leitner |
| **Client** | Shared chính · Web/Mobile consume |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§3.1 |

### 4.2 · Cây chủ đề + cổng kiểm tra — Làm mới

| | |
| -- | -- |
| **Mô tả** | Chủ đề cố định (vd Daily Life → …). Cuối chủ đề = cổng. Chưa đạt → chỉ sinh/luyện phần yếu. Đạt → mở chủ đề sau; KT cũ quay lại theo FSRS |
| **Client** | Mobile/Web · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§3.2 |

### 4.3 · AI goi y bài luyện theo điểm yếu — Làm mới

| | |
| -- | -- |
| **Mô tả** | Ưu tiên KT yếu + đến hạn ôn; AI sinh cả bài theo band. Mọi câu → hàng đợi duyệt (7.3). Chỉ câu duyệt mới tới learner. Tái sử dụng kho chung. Ưu tiên lấy câu đã duyệt trước khi gọi AI mới |
| **Hệ quả** | Không cắt được AI nếu chậm tiến độ · có cost/quota ngày · khối lượng duyệt tăng theo user |
| **Client** | Shared sinh · Admin duyệt · Web/Mobile nhận bài |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§3.3 |

### 4.4 · Dashboard tiến độ cá nhân — Làm mới

| | |
| -- | -- |
| **Mô tả** | Streak · từ đã thuộc · activity 7/30/90 · điểm thi qua lần · bản đồ mạnh/yếu. Khác Analytics admin (traffic) |
| **Client** | Web chính · Mobile chính · Admin theo dõi tổng hợp |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§3.4 · T§4 Tiến độ |

### 4.5 · Adaptive kế hoạch ngày (lớp luật) — Làm mới

| | |
| -- | -- |
| **Mô tả** | Input: điểm, lỗi lặp, mục tiêu, thời gian rảnh, hoạt động → gợi ý kỹ năng/nội dung + lý do dễ hiểu. **Không thay** FSRS/cây chủ đề; bổ sung xếp lịch ngày |
| **Client** | Shared · Mobile surface |
| **Phạm vi** | MVP |
| **Nguồn** | T§3 Adaptive Engine v1 |

---

## 5 · Thư viện & sổ tay

Data thầy cấp; không lấy nguồn ngoài trái phép.

### 5.1 · Tra từ & cụm — Đã có — cần mở rộng

| | |
| -- | -- |
| **Mô tả** | Tra EN/VI/đồng nghĩa/chủ đề. Kết quả: IPA · nghĩa · từ loại · collocation · ví dụ · band. Nút thêm sổ tay / flashcard |
| **Client** | Web/Mobile · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§4.1 · T RAG library |

### 5.2 · Sổ tay ghi chú tự do — Làm mới

| | |
| -- | -- |
| **Mô tả** | Ghi chú tự do; chèn từ đã tra (giữ link thư viện); tiêu đề/thẻ/tìm kiếm. Từ chèn → nguồn flashcard, game gõ, FSRS |
| **Client** | Web/Mobile |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§4.2 (14·09) |

### 5.3 · Flashcard — Làm mới

| | |
| -- | -- |
| **Mô tả** | Thẻ lật; tự đánh giá nhớ/quên; giãn cách theo lịch. Nguồn: sổ tay hoặc bộ theo band |
| **Client** | Mobile chính · Web hỗ trợ |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§4.3 |

### 5.4 · Trợ lý RAG hỏi đáp — Làm mới(xem lai du time kh)

| | |
| -- | -- |
| **Mô tả** | Giải thích từ · cấu trúc câu · so sánh từ · ngữ pháp. Chỉ dựa nội dung đã phê duyệt. Quota/ngày |
| **Client** | Web/Mobile · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§5.5 · T§3 RAG |

---

## 6 · Cộng đồng & thi đua

Mobile chính trải nghiệm; Admin kiểm duyệt; Web hỗ trợ/hạn chế.

### 6.1 · Bảng tin / blog cộng đồng — Làm mới

| | |
| -- | -- |
| **Mô tả** | User đăng · bình luận · thích · theo dõi. Phân loại: kinh nghiệm · hỏi đáp · tìm bạn · tìm gia sư · mẹo · việc làm. Báo cáo / ẩn |
| **Hiện trạng** | Blog ~1 bài, chỉ admin |
| **Client** | Mobile chính · Web hạn chế · Admin kiểm duyệt · Shared |
| **Phạm vi** | MVP (nền tảng) |
| **Nguồn** | Đ§5.1 · T§5 |

### 6.2 · Nhóm học & thử thách tuần — Làm mới

| | |
| -- | -- |
| **Mô tả** | Nhóm tạo sẵn đối tác/admin · ghim · thử thách tuần · thảo luận gắn chương trình · khám phá/tham gia thủ công |
| **Client** | Mobile chính · Admin quản trị · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | T§5 |

### 6.3 · Quiz theo chủ đề — Làm mới

| | |
| -- | -- |
| **Mô tả** | Cùng bộ đề chủ đề, chơi bất cứ lúc nào. Điểm = đúng; tie-break thời gian. Nguồn = kho chung (7.2). Live PvP = sau |
| **Client** | Mobile/Web · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§5.2 |

### 6.4 · Bảng xếp hạng công khai — Làm mới

| | |
| -- | -- |
| **Mô tả** | Theo chủ đề; tuần/tháng/all. Chỉ lần làm đầu; chặn thời gian phi lý. Sự kiện học đã xác minh |
| **Client** | Mobile chính · Web hiển thị · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§5.3 · T§5 gamification |

### 6.5 · Gia sư  & lớp học — Đã có — cần mở rộng(bo)

| | |
| -- | -- |
| **Mô tả** | Hồ sơ gia sư (bằng cấp, kinh nghiệm, học phí) · lọc IELTS/giao tiếp · nhắn trong hệ thống. Hiện: khung trang, 0 gia sư, liên hệ ngoài |
| **Client** | Web/Mobile · Admin quản lý hồ sơ |
| **Phạm vi** | MVP (mở rộng khung hiện có) |
| **Nguồn** | Đ§5.4 *(ghi đè tree “hoãn marketplace”)* |

### 6.6 · An toàn cộng đồng (phía learner) — Làm mới

| | |
| -- | -- |
| **Mô tả** | Ẩn/báo cáo · rate limit/spam · chuyển tiếp admin. Điểm số/transcript/phản hồi mặc định riêng tư |
| **Client** | Mobile/Web · Admin quyết định cuối |
| **Phạm vi** | MVP |
| **Nguồn** | T§5 |

---

## 7 · Kho nội dung & nhãn kiến thức

Nền đỡ domain 3–4–6. Shared + Admin.

### 7.1 · Nhập dữ liệu thầy — Làm mới

| | |
| -- | -- |
| **Mô tả** | Import có validate · báo lỗi dòng · idempotent. Màn admin sửa nhãn KT. **Blocker:** chưa có file mẫu thật → chưa chốt schema |
| **Client** | Admin · Shared pipeline |
| **Phạm vi** | MVP (làm trước) |
| **Nguồn** | Đ§6.2 · T§7 pipeline |

### 7.2 · Kho câu hỏi + nhãn kiến thức — Làm mới

| | |
| -- | -- |
| **Mô tả** | Một kho: đề thi · bài luyện · quiz · (meta) game. Mỗi câu gắn điểm KT đo được. Bỏ nhãn = mất “yếu chỗ nào luyện chỗ đó” |
| **Client** | Shared · Admin QA/version |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§6.3 · T§2 thư viện · T§6 CMS |

### 7.3 · Hàng đợi duyệt câu AI — Làm mới

| | |
| -- | -- |
| **Mô tả** | Xem câu/đáp án/giải thích/KT/band · duyệt · sửa+duyệt · từ chối+lý do. Bulk/shortcut/filter. Learner báo lỗi → ngưỡng → gỡ chờ duyệt lại |
| **Chưa chốt** | Ai duyệt (thầy vs team) |
| **Client** | Admin |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§6.6 |

### 7.4 · Versioning / phát hành / hoàn tác nội dung — Làm mới

| | |
| -- | -- |
| **Mô tả** | Gói đề theo quý · QA phê duyệt · publish · rollback. 4–6 full mock QA trước beta (mục tiêu tree) |
| **Client** | Admin · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | T§2 · T§6 · T§7 |

---

## 8 · Phản hồi AI Writing/Speaking

### 8.1 · Chấm Writing (4 tiêu chí) — Làm mới

| | |
| -- | -- |
| **Mô tả** | Rubric IELTS · chú thích lỗi · gợi ý từ/cấu trúc · version phản hồi / dấu vết hiệu chỉnh |
| **Client** | Shared · Web nộp (thi) · Mobile xem lại · Admin sample review |
| **Phạm vi** | MVP bất đồng bộ |
| **Nguồn** | T§3 |

### 8.2 · Chấm Speaking (upload + ASR) — Làm mới

| | |
| -- | -- |
| **Mô tả** | Upload audio an toàn · speech-to-text · rubric 4 tiêu chí · phản hồi + bước tiếp. **Không** phỏng vấn video realtime (→ domain 10) |
| **Client** | Web (thi) · Mobile (luyện/nộp ngoài full exam) · Shared |
| **Phạm vi** | MVP bất đồng bộ |
| **Nguồn** | T§3 |

### 8.3 · Vận hành tác vụ chấm — Làm mới

| | |
| -- | -- |
| **Mô tả** | Trạng thái chờ/xong/lỗi · retry · lecturer sample · khiếu nại learner |
| **Client** | Admin · Shared queue |
| **Phạm vi** | MVP |
| **Nguồn** | T§3 · T§6 |

---

## 9 · Admin & vận hành nền tảng

Admin Web = actor riêng, không phải “chính backend”.

### 9.1 · Quản lý người dùng — Làm mới

| | |
| -- | -- |
| **Mô tả** | Tìm · xem hồ sơ/hoạt động · gói · khóa/mở khóa · lịch sử trạng thái |
| **Client** | Admin · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | T§6 |

### 9.2 · Quản lý nội dung đề/câu/audio — Làm mới

| | |
| -- | -- |
| **Mô tả** | CRUD gói đề · câu · passage · audio · đáp án/giải thích · version · QA · duyệt/từ chối · phát hành · hoàn tác. Gồm duyệt nhãn KT |
| **Client** | Admin |
| **Phạm vi** | MVP |
| **Nguồn** | Đ§6.5 · T§6 |

### 9.3 · Vận hành chấm AI — Làm mới

| | |
| -- | -- |
| **Mô tả** | Xem/lọc tác vụ · nguyên nhân fail · retry · chấm lại · hàng đợi sample giảng viên · hiệu chỉnh · xem version prompt |
| **Client** | Admin · Shared |
| **Phạm vi** | MVP |
| **Nguồn** | T§6 |

### 9.4 · Kiểm duyệt cộng đồng — Làm mới

| | |
| -- | -- |
| **Mô tả** | Hàng đợi báo cáo · rà soát post/comment · ẩn/xóa · cảnh báo · khóa · lịch sử |
| **Client** | Admin |
| **Phạm vi** | MVP |
| **Nguồn** | T§6 · Đ§5.1 |

### 9.5 · Quản trị nhóm học — Làm mới

| | |
| -- | -- |
| **Mô tả** | Tạo/sửa/lưu trữ nhóm · ghim · thử thách tuần · phạm vi hiển thị |
| **Client** | Admin |
| **Phạm vi** | MVP |
| **Nguồn** | T§6 |

### 9.6 · Gói đăng ký & quyền lợi — Làm mới

| | |
| -- | -- |
| **Mô tả** | Xem gói · cấu hình feature flag/quota · xem subscription learner · điều chỉnh thủ công |
| **Client** | Admin · Shared billing |
| **Phạm vi** | MVP |
| **Nguồn** | T§6 |

### 9.7 · Dashboard vận hành & audit — Làm mới

| | |
| -- | -- |
| **Mô tả** | Active learners · lượt thi · volume/fail AI · báo cáo cộng đồng · chi phí AI. Audit: hành động admin · đổi nội dung · kiểm duyệt · đổi entitlement |
| **Client** | Admin |
| **Phạm vi** | MVP |
| **Nguồn** | T§6 |

### 9.8 · Observability, bảo mật, độ tin cậy — Làm mới

| | |
| -- | -- |
| **Mô tả** | Error tracking · structured log · queue metrics · cost AI · RBAC · signed media URL · validate upload · consent/retention · backup drill · DB txn/audit · vendor failure handling · perf targets |
| **Client** | Shared · Admin xem một phần |
| **Phạm vi** | MVP (mức cần chạy beta) |
| **Nguồn** | T§7 |

---

## 10 · Ngoài MVP / cần kiểm chứng

| Leaf | Ghi chú | Nguồn |
| ---- | ------- | ----- |
| Phỏng vấn Speaking AI realtime (video) | Hoãn | T§8 |
| Streaming ASR / LLM / TTS pipeline | Hoãn | T§8 |
| Auto match study buddy | Hoãn | T§8 |
| DM / phòng live / social graph nặng | Hoãn | T§8 |
| Recommendation ML (thay luật + FSRS) | Hoãn | T§8 |
| Nhánh TOEIC | Hoãn | T§8 |
| Quiz PvP realtime | Mở rộng sau Đ§5.2 | Đ§5.2 |
| Chấm phát âm qua micro (nâng 2.1) | Sau nếu có nhu cầu | Đ§1.1 |
| CRM / marketing automation / BI nâng cao / payroll tutor / CMS workflow phức tạp | Ngoài admin MVP | T nguyên tắc |

**Không** đưa tutor marketplace vào domain 10 — đã ở **6.5 MVP** theo đặc tả.

---

## Ma trận sở hữu client (MVP)

| Nhóm | Web | Mobile | Admin | Shared |
| ---- | --: | -----: | ----: | -----: |
| Auth, hồ sơ, mục tiêu, entitlement | Hỗ trợ | Chính | Hỗ trợ | Chính |
| Game, phát âm, kế hoạch ngày, nghe 10', streak | Hỗ trợ | Chính | Theo dõi | Chính |
| Thi thử / mô phỏng kỳ thi / đầu vào đầy đủ | **Chính** | **Không** | Hỗ trợ nội dung | Chính |
| Bài luyện ngắn theo dạng / lộ trình | Hỗ trợ | Chính | Theo dõi | Chính |
| Kết quả thi & phân tích lỗi | Chính | Xem tóm tắt | Theo dõi | Chính |
| Tra cứu, sổ tay, flashcard | Hỗ trợ | Chính | — | Chính |
| Cộng đồng, quiz, BXH, nhóm | Hạn chế | Chính | Kiểm duyệt | Chính |
| Gia sư | Hỗ trợ | Hỗ trợ | Quản lý hồ sơ | Chính |
| Nộp/xem phản hồi W/S | Chính (thi) | Chính (xem/luyện) | Vận hành | Chính |
| CMS, duyệt AI Q, chấm AI ops, audit | Không | Không | Chính | Chính |

---

## Việc cần làm trước tiên

1. Xin thầy **file mẫu** đề IELTS + kho từ/ngữ pháp → chốt schema 7.2.  
2. Hỏi **ai duyệt** câu AI (7.3) — rủi ro vận hành theo scale.  
3. Xin **mã nguồn** engine thi hiện tại (3.2) — mở rộng vs viết lại.  
4. Giữ nguyên 2 file nguồn; file này là SSOT mới cho plan.

---

## Tiêu chí nghiệm thu tài liệu

- [ ] Mọi leaf MVP có đủ: mô tả · trạng thái · client · phạm vi · nguồn  
- [ ] Mobile **không** xuất hiện là client chính/hỗ trợ của full exam simulator  
- [ ] Conflict bảng đầu đã phản ánh quyết định 18·09  
- [ ] Domain 9 đủ để vận hành beta (user, content, AI grade, mod, entitlements, audit)  
- [ ] Domain 10 tách rõ khỏi MVP  
- [ ] Đủ làm đầu vào `/ck:plan` không cần mở lại 2 file cũ trừ khi truy vết

---

## Bản quyền

Chỉ dùng dữ liệu/mã theo cho phép của thầy. Tham khảo sản phẩm khác = mô hình tính năng, không copy data/UI/code.

**File nguồn giữ nguyên:** `01-dac-ta-tinh-nang.md` · `260913-ielts-platform-feature-tree.md`  
**Bỏ qua:** `01-dac-ta-tinh-nang - Copy.md`
