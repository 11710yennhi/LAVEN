package iuh.fit.laven.dto.request.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Tham số của GET /api/public/ai/search. */
@Getter
@Setter
public class SemanticSearchRequest {

    @NotBlank(message = "Nội dung tìm kiếm không được rỗng")
    @Size(max = 500, message = "Nội dung tìm kiếm tối đa 500 ký tự")
    private String query;

    private String categoryId;

    @PositiveOrZero(message = "Giá tối thiểu không được âm")
    private Integer minPrice;

    @PositiveOrZero(message = "Giá tối đa không được âm")
    private Integer maxPrice;

    @Min(value = 1, message = "topK phải từ 1 đến 20")
    @Max(value = 20, message = "topK phải từ 1 đến 20")
    private Integer topK = 10;
}