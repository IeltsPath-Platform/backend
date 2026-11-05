# Cài Graphify cho người lần đầu

Graphify giúp tạo knowledge graph từ source code để đọc kiến trúc, quan hệ file,
class, dependency và luồng gọi nhanh hơn. Package trên PyPI tên là `graphifyy`
nhưng command sau khi cài là `graphify`.

Kiểm tra máy đã có `uv` chưa:

```powershell
uv --version
```

Nếu chưa có `uv`, cài bằng PowerShell:

```powershell
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"
```

Sau khi cài, đóng terminal rồi mở lại. Nếu command vẫn chưa nhận, chạy:

```powershell
uv tool update-shell
```

Cài Graphify bằng `uv`:

```powershell
uv tool install graphifyy
graphify --version
```

Đăng ký Graphify skill cho Codex trong project hiện tại (tuỳ chọn):

```powershell
graphify install --platform codex
```

Tạo knowledge graph từ source code. Chế độ `--code-only` không cần API key hoặc LLM:

```powershell
graphify extract . --code-only
```

Tạo trang đồ thị tương tác từ graph đã sinh:

```powershell
graphify export html --graph .\graphify-out\graph.json
```

Cập nhật đồ thị sau khi code thay đổi (Incremental Update):

Sau khi thêm mới hoặc sửa đổi code, chạy lệnh cập nhật gia tăng để cập nhật graph nhanh chóng mà không cần extract lại toàn bộ từ đầu:

```powershell
graphify update .
```

Nếu vừa refactor lớn hoặc xóa nhiều file, dùng cờ `--force`:

```powershell
graphify update . --force
```

Sau khi update, chạy lại lệnh xuất HTML để đồng bộ giao diện hiển thị:

```powershell
graphify export html --graph .\graphify-out\graph.json
```

Truy vấn và phân tích quan hệ codebase qua CLI:

```powershell
graphify query "<câu hỏi về codebase>"
graphify explain "<khái niệm hoặc symbol>"
graphify path "<symbol A>" "<symbol B>"
graphify affected "<symbol thay đổi>"
```

Kết quả nằm trong `graphify-out/`:

- `graph.json`: knowledge graph dùng với các lệnh như `graphify query`, `graphify path` và `graphify explain`.
- `graph.html`: trang đồ thị tương tác, có thể mở trực tiếp bằng trình duyệt.
- `cache/`: cache nội bộ; `cache/stat-index.json` không phải graph hoàn chỉnh.

Nếu đã cấu hình API key/backend LLM, có thể bỏ `--code-only` để Graphify bổ sung các quan hệ semantic. Repo đã có `.graphifyignore` để bỏ qua artifact Graphify và markdown khi build graph.
