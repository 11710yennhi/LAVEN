# Thanh toán SePay và chat Firebase

## Thanh toán

Điền `BANK_ID`, `BANK_NAME`, `BANK_ACCOUNT_NO`, `BANK_ACCOUNT_NAME` và
`SEPAY_API_KEY` vào `.env` của LAVEN. API key phải giống cấu hình API Key
trong webhook SePay. URL webhook là
`https://<domain-cong-khai>/api/payments/webhook` (POST, JSON, tiền vào).
Khi chạy localhost, dùng ngrok để SePay gọi được URL này.

Sau khi tạo đơn `BANK_TRANSFER`, `OrderService` trả link VietQR có số tiền
`totalAmount` và nội dung `orderId`. `GET /api/customer/orders/{id}/payment-qr`
trả các field `bankId`, `accountNo`, `accountName`, `amount`, `content`, `qrUrl`.
SePay gửi webhook; `BankPaymentService` kiểm tra API key, tài khoản, mã đơn,
số tiền và ID giao dịch. Một transaction MariaDB lưu `bank_transactions`,
chuyển `paymentStatus=PAID`, `orderStatus=CONFIRMED` và ghi thời điểm.
React chỉ đọc trạng thái đơn, không được tự xác nhận thanh toán.
Luồng mới dùng `PENDING -> PAID`; endpoint `payment-notify` cũ vẫn trả lời
frontend nhưng không đổi trạng thái sang `WAITING`. Enum cũ được giữ để đọc
những đơn đã lưu từ trước.

Nếu webhook lỗi nhưng tiền đã vào tài khoản, nhân viên kiểm tra sao kê rồi gọi
`POST /api/staff/orders/{id}/payment/manual` bằng JWT STAFF/ADMIN với JSON
`{"bankReference":"<ma-giao-dich-tren-sao-ke>"}`. Server lưu mã tham chiếu
trên `orders.manualPaymentReference`, `confirmedBy` là nhân viên và đổi
`PAID/CONFIRMED` trong cùng transaction. Xác nhận thủ công không tạo bản ghi
SePay giả trong `bank_transactions`. Mã tham chiếu không được dùng cho đơn khác.

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

Frontend `PaymentPage.jsx` hiện còn nút “Tôi đã chuyển khoản” và hiển thị QR
minh họa khi `qrUrl` rỗng. Khi nối frontend thật, cần bỏ nút này khỏi luồng
webhook, ẩn QR khi `PAID`, và gọi lại `getMyOrder` để thấy trạng thái mới.
