package iuh.fit.laven.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body của POST /api/users/me/email (đổi email, mọi role). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEmailRequest {
    private String newEmail;          // email mới, phải @gmail.com và chưa ai dùng
    private String currentPassword;   // mật khẩu hiện tại để xác nhận chính chủ
}
