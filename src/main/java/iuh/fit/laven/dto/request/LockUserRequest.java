package iuh.fit.laven.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Khóa tài khoản thủ công: bắt buộc ghi lý do (dùng cho Staff - UC32 và Customer - UC30). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LockUserRequest {
    private String reason;
}