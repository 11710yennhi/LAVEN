-- Bảng lưu vector sản phẩm cho tìm kiếm AI trên TiDB (chạy 1 lần trên database TiDB của nhóm).
-- Dùng bởi iuh.fit.laven.config.TiDbVectorStore. Dữ liệu trong bảng tạo lại được bằng
-- POST /api/admin/ai/reindex, nên DROP không làm mất dữ liệu nghiệp vụ.
DROP TABLE IF EXISTS product_vectors;

CREATE TABLE product_vectors (
  id        VARCHAR(36) NOT NULL,   -- UUID dạng chuỗi, sinh từ productId (ProductDocumentMapper.documentId)
  content   TEXT,                   -- đoạn văn mô tả sản phẩm đã đem đi tạo vector
  metadata  JSON,                   -- productId, categoryId, brand, status
  embedding VECTOR(768) NOT NULL,   -- vector 768 chiều của Gemini (gemini-embedding-001)
  PRIMARY KEY (id)
);
