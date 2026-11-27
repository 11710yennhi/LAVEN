# LAVEN
Bài tập lớn môn WWW

## API theo nghiệp vụ v2

- `GET /api/public/products` và `GET /api/public/products/{productId}` trả DTO công khai, không có `originalPrice` (giá nhập) hay `packagingCost`. Bộ lọc giá nhập chỉ có ở API staff.
- `GET /api/public/categories` không trả phí đóng gói.
- Khi thêm tỉnh/thành, staff nhập `shipCostId` theo `PROVINCE_ID` của Viettel Post. API sửa chỉ đổi `priceShip`, giữ nguyên mã và tên tỉnh để đơn cũ tra cứu theo `receiverCity`.
- Sản phẩm mới có mã bắt đầu bằng `PRO`. Mã `PRD` đã có trong database không bị đổi tự động.
- `POST /api/staff/stocks/{variantId}/{colorId}?quantity=0` tạo dòng tồn kho ban đầu một lần. Nhập thêm hàng gọi `POST /api/staff/stocks/{variantId}/{colorId}/adjustment?delta=10`. Không còn API ghi đè số tồn đã có.

## Ảnh sản phẩm: Trình duyệt tải thẳng lên Cloudinary (Direct Upload)

Ảnh không đi qua server LAVEN nhằm tối ưu tài nguyên máy chủ. MariaDB chỉ lưu URL trong `product_images.imageUrl`.
Tạo sản phẩm trước để có `productId`, sau đó nhân viên chọn ảnh trên giao diện.
Chỉ tài khoản STAFF hoặc ADMIN có JWT mới gọi được các API bên dưới.

### Quy trình 3 bước:
1. **Bước 1 (Xin chữ ký)**: React gọi `POST /api/staff/products/{productId}/images/upload-signature`. Server kiểm tra sản phẩm và trả về `cloudName`, `apiKey`, `timestamp`, `publicId`, `signature`.
2. **Bước 2 (Tải ảnh)**: Trình duyệt gửi file ảnh kèm các trường chữ ký thẳng tới `POST https://api.cloudinary.com/v1_1/{cloudName}/image/upload` dưới dạng `FormData`. Cloudinary trả về `secure_url`.
3. **Bước 3 (Lưu URL)**: React gọi `POST /api/staff/products/{productId}/images` với JSON `{ "imageUrl": "<secure_url>" }`. `ProductImageService` kiểm tra URL và lưu vào CSDL, sinh mã `productImageId`.

---

### Danh sách API Quản lý ảnh (Dễ nhận diện):

| Chức năng | Method | URL | Mô tả |
|---|---|---|---|
| **Lấy chữ ký số** | `POST` | `/api/staff/products/{productId}/images/upload-signature` | Sinh signature trước khi tải file |
| **Thêm ảnh (Add)** | `POST` | `/api/staff/products/{productId}/images` | Lưu URL ảnh vừa tải vào CSDL |
| **Cập nhật ảnh (Update)** | `PUT` | `/api/staff/images/{imageId}` | Đổi sang URL ảnh mới (tự dọn ảnh cũ trên Cloudinary) |
| **Xóa ảnh (Delete)** | `DELETE` | `/api/staff/images/{imageId}` | Xóa ảnh khỏi DB và xóa file trên Cloudinary |
| **Xem/Tìm kiếm ảnh** | `GET` | `/api/public/products/{productId}/images` | Lấy danh sách ảnh của sản phẩm |

---

### Ví dụ code React gọi API:

```javascript
// 1. Thêm ảnh mới (Add)
async function themAnhSanPham(productId, file, accessToken) {
  const headers = { Authorization: `Bearer ${accessToken}` };

  // Bước 1: Lấy chữ ký từ LAVEN
  const sigRes = await fetch(`/api/staff/products/${productId}/images/upload-signature`, {
    method: 'POST',
    headers
  });
  if (!sigRes.ok) throw new Error('Không lấy được chữ ký tải ảnh');
  const sig = await sigRes.json();

  // Bước 2: Tải thẳng file lên Cloudinary
  const form = new FormData();
  form.append('file', file);
  form.append('api_key', sig.apiKey);
  form.append('timestamp', String(sig.timestamp));
  form.append('public_id', sig.publicId);
  form.append('signature', sig.signature);

  const cloudRes = await fetch(`https://api.cloudinary.com/v1_1/${sig.cloudName}/image/upload`, {
    method: 'POST',
    body: form
  });
  if (!cloudRes.ok) throw new Error('Cloudinary tải ảnh thất bại');
  const cloudData = await cloudRes.json();

  // Bước 3: Gửi URL về LAVEN lưu vào CSDL
  const saveRes = await fetch(`/api/staff/products/${productId}/images`, {
    method: 'POST',
    headers: { ...headers, 'Content-Type': 'application/json' },
    body: JSON.stringify({ imageUrl: cloudData.secure_url })
  });
  if (!saveRes.ok) throw new Error('Lưu URL ảnh vào LAVEN thất bại');
  return saveRes.json(); // Trả về ProductImage: { productImageId, imageUrl }
}

// 2. Xóa ảnh sản phẩm (Delete)
async function xoaAnhSanPham(imageId, accessToken) {
  const res = await fetch(`/api/staff/images/${imageId}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${accessToken}` }
  });
  if (!res.ok) throw new Error('Xóa ảnh thất bại');
  return res.json(); // { "message": "Xóa ảnh sản phẩm thành công" }
}
```
