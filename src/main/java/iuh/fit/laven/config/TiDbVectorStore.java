package iuh.fit.laven.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * VectorStore chạy trên TiDB (thay cho MariaDBVectorStore của thư viện spring-ai-starter-vector-store-mariadb).
 *
 * Bảng (tạo tay 1 lần, xem src/main/resources/sql/tidb_product_vectors.sql):
 *   product_vectors(id VARCHAR(36) PK, content TEXT, metadata JSON, embedding VECTOR(768))
 *
 * Các service AI (ProductIndexService, SemanticSearchService) chỉ gọi 3 việc của VectorStore,
 * nên không phải sửa:
 *   - add(documents)          : Gemini tạo vector rồi thêm/ghi đè theo id
 *   - delete(ids)             : xóa theo id
 *   - similaritySearch(req)   : tìm theo khoảng cách cosine, lọc theo metadata (vd categoryId)
 */
@Slf4j
@Component
public class TiDbVectorStore implements VectorStore {

    /** Mỗi lần gọi Gemini tạo tối đa bấy nhiêu vector, tránh 1 request quá lớn. */
    private static final int EMBED_BATCH_SIZE = 50;
    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9_]+$");

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;
    private final String table;

    public TiDbVectorStore(JdbcTemplate jdbcTemplate,
                           EmbeddingModel embeddingModel,
                           ObjectMapper objectMapper,
                           @Value("${app.ai.vector-table:product_vectors}") String table) {
        // Tên bảng được ghép thẳng vào câu SQL nên chỉ cho phép chữ, số, gạch dưới
        if (!SAFE_NAME.matcher(table).matches()) {
            throw new IllegalStateException("Tên bảng vector không hợp lệ: " + table);
        }
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
        this.objectMapper = objectMapper;
        this.table = table;
    }

    // ===================== Thêm / ghi đè =====================

    @Override
    public void add(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO " + table + " (id, content, metadata, embedding) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE content = VALUES(content), metadata = VALUES(metadata), "
                + "embedding = VALUES(embedding)";

        for (int from = 0; from < documents.size(); from += EMBED_BATCH_SIZE) {
            List<Document> batch = documents.subList(from, Math.min(from + EMBED_BATCH_SIZE, documents.size()));
            List<float[]> vectors = embeddingModel.embed(batch.stream().map(Document::getText).toList());

            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < batch.size(); i++) {
                Document document = batch.get(i);
                rows.add(new Object[]{
                        document.getId(),
                        document.getText(),
                        toJson(document.getMetadata()),
                        toVectorLiteral(vectors.get(i))
                });
            }
            jdbcTemplate.batchUpdate(sql, rows);
        }
    }

    // ===================== Xóa =====================

    @Override
    public void delete(List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return;
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(idList.size(), "?"));
        jdbcTemplate.update("DELETE FROM " + table + " WHERE id IN (" + placeholders + ")", idList.toArray());
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        List<Object> params = new ArrayList<>();
        String where = toSql(filterExpression, params);
        jdbcTemplate.update("DELETE FROM " + table + " WHERE " + where, params.toArray());
    }

    // ===================== Tìm kiếm =====================

    /**
     * VEC_COSINE_DISTANCE trả về khoảng cách (0 = giống hệt). Điểm giống = 1 - khoảng cách,
     * giống cách MariaDBVectorStore tính, nên ngưỡng app.ai.min-similarity (0.55) giữ nguyên ý nghĩa.
     */
    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        String queryVector = toVectorLiteral(embeddingModel.embed(request.getQuery()));

        List<Object> params = new ArrayList<>();
        params.add(queryVector);
        StringBuilder sql = new StringBuilder("SELECT id, content, metadata, VEC_COSINE_DISTANCE(embedding, ?) AS distance FROM ")
                .append(table);
        if (request.getFilterExpression() != null) {
            sql.append(" WHERE ").append(toSql(request.getFilterExpression(), params));
        }
        sql.append(" ORDER BY distance LIMIT ?");
        params.add(request.getTopK());

        double threshold = request.getSimilarityThreshold();
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
                    double score = 1.0 - rs.getDouble("distance");
                    return Document.builder()
                            .id(rs.getString("id"))
                            .text(rs.getString("content"))
                            .metadata(fromJson(rs.getString("metadata")))
                            .score(score)
                            .build();
                }, params.toArray()).stream()
                .filter(document -> document.getScore() != null && document.getScore() >= threshold)
                .toList();
    }

    // ===================== Bộ lọc metadata -> SQL =====================

    /**
     * Hỗ trợ các bộ lọc code đang dùng: key == value (ví dụ categoryId == 'CAT001'), nối bằng AND / OR.
     * Giá trị luôn truyền bằng tham số ?, không ghép chuỗi, để tránh SQL injection.
     */
    private String toSql(Filter.Operand operand, List<Object> params) {
        if (!(operand instanceof Filter.Expression expression)) {
            throw new UnsupportedOperationException("Bộ lọc vector không hỗ trợ: " + operand);
        }
        return switch (expression.type()) {
            case AND -> "(" + toSql(expression.left(), params) + " AND " + toSql(expression.right(), params) + ")";
            case OR -> "(" + toSql(expression.left(), params) + " OR " + toSql(expression.right(), params) + ")";
            case EQ, NE -> {
                if (!(expression.left() instanceof Filter.Key key) || !(expression.right() instanceof Filter.Value value)) {
                    throw new UnsupportedOperationException("Bộ lọc vector không hỗ trợ: " + expression);
                }
                String name = key.key().replace("\"", "").replace("'", "");
                if (!SAFE_NAME.matcher(name).matches()) {
                    throw new IllegalArgumentException("Tên trường lọc không hợp lệ: " + key.key());
                }
                params.add(String.valueOf(value.value()));
                yield "JSON_UNQUOTE(JSON_EXTRACT(metadata, '$." + name + "')) "
                        + (expression.type() == Filter.ExpressionType.EQ ? "=" : "<>") + " ?";
            }
            default -> throw new UnsupportedOperationException("Bộ lọc vector không hỗ trợ: " + expression.type());
        };
    }

    // ===================== Chuyển đổi dữ liệu =====================

    /** float[] -> "[0.12,-0.03,...]": dạng chuỗi TiDB tự đổi sang kiểu VECTOR. */
    private static String toVectorLiteral(float[] vector) {
        StringBuilder text = new StringBuilder(vector.length * 12).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                text.append(',');
            }
            text.append(vector[i]);
        }
        return text.append(']').toString();
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không chuyển được metadata sang JSON", e);
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { });
        } catch (JsonProcessingException e) {
            log.warn("Metadata vector không đọc được: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
