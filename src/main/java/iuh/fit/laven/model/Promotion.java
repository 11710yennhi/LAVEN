package iuh.fit.laven.model;

import iuh.fit.laven.model.enums.DiscountType;
import iuh.fit.laven.model.enums.PromotionApplyType;
import iuh.fit.laven.model.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Entity
@Table(name = "promotions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Promotion {

    private static final Pattern PROMOTION_ID_PATTERN = Pattern.compile("^PRM\\d{16}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{4,20}$");

    @Id
    @Column(length = 19, nullable = false)
    private String promotionId;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PromotionApplyType applyType;

    // ORDER/SHIPPING: bắt buộc; PRODUCT: null. Không trùng (unique)
    @Column(length = 20, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private Integer discountValue;

    @Column(nullable = false)
    private Integer minOrderValue = 0;

    @Column(nullable = false)
    private Integer maxDiscountValue = 0;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean status;

    // null = mã công khai; khác null = mã riêng (bồi thường)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId")
    private User customer;

    // null = không giới hạn
    private Integer maxOrderUsage;

    // ===== 3.1 Constructor đầy đủ =====
    // Thứ tự có chủ đích: applyType trước code, discountType trước discountValue, startDate trước endDate
    public Promotion(String promotionId, String name, String description, PromotionApplyType applyType,
                     String code, DiscountType discountType, Integer discountValue, Integer minOrderValue,
                     Integer maxDiscountValue, LocalDate startDate, LocalDate endDate, boolean status,
                     User customer, Integer maxOrderUsage) {
        setPromotionId(promotionId);
        setName(name);
        setDescription(description);
        setApplyType(applyType);
        setCode(code);
        setDiscountType(discountType);
        setDiscountValue(discountValue);
        setMinOrderValue(minOrderValue);
        setMaxDiscountValue(maxDiscountValue);
        setStartDate(startDate);
        setEndDate(endDate);
        setStatus(status);
        setCustomer(customer);
        setMaxOrderUsage(maxOrderUsage);
    }

    // ===== 2. Setter có kiểm tra (mục h, bảng Promotion) =====

    public void setPromotionId(String promotionId) {
        if (promotionId == null || !PROMOTION_ID_PATTERN.matcher(promotionId).matches()) {
            throw new IllegalArgumentException("Mã khuyến mãi không hợp lệ");
        }
        this.promotionId = promotionId;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên khuyến mãi không được rỗng");
        }
        String value = name.trim();
        if (value.length() > 100) {
            throw new IllegalArgumentException("Tên khuyến mãi tối đa 100 ký tự");
        }
        this.name = value;
    }

    public void setDescription(String description) {
        String value = description == null || description.isBlank() ? null : description.trim();
        if (value != null && value.length() > 255) {
            throw new IllegalArgumentException("Mô tả tối đa 255 ký tự");
        }
        this.description = value;
    }

    public void setApplyType(PromotionApplyType applyType) {
        if (applyType == null) {
            throw new IllegalArgumentException("Loại áp dụng khuyến mãi không hợp lệ");
        }
        this.applyType = applyType;
    }

    // Kiểm tra trùng mã và mã dành riêng KM00000000 cần truy vấn DB: nằm ở PromotionService
    public void setCode(String code) {
        String value = code == null || code.isBlank() ? null : code.trim().toUpperCase();
        if (applyType == PromotionApplyType.PRODUCT) {
            if (value != null) {
                throw new IllegalArgumentException("Mã giảm giá không hợp lệ");
            }
            this.code = null;   // khuyến mãi sản phẩm áp dụng tự động, không có mã
            return;
        }
        if (value == null) {
            throw new IllegalArgumentException(
                    "Mã giảm giá không được rỗng đối với khuyến mãi đơn hàng/vận chuyển");
        }
        if (!CODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Mã giảm giá không hợp lệ");
        }
        this.code = value;
    }

    public void setDiscountType(DiscountType discountType) {
        if (discountType == null) {
            throw new IllegalArgumentException("Loại giảm giá không hợp lệ");
        }
        this.discountType = discountType;
    }

    public void setDiscountValue(Integer discountValue) {
        if (discountValue == null || discountValue <= 0) {
            throw new IllegalArgumentException("Giá trị giảm phải lớn hơn 0");
        }
        if (discountType == DiscountType.PERCENT && discountValue > 100) {
            throw new IllegalArgumentException("Phần trăm giảm không được vượt quá 100");
        }
        this.discountValue = discountValue;
    }

    public void setMinOrderValue(Integer minOrderValue) {
        int value = minOrderValue == null ? 0 : minOrderValue;
        if (value < 0) {
            throw new IllegalArgumentException("Giá trị đơn hàng tối thiểu phải lớn hơn hoặc bằng 0");
        }
        this.minOrderValue = value;
    }

    // 0 = không giới hạn (khi PERCENT)
    public void setMaxDiscountValue(Integer maxDiscountValue) {
        int value = maxDiscountValue == null ? 0 : maxDiscountValue;
        if (value < 0) {
            throw new IllegalArgumentException("Giá trị giảm tối đa phải lớn hơn hoặc bằng 0");
        }
        this.maxDiscountValue = value;
    }

    public void setStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Ngày bắt đầu không được rỗng");
        }
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        if (endDate == null || (startDate != null && endDate.isBefore(startDate))) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        this.endDate = endDate;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    // Mã riêng chỉ cấp cho khách hàng (Admin, Staff không mua hàng)
    public void setCustomer(User customer) {
        if (customer != null && customer.getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Khách hàng không hợp lệ");
        }
        this.customer = customer;
    }

    public void setMaxOrderUsage(Integer maxOrderUsage) {
        if (maxOrderUsage != null && maxOrderUsage < 1) {
            throw new IllegalArgumentException("Số lượt sử dụng tối đa phải lớn hơn hoặc bằng 1");
        }
        this.maxOrderUsage = maxOrderUsage;
    }

    @Override
    public String toString() {
        return "Promotion{promotionId='" + promotionId + "', name='" + name + "', description='" + description
                + "', applyType=" + applyType + ", code='" + code + "', discountType=" + discountType
                + ", discountValue=" + discountValue + ", minOrderValue=" + minOrderValue
                + ", maxDiscountValue=" + maxDiscountValue + ", startDate=" + startDate
                + ", endDate=" + endDate + ", status=" + status + ", customerId='"
                + (customer == null ? null : customer.getUserId()) + "', maxOrderUsage=" + maxOrderUsage + "}";
    }
}