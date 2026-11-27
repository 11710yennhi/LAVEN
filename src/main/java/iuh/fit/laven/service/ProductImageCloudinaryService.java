package iuh.fit.laven.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import iuh.fit.laven.dto.response.ProductImageUploadSignatureResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Xử lý chữ ký số Cloudinary (Direct Upload) và xóa ảnh trên Cloudinary.
 * Server LAVEN không nhận hay lưu trữ file nhị phân của ảnh.
 */
@Service
public class ProductImageCloudinaryService {
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public ProductImageCloudinaryService(
        @Value("${CLOUDINARY_CLOUD_NAME:}") String cloudName,
        @Value("${CLOUDINARY_API_KEY:}") String apiKey,
        @Value("${CLOUDINARY_API_SECRET:}") String apiSecret
    ) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    /**
     * Tạo chữ ký số để React gửi file thẳng lên Cloudinary.
     */
    public ProductImageUploadSignatureResponse signUpload(String productId, String categoryName) {
        requireConfiguration();
        long timestamp = Instant.now().getEpochSecond();
        String publicId = "laven/products/" + productId + "/" + UUID.randomUUID();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("timestamp", timestamp);
        parameters.put("public_id", publicId);
        String assetFolder = categoryFolder(categoryName);
        if (assetFolder != null) {
            parameters.put("asset_folder", assetFolder);
        }

        Cloudinary cloudinary = getCloudinary();
        cloudinary.signRequest(parameters, Map.of());

        return new ProductImageUploadSignatureResponse(
            cloudName, apiKey, timestamp, publicId, (String) parameters.get("signature"), assetFolder
        );
    }

    /**
     * Kiểm tra URL ảnh trả về từ Cloudinary có hợp lệ và đúng sản phẩm hay không.
     */
    public String validateImageUrl(String imageUrl, String productId) {
        requireConfiguration();
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL ảnh Cloudinary không được để trống");
        }

        try {
            URI uri = URI.create(imageUrl.trim());
            String path = uri.getPath();
            String prefix = "/" + cloudName + "/image/upload/";

            // Phải là HTTPS, đúng host res.cloudinary.com, đúng cloudName và đúng thư mục của sản phẩm
            if ("https".equalsIgnoreCase(uri.getScheme())
                && "res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                && path != null && path.startsWith(prefix)) {

                String relativePath = path.substring(prefix.length());
                String expectedPattern = "^(v\\d+/)?laven/products/" + Pattern.quote(productId) + "/[A-Za-z0-9_-]+(\\.[A-Za-z0-9]+)?$";

                if (relativePath.matches(expectedPattern)) {
                    return uri.toString();
                }
            }
        } catch (IllegalArgumentException ignored) {
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL ảnh Cloudinary không hợp lệ hoặc không thuộc về sản phẩm này");
    }

    /**
     * Xóa ảnh trực tiếp trên Cloudinary bằng URL.
     */
    public void deleteByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || cloudName.isBlank()) {
            return;
        }
        try {
            URI uri = URI.create(imageUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                || uri.getPath() == null || !uri.getPath().startsWith("/" + cloudName + "/image/upload/")) {
                return;
            }
            String publicId = extractPublicId(uri.getPath());
            if (publicId != null && !publicId.isBlank()) {
                requireConfiguration();
                Map<?, ?> result = getCloudinary().uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
                if (!"ok".equals(result.get("result")) && !"not found".equals(result.get("result"))) {
                    throw new IllegalStateException("Cloudinary không xác nhận xóa ảnh");
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Không xóa được ảnh sản phẩm trên Cloudinary", ex);
        }
    }
    /**
     * Bóc tách public_id từ URL ảnh Cloudinary.
     */
    public String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        try {
            int uploadIdx = imageUrl.indexOf("/upload/");
            if (uploadIdx == -1) {
                return null;
            }
            String sub = imageUrl.substring(uploadIdx + "/upload/".length());
            // Bỏ tiền tố version ví dụ v1731934000/
            if (sub.startsWith("v") && sub.indexOf('/') != -1) {
                String potentialVer = sub.substring(1, sub.indexOf('/'));
                if (potentialVer.chars().allMatch(Character::isDigit)) {
                    sub = sub.substring(sub.indexOf('/') + 1);
                }
            }
            // Bỏ phần mở rộng file .jpg, .png...
            int lastDot = sub.lastIndexOf('.');
            return lastDot != -1 ? sub.substring(0, lastDot) : sub;
        } catch (Exception ex) {
            return null;
        }
    }

    private String categoryFolder(String categoryName) {
        if (categoryName == null) {
            return null;
        }
        String name = Normalizer.normalize(categoryName, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").strip().toLowerCase(Locale.ROOT);
        if (name.matches("va\\s*li(?:\\b.*)?")) return "vali";
        if (name.matches("ba\\s*lo(?:\\b.*)?")) return "balo";
        if (name.matches("tui\\s*xach(?:\\b.*)?")) return "tuixach";
        return null;
    }

    private Cloudinary getCloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret
        ));
    }

    private void requireConfiguration() {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Chưa cấu hình CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY và CLOUDINARY_API_SECRET");
        }
    }
}
