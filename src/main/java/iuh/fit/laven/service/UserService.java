package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.CreateStaffRequest;
import iuh.fit.laven.dto.request.UpdateProfileRequest;
import iuh.fit.laven.dto.response.UserResponse;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.User;
import iuh.fit.laven.model.enums.UserRole;
import iuh.fit.laven.repository.RefreshTokenRepository;
import iuh.fit.laven.repository.UserRepository;
import iuh.fit.laven.util.IdGenerator;
import iuh.fit.laven.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    public static final int AUTO_LOCK_THRESHOLD = 5;
    public static final String AUTO_LOCK_REASON = "Tự động khóa: đạt 5 đơn giao thất bại";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    // ---------------- UC15: hồ sơ cá nhân ----------------
    @Transactional(readOnly = true)
    public UserResponse getProfile(String userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setPhone(ValidationUtil.validatePhone(request.getPhone()));
        user.setFullName(ValidationUtil.validateFullName(request.getFullName()));
        user.setDateOfBirth(ValidationUtil.validateDateOfBirth(request.getDateOfBirth()));
        user.setGender(ValidationUtil.optional(request.getGender()));
        user.setAddressLine(ValidationUtil.optional(request.getAddressLine(), 255,
                "Số nhà, tên đường tối đa 255 ký tự"));
        user.setWard(ValidationUtil.optional(request.getWard(), 100, "Phường/xã tối đa 100 ký tự"));
        user.setCity(ValidationUtil.optional(request.getCity(), 100, "Tỉnh/thành phố tối đa 100 ký tự"));
        return UserResponse.from(userRepository.save(user));
    }

    // ---------------- UC32: Admin quản lý nhân viên ----------------
    @Transactional
    public UserResponse createStaff(CreateStaffRequest request) {
        String email = ValidationUtil.validateEmail(request.getEmail());
        String phone = ValidationUtil.validatePhone(request.getPhone());
        ValidationUtil.validatePassword(request.getPassword());
        String fullName = ValidationUtil.validateFullName(request.getFullName());
        LocalDate dateOfBirth = ValidationUtil.validateDateOfBirth(request.getDateOfBirth());
        if (request.getHireDate() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Ngày vào làm không được rỗng đối với nhân viên");
        }
        if (userRepository.existsByEmail(email)) {
            throw new AppException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }

        User staff = User.builder()
                .userId(IdGenerator.nextUnique("USR", userRepository::existsById))
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(fullName)
                .role(UserRole.STAFF)
                .dateOfBirth(dateOfBirth)
                .gender(ValidationUtil.optional(request.getGender()))
                .hireDate(request.getHireDate())
                .addressLine(ValidationUtil.optional(request.getAddressLine(), 255,
                        "Số nhà, tên đường tối đa 255 ký tự"))
                .ward(ValidationUtil.optional(request.getWard(), 100, "Phường/xã tối đa 100 ký tự"))
                .city(ValidationUtil.optional(request.getCity(), 100, "Tỉnh/thành phố tối đa 100 ký tự"))
                .build();
        return UserResponse.from(userRepository.save(staff));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listStaffs() {
        return userRepository.findByRole(UserRole.STAFF).stream().map(UserResponse::from).toList();
    }

    // ---------------- UC35: tự động khóa tài khoản (quy trình 15) ----------------

    /**
     * Gọi MỖI KHI một đơn của khách chuyển sang DELIVERY_FAILED (quy trình 15, bước 1-2).
     *
     * @param failedDeliveryCount tổng số đơn DELIVERY_FAILED của khách từ trước tới nay (đã gồm đơn vừa thất bại)
     * @param hasActiveOrders     khách còn đơn PENDING, CONFIRMED, SHIPPING hoặc DELIVERED hay không
     */
    @Transactional
    public void lockIfNeeded(String customerId, long failedDeliveryCount, boolean hasActiveOrders) {
        User user = findUser(customerId);
        if (user.getRole() != UserRole.CUSTOMER || !user.isStatus()) {
            return;
        }
        if (failedDeliveryCount < AUTO_LOCK_THRESHOLD) {
            return;
        }
        if (hasActiveOrders) {
            user.setPendingLock(true);
            userRepository.save(user);
        } else {
            lock(user);
        }
    }

    /**
     * Gọi MỖI KHI một đơn của khách kết thúc (COMPLETED, CANCELLED, DELIVERY_FAILED) (bước 3):
     * nếu khách đang pendingLock và không còn đơn hoạt động thì khóa.
     */
    @Transactional
    public void lockIfPendingAndNoActiveOrders(String customerId, boolean hasActiveOrders) {
        User user = findUser(customerId);
        if (user.isStatus() && user.isPendingLock() && !hasActiveOrders) {
            lock(user);
        }
    }

    private void lock(User user) {
        user.setStatus(false);
        user.setPendingLock(false);
        user.setLockedDate(LocalDate.now());
        user.setLockedReason(AUTO_LOCK_REASON);
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }
}
