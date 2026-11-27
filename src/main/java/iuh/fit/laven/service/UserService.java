package iuh.fit.laven.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import iuh.fit.laven.dto.request.ChangeEmailRequest;
import iuh.fit.laven.dto.request.CreateStaffRequest;
import iuh.fit.laven.dto.request.LockUserRequest;
import iuh.fit.laven.dto.request.UpdateProfileRequest;
import iuh.fit.laven.dto.request.UpdateStaffRequest;
import iuh.fit.laven.dto.response.UserResponse;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.User;
import iuh.fit.laven.model.enums.UserRole;
import iuh.fit.laven.repository.RefreshTokenRepository;
import iuh.fit.laven.repository.UserRepository;
import iuh.fit.laven.security.JwtService;
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
    private static final String INVALID_EMAIL_CHANGE_LINK = "Link xác thực đổi email không hợp lệ hoặc đã hết hạn";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

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

    // ---------------- UC15 mở rộng: đổi email có xác thực (mọi role) ----------------

    /**
     * Bước 1: kiểm tra rồi gửi link xác thực tới email MỚI. Chưa đổi gì trong DB.
     * Thứ tự: định dạng -> khác email cũ -> mật khẩu hiện tại -> unique.
     */
    @Transactional(readOnly = true)
    public void requestEmailChange(String userId, ChangeEmailRequest request) {
        User user = findUser(userId);
        String newEmail = ValidationUtil.validateEmail(request.getNewEmail());
        if (newEmail.equals(user.getEmail())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Email mới phải khác email hiện tại");
        }
        String current = request.getCurrentPassword() == null ? "" : request.getCurrentPassword();
        if (!passwordEncoder.matches(current, user.getPasswordHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new AppException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
        String token = jwtService.generateEmailChangeToken(user, newEmail);
        emailService.sendEmailChangeVerification(newEmail, token);
    }

    /**
     * Bước 2: khách bấm link. Chỉ đổi khi token đúng, còn hạn, email hiện tại vẫn là email cũ
     * trong token (link dùng 1 lần) và email mới vẫn chưa ai dùng.
     */
    @Transactional
    public void confirmEmailChange(String token) {
        if (token == null || token.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, INVALID_EMAIL_CHANGE_LINK);
        }
        Claims claims;
        try {
            claims = jwtService.parseEmailChangeToken(token.trim());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, INVALID_EMAIL_CHANGE_LINK);
        }
        String oldEmail = claims.get(JwtService.OLD_EMAIL_CLAIM, String.class);
        String newEmail = claims.get(JwtService.NEW_EMAIL_CLAIM, String.class);
        User user = userRepository.findById(claims.getSubject())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, INVALID_EMAIL_CHANGE_LINK));
        // Email đã đổi rồi (bấm link lần 2) hoặc đã đổi sang email khác: link cũ hết hiệu lực
        if (oldEmail == null || newEmail == null || !oldEmail.equals(user.getEmail())) {
            throw new AppException(HttpStatus.BAD_REQUEST, INVALID_EMAIL_CHANGE_LINK);
        }
        if (!user.isStatus()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }
        // Kiểm tra lại: trong lúc chờ có thể đã có người dùng email này
        if (userRepository.existsByEmail(newEmail)) {
            throw new AppException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
        user.setEmail(newEmail);   // setter kiểm tra định dạng @gmail.com và chuyển chữ thường
        userRepository.save(user);
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

        // Quy trình 2: Admin tạo nhân viên ngay, không cần xác thực email
        User staff = new User(
                IdGenerator.nextUnique("USR", userRepository::existsById),
                email, phone, passwordEncoder.encode(request.getPassword()), fullName,
                ValidationUtil.optional(request.getAddressLine(), 255, "Số nhà, tên đường tối đa 255 ký tự"),
                ValidationUtil.optional(request.getWard(), 100, "Phường/xã tối đa 100 ký tự"),
                ValidationUtil.optional(request.getCity(), 100, "Tỉnh/thành phố tối đa 100 ký tự"),
                UserRole.STAFF, true,
                dateOfBirth, ValidationUtil.optional(request.getGender()),
                request.getHireDate(), null, null,   // hireDate, lockedDate, lockedReason
                false, null, 0, null);
        return UserResponse.from(userRepository.save(staff));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listStaffs() {
        return userRepository.findByRole(UserRole.STAFF).stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getStaff(String staffId) {
        return UserResponse.from(findStaff(staffId));
    }

    /** Sửa thông tin nhân viên (không đổi email, mật khẩu). Kiểm tra định dạng nằm trong setter của User. */
    @Transactional
    public UserResponse updateStaff(String staffId, UpdateStaffRequest request) {
        User staff = findStaff(staffId);
        staff.setPhone(request.getPhone());
        staff.setFullName(request.getFullName());
        staff.setDateOfBirth(request.getDateOfBirth());
        staff.setGender(request.getGender());
        staff.setHireDate(request.getHireDate());   // role = STAFF nên setter bắt buộc có ngày vào làm
        staff.setAddressLine(ValidationUtil.optional(request.getAddressLine(), 255,
                "Số nhà, tên đường tối đa 255 ký tự"));
        staff.setWard(ValidationUtil.optional(request.getWard(), 100, "Phường/xã tối đa 100 ký tự"));
        staff.setCity(ValidationUtil.optional(request.getCity(), 100, "Tỉnh/thành phố tối đa 100 ký tự"));
        return UserResponse.from(userRepository.save(staff));
    }

    /** Khóa thủ công: bắt buộc lý do, xóa toàn bộ refresh token (quy trình 3 bước 7). */
    @Transactional
    public UserResponse lockStaff(String staffId, LockUserRequest request) {
        User staff = findStaff(staffId);
        if (!staff.isStatus()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tài khoản đã bị khóa");
        }
        lock(staff, request == null ? null : request.getReason());
        return UserResponse.from(staff);
    }

    @Transactional
    public UserResponse unlockStaff(String staffId) {
        User staff = findStaff(staffId);
        unlock(staff);
        return UserResponse.from(staff);
    }

    // ---------------- UC30: Staff quản lý khách hàng ----------------
    @Transactional(readOnly = true)
    public List<UserResponse> searchCustomers(String keyword) {
        return userRepository.search(UserRole.CUSTOMER, ValidationUtil.optional(keyword)).stream()
                .map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getCustomer(String customerId) {
        return UserResponse.from(findCustomer(customerId));
    }

    /** Khóa thủ công: bắt buộc lý do, xóa toàn bộ refresh token. */
    @Transactional
    public UserResponse lockCustomer(String customerId, LockUserRequest request) {
        User customer = findCustomer(customerId);
        if (!customer.isStatus()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tài khoản đã bị khóa");
        }
        lock(customer, request == null ? null : request.getReason());
        return UserResponse.from(customer);
    }

    /** Mở khóa (quy trình 15 bước 4). Lần giao thất bại tiếp theo vẫn kích hoạt tự động khóa lại. */
    @Transactional
    public UserResponse unlockCustomer(String customerId) {
        User customer = findCustomer(customerId);
        unlock(customer);
        return UserResponse.from(customer);
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
            lock(user, AUTO_LOCK_REASON);
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
            lock(user, AUTO_LOCK_REASON);
        }
    }

    /**
     * Quy trình 15 bước 3: khách đang pendingLock thì không được đặt đơn mới.
     * Phần Đơn hàng (TV3) gọi ở ĐẦU hàm đặt đơn.
     */
    @Transactional(readOnly = true)
    public void checkCanPlaceOrder(String customerId) {
        User customer = findCustomer(customerId);
        if (!customer.isStatus()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }
        if (customer.isPendingLock()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Tài khoản đang chờ khóa, không thể đặt đơn mới");
        }
    }

    // ---------------- Nội bộ ----------------

    /** Dùng chung cho khóa thủ công và tự động khóa. */
    private void lock(User user, String reason) {
        user.setStatus(false);
        user.setLockedReason(reason);   // status đã = false nên setter bắt buộc có lý do
        user.setLockedDate(LocalDate.now());
        user.setPendingLock(false);
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);   // đăng xuất khỏi mọi thiết bị
    }

    /** Mở khóa (quy trình 15 bước 4): status = true, pendingLock = false, xóa lockedDate và lockedReason. */
    private void unlock(User user) {
        if (user.isStatus()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tài khoản đang hoạt động, không cần mở khóa");
        }
        user.setStatus(true);
        user.setPendingLock(false);
        user.setLockedDate(null);
        user.setLockedReason(null);
        userRepository.save(user);
    }

    /** Chỉ tìm tài khoản role = STAFF; id của ADMIN hay CUSTOMER đều coi như không tìm thấy. */
    private User findStaff(String staffId) {
        return userRepository.findById(staffId)
                .filter(u -> u.getRole() == UserRole.STAFF)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên"));
    }

    /** Chỉ tìm tài khoản role = CUSTOMER. */
    private User findCustomer(String customerId) {
        return userRepository.findById(customerId)
                .filter(u -> u.getRole() == UserRole.CUSTOMER)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }
}