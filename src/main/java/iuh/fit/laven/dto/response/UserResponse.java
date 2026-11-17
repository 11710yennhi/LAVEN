package iuh.fit.laven.dto.response;

import iuh.fit.laven.model.User;
import iuh.fit.laven.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Thông tin người dùng trả ra API. Không có passwordHash. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String userId;
    private String email;
    private String phone;
    private String fullName;
    private String addressLine;
    private String ward;
    private String city;
    private UserRole role;
    private boolean status;
    private boolean pendingLock;
    private LocalDate dateOfBirth;
    private String gender;
    private LocalDate hireDate;

    public static UserResponse from(User u) {
        return UserResponse.builder()
                .userId(u.getUserId())
                .email(u.getEmail())
                .phone(u.getPhone())
                .fullName(u.getFullName())
                .addressLine(u.getAddressLine())
                .ward(u.getWard())
                .city(u.getCity())
                .role(u.getRole())
                .status(u.isStatus())
                .pendingLock(u.isPendingLock())
                .dateOfBirth(u.getDateOfBirth())
                .gender(u.getGender())
                .hireDate(u.getHireDate())
                .build();
    }
}
