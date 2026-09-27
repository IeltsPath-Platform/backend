# Brainstorm: AI Learning so với DeepTutor, đã đủ chức năng chưa?

- Ngày: 2026-09-27
- Nhánh: `feat/ai-learning-service`
- Phạm vi: `services/ai-learning-service` sau pha 1–10 của plan `260926-2249-tutor-study-review`
- Tham chiếu: `third_party/deeptutor` (chỉ đọc; service không phụ thuộc DeepTutor)
- Quyết định của người dùng: **Hướng A, củng cố vận hành**

## Vấn đề
AI Learning đã hoàn tất pha 1–10. Câu hỏi: so với DeepTutor, còn thiếu chức năng nào cần làm cho IELTSPath, và thiếu cái
nào không cần?

## Bối cảnh
- DeepTutor là nền tảng dạy học **tổng quát**, khoảng 15 capability (Chat, Ask Questions, Quiz, Research, Visualize, Solve,
  Course Study, Mastery Path, Immersive Reading, Immersive Watching…), RAG nhiều engine, memory L1/L2/L3,
  subagent/partner, sinh ảnh/video/voice, MCP, CLI, plugin, EduHub.
- AI Learning chỉ lấy phần phục vụ **lộ trình IELTS có mastery**. Engine đã port, không phụ thuộc DeepTutor.
- IELTSPath đã có ở service khác: câu hỏi và video (Content), đề và chấm (Assessment), notes/flashcards/tiến độ video
  (learning-support), notification-service.

## Đối chiếu

| Capability DeepTutor | AI Learning | Đánh giá |
| --- | --- | --- |
| Mastery Path (gate, `next_objective`, lịch ôn KP) | Có (port) | Đủ |
| Mastery loop (quiz, grade, assess) | Có (pha 1–4) | Đủ |
| Outline revise và profile | Có (pha 5, chỉ đổi thứ tự) | Đủ; cố ý không thêm/bỏ KP (Content sở hữu curriculum) |
| Quiz, Question Notebook, practice review | Có (pha 6) | Đủ |
| Memory | Có (pha 7, 1 tầng) | Đủ; L1/L2/L3 là quá mức cần |
| Notebook, ghi chú | Có (pha 8, dùng notes của learning-support) | Đủ |
| Immersive Reading | Có (pha 9) | Đủ |
| Lưu thành flashcard | Có (pha 10) | Đủ |
| Immersive Watching | Chưa | Đáng cân nhắc (Listening; video đã có ở Content) |
| Stream từng token | Chưa (stream theo từng bước) | Nên làm trước khi có frontend thật |
| Giới hạn chi phí LLM theo học viên | Chưa | Nên làm trước khi public |
| Nhắc lịch ôn (cron, reminder) | Chưa | Đáng làm, rẻ (qua notification-service) |
| Gợi ý câu hỏi, hint | Chưa | Nhỏ, tùy UX |
| Voice TTS/STT | Chưa | Để sau; thuộc một service Speaking riêng |
| Chấm Writing/Speaking bằng AI | Chưa | Thuộc Assessment, không phải AI Learning |
| RAG nhiều engine, upload tài liệu | Không | Không cần (Content quản lý nội dung) |
| Solve, Research, Visualize, Math animator, sinh media | Không | Không cần cho IELTS |
| Subagent/Partner, IM, MCP, CLI, plugin, EduHub | Không | Không cần (tính năng nền tảng tổng quát) |
| Persona, Co-Writer, Book | Không | Không cần (lộ trình mastery đã thay vai trò course/book) |

## Kết luận
Về chức năng **học**, AI Learning đã đủ phần lõi cho IELTSPath. Phần DeepTutor có mà AI Learning thiếu phần lớn thuộc về
nền tảng tổng quát; thêm vào chỉ tăng độ phức tạp (YAGNI). Rủi ro thật nằm ở **độ sẵn sàng vận hành**:
1. Pha 5–10 chưa kiểm E2E qua Gateway (pha 1–4 đã có báo cáo E2E).
2. Không có giới hạn chi phí LLM theo học viên, nên có rủi ro tiền thật khi mở cho người dùng.
3. Chưa stream từng token, nên câu trả lời dài chờ lâu mới hiện.

## Các hướng đã cân nhắc

| Hướng | Nội dung | Ưu | Nhược | Công |
| --- | --- | --- | --- | --- |
| **A. Củng cố vận hành (đã chọn)** | E2E qua Gateway cho pha 5–10; giới hạn chi phí LLM; stream token | Biến cái đã có thành dùng được thật; giảm rủi ro chi phí; chuẩn bị cho frontend | Không có tính năng mới để demo | ~3–4 ngày |
| B. Immersive Watching | Tutor theo mốc thời gian video | Tính năng mới giá trị nhất (Listening); dữ liệu có sẵn | Cộng thêm độ phức tạp lên nền chưa kiểm E2E | ~3 ngày |
| C. Nhắc lịch ôn | Event "N câu/KP đến hạn" gửi notification-service | Rẻ, giữ chân học viên | Cần hợp đồng event với notification-service | ~1 ngày |

Thứ tự đề xuất sau A: C rồi B.

## Phạm vi hướng A (đầu vào cho `/ck:plan`)
1. **E2E qua Gateway cho pha 5–10:** mở rộng `tests/e2e` (LLM stub, script mode) cho các luồng: đổi thứ tự và hồ sơ; luyện
   câu hỏi, trả lời, ôn; memory; `note.draft`; session đọc bài (Content thật hoặc stub); lưu flashcard qua learning-support.
   Có báo cáo E2E như pha 4.
2. **Giới hạn chi phí LLM theo học viên:** hạn mức theo ngày (số lượt tutor và/hoặc token), kiểm trước khi gọi LLM, trả
   mã lỗi rõ (ví dụ `429` hoặc `turn.failed` với `failureCode=quota_exceeded`), cấu hình bằng biến môi trường, tính cả lần
   tóm tắt memory.
3. **Stream từng token:** `ChatCompletionsClient` hỗ trợ `stream=true`; SSE thêm event `assistant.delta`. Giữ quy tắc bỏ
   prose khi lượt đặt thẻ câu hỏi (không stream prose của reply có tool card); tương thích ngược với client chỉ đọc
   `assistant.message`.

## Câu hỏi còn mở (chốt khi lập plan)
- Hạn mức chi phí: đếm theo lượt hay theo token? Mức mặc định? Có khác nhau theo gói (FREE/PREMIUM, access-service) không?
- Streaming: có cần cho cả lượt có tool (chỉ stream prose sau cùng) hay chỉ lượt trả lời thuần?
- E2E: chạy trên docker-compose đầy đủ (Gateway, Content, learning-support) hay stub bớt service?

## Tiêu chí thành công
- Báo cáo E2E pha 5–10 qua Gateway, tất cả luồng pass.
- Vượt hạn mức thì bị chặn trước khi gọi LLM, có test; mức cấu hình được.
- Frontend thấy chữ hiện dần khi tutor trả lời; đáp án của câu hỏi vẫn không lộ; client cũ vẫn chạy.
