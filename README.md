# IP Watcher

App Android (Kotlin) tự động: kiểm tra IP hiện tại → nếu không khớp dải mong muốn →
tự bật/tắt Airplane Mode để đổi IP → lặp lại. Có Quick Settings Tile để bật/tắt
nhanh từ thanh trạng thái.

## Cơ chế hoạt động (quan trọng, đọc trước khi dùng)

Android **không** cho phép app thường tự bật/tắt Airplane Mode nếu không root.
App này dùng đúng kỹ thuật mà app "HTTP Custom" dùng: đăng ký làm
**Trợ lý số mặc định (Digital assistant app)** thông qua `VoiceInteractionService`.
Khi bạn chọn app này làm Trợ lý mặc định trong Settings, nó được cấp quyền hệ thống
để gửi Intent `Settings.ACTION_VOICE_CONTROL_AIRPLANE_MODE` — bật/tắt Airplane Mode
thật, không cần root, **không cần ADB, không cần máy tính**.

App không dùng AccessibilityService, không cần cấp quyền đặc biệt qua dòng lệnh nào cả.

**Lưu ý thật:**
- Một số máy Samsung (One UI), Xiaomi (MIUI), Oppo/realme (ColorOS) có thể chặn hoặc
  hạn chế Intent này ở phiên bản Android/OEM mới. Nếu không hoạt động, đó là do
  hãng máy chặn, không phải do code sai.
- Khi đặt app này làm Trợ lý mặc định, bạn sẽ **mất** trợ lý đang dùng (Google
  Assistant/Bixby...) cho đến khi đổi lại trong Settings.
- App gọi API công khai `api.ipify.org` để lấy IP hiện tại, cần Internet.

## Cấu trúc project

Đây là project Android Studio chuẩn (Gradle Kotlin DSL). Mở trực tiếp bằng
Android Studio hoặc build qua GitHub Actions (xem bên dưới).

## Cách lấy file .apk — 2 cách, không cần cài gì phức tạp

### Cách 1: GitHub Actions (khuyên dùng, không cần cài Android Studio)

1. Tạo một repo GitHub mới (miễn phí), tải toàn bộ project này lên (kéo-thả trên
   web GitHub hoặc `git push`).
2. Vào tab **Actions** của repo → workflow "Build APK" sẽ tự chạy (đã cấu hình sẵn
   ở `.github/workflows/build.yml`). Nếu không tự chạy, bấm **Run workflow**.
3. Sau khi chạy xong (2-4 phút), vào job vừa chạy → phần **Artifacts** → tải
   `ip-watcher-debug-apk` → giải nén ra sẽ có `app-debug.apk`.
4. Chuyển file này vào điện thoại, cài như APK thường (cần bật "Cài từ nguồn không
   xác định" cho trình quản lý file/trình duyệt).

### Cách 2: Build bằng Android Studio (nếu bạn có máy tính)

1. Cài Android Studio (miễn phí, tải tại developer.android.com).
2. Mở project này bằng "Open" trong Android Studio, đợi Gradle sync xong.
3. Menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. File `.apk` nằm trong `app/build/outputs/apk/debug/app-debug.apk`.

## Cách dùng sau khi cài app

1. Mở app → nhấn **"Đặt làm Trợ lý mặc định"** → hệ thống hỏi xác nhận → Đồng ý.
   Nếu điện thoại không hiện hộp thoại này, tự vào **Settings > Apps > Default apps
   > Digital assistant app** và chọn "IP Watcher".
2. Nhập dải IP mong muốn, ví dụ: `42.` (khớp mọi IP bắt đầu bằng 42.) hoặc để trống
   nếu chỉ muốn theo dõi không lọc.
3. Nhập khoảng thời gian kiểm tra (giây).
4. Nhấn **Lưu cấu hình** → **Bắt đầu theo dõi**.
5. App chạy nền (foreground service, có thông báo hiển thị trạng thái).

## Thêm Tile vào thanh trạng thái (vuốt từ trên xuống)

1. Vuốt 2 lần từ trên xuống để mở Quick Settings đầy đủ.
2. Bấm nút **bút chì / Edit** (biểu tượng chỉnh sửa các tile).
3. Kéo tile **"IP Watcher"** từ danh sách vào khu vực đang dùng.
4. Từ đó có thể bấm tile để bật/tắt việc theo dõi ngay từ thanh trạng thái,
   không cần mở app.

## Tùy chỉnh

Trong `Prefs.kt` có thể chỉnh thêm:
- `KEY_TOGGLE_ON_MS`: thời gian giữ Airplane Mode ở trạng thái ON trước khi tắt.
- `KEY_RECONNECT_WAIT_MS`: thời gian chờ mạng kết nối lại sau khi tắt Airplane Mode.

## Nếu không thấy Airplane Mode tự bật/tắt

1. Mở app, bấm nút **"Test Toggle (bật máy bay 3s rồi tắt)"**. Đây là cách test
   riêng, tách khỏi vòng lặp theo dõi IP, để biết chính xác cơ chế toggle có
   hoạt động trên máy bạn hay không.
   - Nếu Toast báo "Gửi lệnh THẤT BẠI ngay từ bước đầu": thử **khởi động lại máy**
     sau khi đặt app làm Trợ lý mặc định (một số máy chỉ thực sự bind
     VoiceInteractionService sau khi reboot).
   - Nếu Toast báo "Đã gửi lệnh BẬT" nhưng máy bay không lên: khả năng cao thiết
     bị/OEM (Samsung, Xiaomi, Oppo...) chặn Intent
     `Settings.ACTION_VOICE_CONTROL_AIRPLANE_MODE` ở phiên bản Android của máy.
     Đây là giới hạn từ hãng máy, không sửa được bằng code.

2. Nếu có máy tính, cắm dây USB, bật **USB debugging** (Settings > About phone >
   bấm 7 lần vào Build number > vào lại Settings > Developer options > USB
   debugging) — việc này **không cần root, không cấp quyền đặc biệt gì**, chỉ để
   xem log:
   ```
   adb logcat -s IpWatcher
   ```
   Bấm nút Test Toggle rồi xem log hiện ra dòng nào, gửi lại đoạn log đó để
   chẩn đoán chính xác nguyên nhân.

3. Trong ô "dải IP mong muốn", giờ đã hỗ trợ nhập **nhiều IP/tiền tố**, ngăn cách
   bằng `;` hoặc `,`, ví dụ: `42.` hoặc `10.119,10.41,10.118`. App sẽ coi là khớp
   nếu IP hiện tại bắt đầu bằng MỘT trong các giá trị đó.

## Hao pin / dữ liệu

Vì phải gọi API kiểm tra IP liên tục theo chu kỳ, nên khoảng kiểm tra quá ngắn
(dưới vài giây) sẽ tốn pin và dữ liệu hơn. 5-10 giây là hợp lý cho hầu hết nhu cầu.
