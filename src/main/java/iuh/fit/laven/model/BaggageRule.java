package iuh.fit.laven.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import iuh.fit.laven.model.enums.BaggageType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Quy định hành lý của một hãng theo loại (xách tay / ký gửi) và hạng vé.
 * Mỗi (hãng, loại, hạng vé) chỉ có một dòng.
 */
@Entity
@Table(name = "baggage_rules", uniqueConstraints = @UniqueConstraint(
    name = "uk_baggage_rule", columnNames = {"airlineId", "baggageType", "fareClass"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaggageRule {
    @Id
    @Column(length = 20, nullable = false, updatable = false)
    private String baggageRuleId;

    /** Hãng lấy theo đường dẫn API, không nhận từ body. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "airlineId", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Airline airline;

    @NotNull(message = "Loại hành lý không được rỗng")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BaggageType baggageType;

    @NotBlank(message = "Hạng vé không được rỗng")
    @Size(max = 100, message = "Hạng vé tối đa 100 ký tự")
    @Column(nullable = false, length = 100)
    private String fareClass;

    /** Số kiện được mang; 0 = hạng vé này không có hành lý loại đó. */
    @NotNull(message = "Số kiện không được rỗng")
    @PositiveOrZero(message = "Số kiện không được âm")
    @Column(nullable = false)
    private Integer pieceCount;

    /** Cân nặng tối đa mỗi kiện (kg). */
    @NotNull(message = "Cân nặng tối đa không được rỗng")
    @Positive(message = "Cân nặng tối đa phải lớn hơn 0")
    @Column(nullable = false)
    private Double maxWeight;

    /** Kích thước tối đa (cm); nhập đủ cả 3 chiều hoặc bỏ trống cả 3. */
    @Positive(message = "Chiều dài tối đa phải lớn hơn 0")
    private Double maxLength;

    @Positive(message = "Chiều rộng tối đa phải lớn hơn 0")
    private Double maxWidth;

    @Positive(message = "Chiều cao tối đa phải lớn hơn 0")
    private Double maxHeight;

    /** Tổng 3 chiều tối đa (cm), thường dùng cho hành lý ký gửi. */
    @Positive(message = "Tổng 3 chiều tối đa phải lớn hơn 0")
    private Double maxTotalDimension;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    @Column(length = 500)
    private String note;
}
