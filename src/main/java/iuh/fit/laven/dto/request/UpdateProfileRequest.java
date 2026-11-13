package iuh.fit.laven.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Cập nhật thông tin cá nhân (UC15). Không cho đổi email, role, status. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String phone;
    private String fullName;
    private String addressLine;
    private String ward;
    private String city;
    private LocalDate dateOfBirth;
    private String gender;
}
