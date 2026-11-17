package iuh.fit.laven.config;

import iuh.fit.laven.model.User;
import iuh.fit.laven.model.enums.UserRole;
import iuh.fit.laven.repository.UserRepository;
import iuh.fit.laven.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Khi khởi động: nếu DB chưa có tài khoản ADMIN nào thì tạo một ADMIN
 * từ ADMIN_EMAIL / ADMIN_PASSWORD trong file .env.
 * Tài liệu không quy định cách tạo ADMIN đầu tiên; nhóm chọn cách này (quyết định ngày 17/11).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return; // đã có ADMIN thì không làm gì, chạy lại nhiều lần cũng không tạo trùng
        }
        if (email.isBlank() || password.isBlank()) {
            log.warn("Chưa có tài khoản ADMIN. Khai báo ADMIN_EMAIL và ADMIN_PASSWORD trong file .env rồi chạy lại");
            return;
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Không tạo ADMIN: email {} đã được tài khoản khác sử dụng", normalizedEmail);
            return;
        }

        // Admin không mua hàng nên KHÔNG tạo Cart. hireDate bắt buộc với ADMIN (User.setHireDate)
        User admin = new User(
                IdGenerator.nextUnique("USR", userRepository::existsById),
                normalizedEmail, "0900000000", passwordEncoder.encode(password), "Quản Trị Viên",
                null, null, null,
                UserRole.ADMIN, true,
                LocalDate.of(1990, 1, 1), null,
                LocalDate.now(), null, null,
                false, null, 0, null);
        userRepository.save(admin);
        log.info("Đã tạo tài khoản ADMIN đầu tiên: {}", normalizedEmail);
    }
}