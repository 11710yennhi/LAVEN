package iuh.fit.laven.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String email;
    private String phone;
    private String password;      // mật khẩu gốc, service sẽ băm bằng BCrypt
    private String fullName;
    private LocalDate dateOfBirth; // JSON gửi dạng "2002-05-06"
    private String gender;         // có thể rỗng
}