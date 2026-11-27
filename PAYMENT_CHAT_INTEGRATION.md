# Thanh toán SePay và chat Firebase

## Thanh toán

Điền `BANK_ID`, `BANK_NAME`, `BANK_ACCOUNT_NO`, `BANK_ACCOUNT_NAME` và
`SEPAY_API_KEY` vào `.env` của LAVEN. API key phải giống cấu hình API Key
trong webhook SePay. URL webhook là
`https://<domain-cong-khai>/api/payments/webhook` (POST, JSON, tiền vào).
Khi chạy localhost, dùng ngrok để SePay gọi được URL này.

Sau khi tạo đơn `BANK_TRANSFER`, `OrderService` trả link VietQR có số tiền
`totalAmount` và nội dung `orderId`. `GET /api/customer/orders/{id}/payment-qr`
trả các field gốc của nhóm Hà: `bankName`, `bankAccount`, `accountName`,
`amount`, `transferContent`, `qrCodeUrl`, `paymentStatus`.
`PaymentQrResponseCompatibility` bổ sung `bankId`, `accountNo`, `content`,
`qrUrl` vào cùng JSON để trang thanh toán React đọc được, không đổi field gốc
hay đường dẫn API của Hà.
SePay gửi webhook; `BankPaymentService` kiểm tra API key, tài khoản, mã đơn,
số tiền và ID giao dịch. Một transaction MariaDB lưu `bank_transactions`,
chuyển `paymentStatus=PAID`, ghi `paidAt` và giữ `orderStatus=PENDING`.
React chỉ đọc trạng thái đơn, không được tự xác nhận thanh toán.
Nhân viên bấm **Xác nhận đơn** để chuyển orderStatus sang CONFIRMED và ghi
confirmedBy, confirmedAt. Endpoint
`payment-notify` và API nhân viên cũ của nhóm Hà được giữ nguyên; cần thống nhất
với Hà trước khi thay luồng cũ ở frontend.

Nếu webhook lỗi nhưng tiền đã vào tài khoản, nhân viên kiểm tra sao kê rồi gọi
`POST /api/staff/orders/{id}/payment/manual` bằng JWT STAFF/ADMIN với JSON
`{"bankReference":"FT123","accountNumber":"5817102004","amountIn":2000,
"transferContent":"ORD...","transactionDate":"2026-11-22T12:30:00"}`.
Server đối chiếu tài khoản, số tiền, mã đơn và mã tham chiếu; lưu bản ghi
`bank_transactions` có `gateway=MANUAL_VERIFIED` và `transactionId=MANUAL:FT123`
để phân biệt với webhook SePay. Thanh toán chuyển sang `PAID`, đơn vẫn
`PENDING` cho đến khi nhân viên xác nhận đơn. Không được dùng mã tham chiếu hai lần.

## Chat

Điền các biến `FIREBASE_*` trong `.env`. Đặt file service account **ngoài Git**.
`GET /api/chat/session` cần JWT LAVEN, trả Firebase custom token, cấu hình
Firebase Web SDK và danh sách khách cho STAFF/ADMIN. Firebase `uid` chính là
`User.userId`; claim `role` lấy từ MariaDB. Backend không lưu tin nhắn.

Trong Firebase Console > Realtime Database > Rules, dùng nội dung
`firebase-chat.rules.json`. Dữ liệu chat:
`chats/{customerId}/messages/{pushId} = { senderId, content, sentAt }`.
`sentAt` là chuỗi `dd/MM/yyyy HH:mm:ss`. React cần đổi `chatService.js` từ
`localStorage` sang Firebase SDK, đăng nhập bằng custom token và dùng các field
tiếng Anh trên. Hiện frontend chưa thực hiện bước này.

Frontend `PaymentPage.jsx` đã đọc được QR nhờ các field bổ sung, nhưng vẫn còn
nút “Tôi đã chuyển khoản” gọi endpoint cũ, và trang nhân viên vẫn gọi API xác
nhận cũ. Backend tự đổi thanh toán sang `PAID` từ webhook không phụ thuộc nút này.
Để dùng thao tác xác nhận thủ công trên giao diện, frontend phải gọi endpoint
`/payment/manual` cùng dữ liệu giao dịch đã đối chiếu; frontend chưa được sửa
trong lần tích hợp backend này.
