package iuh.fit.laven.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thông tin chữ ký số trả về cho React để tải ảnh trực tiếp lên Cloudinary.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageUploadSignatureResponse {
    private String cloudName;
    private String apiKey;
    private long timestamp;
    private String publicId;
    private String signature;
}
