package iuh.fit.laven.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO nhận URL ảnh từ React gửi về sau khi tải ảnh trực tiếp lên Cloudinary.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageRequest {
    @NotBlank(message = "URL ảnh không được để trống")
    private String imageUrl;
}
