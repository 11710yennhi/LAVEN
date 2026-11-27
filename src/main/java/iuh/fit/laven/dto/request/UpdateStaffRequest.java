package iuh.fit.laven.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Admin sửa thông tin nhân viên (UC32). Không cho đổi email và mật khẩu. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStaffRequest {
    private String phone;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private LocalDate hireDate;   // bắt buộc với nhân viên
    private String addressLine;
    private String ward;
    private String city;
}