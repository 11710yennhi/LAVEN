package iuh.fit.laven.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Admin tạo tài khoản nhân viên (UC32). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateStaffRequest {
    private String email;
    private String phone;
    private String password;      // mật khẩu ban đầu
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private LocalDate hireDate;   // bắt buộc
    private String addressLine;
    private String ward;
    private String city;
}
