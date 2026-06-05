# Iron AI Coach - Gemini Online

App Android Java dùng Gemini API làm AI huấn luyện viên nghiêm khắc.

## Tính năng

- AI online bằng Gemini API, mặc định `gemini-2.5-flash`.
- Tạo giáo án toàn thân: chân, tay, vai cổ, bụng, ngực, lưng, mặt, ánh mắt.
- Nhập dữ liệu sau tập: bài, tạ, set, reps, RPE, đau/mỏi, hoàn thành.
- Lần sau AI dựa vào nhật ký trước để tăng/giảm cường độ.
- Video: tự tạo truy vấn YouTube và mở video; nếu dán link YouTube công khai, Gemini gợi ý timestamp/đoạn cần xem.
- Check-in hằng ngày, bảng hành trình, báo thức hằng ngày.
- Dữ liệu người dùng/API key lưu cục bộ trên máy bằng SharedPreferences; không có server trung gian; `allowBackup=false`.

## Vì sao dùng Gemini?

App cần hiểu video và chọn đoạn/timestamp cho bài tập. Gemini API có tài liệu chính thức cho video understanding, hỗ trợ video/YouTube công khai. Vì vậy bản này chọn Gemini thay vì GPT-4o cho nhu cầu video hướng dẫn tập.

## Giới hạn pháp lý/kỹ thuật

App không tải lậu hoặc cắt lậu video YouTube. Nó tìm/mở video và nhảy tới timestamp hợp lệ. Nếu muốn tải/cắt thật, chỉ dùng với video bạn sở hữu hoặc nguồn cho phép tải trực tiếp.

## Build APK bằng GitHub

1. Upload project lên GitHub.
2. Vào tab Actions.
3. Chạy workflow `Build APK`.
4. Tải APK trong Artifacts.

## Nhập API key

Mở app > tab **Dữ liệu** > nhập Gemini API key > bấm **Test gọi Gemini**.
