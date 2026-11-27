package iuh.fit.laven.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Hãng bay nội địa, admin nhập để chatbot kiểm tra quy định hành lý. */
@Entity
@Table(name = "airlines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Airline {
    @Id
    @Column(length = 20, nullable = false, updatable = false)
    private String airlineId;

    @NotBlank(message = "Tên hãng bay không được rỗng")
    @Size(max = 100, message = "Tên hãng bay tối đa 100 ký tự")
    @Column(nullable = false, unique = true, length = 100)
    private String airlineName;

    @NotBlank(message = "Link chính sách hành lý không được rỗng")
    @Size(max = 500, message = "Link chính sách tối đa 500 ký tự")
    @Column(nullable = false, length = 500)
    private String baggagePolicyUrl;

    /** Ngày admin kiểm tra lại quy định trên trang của hãng. */
    @NotNull(message = "Ngày kiểm tra không được rỗng")
    @Column(nullable = false)
    private LocalDate verifiedAt;

    @Column(nullable = false)
    private boolean status = true;
}
