package iuh.fit.laven.service;

import iuh.fit.laven.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String baseUrl;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String fromAddress,
                        @Value("${app.base-url}") String baseUrl,
                        @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
        this.frontendUrl = frontendUrl;
    }

    /** Link xác thực đăng ký. token là token GỐC (chưa băm). Gửi lỗi thì ném AppException để rollback. */
    public void sendVerificationEmail(String to, String token) {
        String link = baseUrl + "/api/auth/verify?token=" + token;
        String content = "Chào bạn,\n\n"
                + "Cảm ơn bạn đã đăng ký tài khoản LAVEN. "
                + "Vui lòng bấm vào link dưới đây để xác thực email (có hiệu lực trong 30 phút):\n\n"
                + link + "\n\n"
                + "Nếu bạn không đăng ký, hãy bỏ qua email này.\n\n"
                + "LAVEN";
        sendOrThrow(to, "Xác thực tài khoản LAVEN", content);
    }

    /** Link đặt lại mật khẩu trỏ tới trang của React (giai đoạn sau). */
    public void sendPasswordResetEmail(String to, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String content = "Chào bạn,\n\n"
                + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản LAVEN của bạn. "
                + "Bấm vào link dưới đây để đặt mật khẩu mới (có hiệu lực trong 30 phút):\n\n"
                + link + "\n\n"
                + "Nếu bạn không yêu cầu, hãy bỏ qua email này.\n\n"
                + "LAVEN";
        sendOrThrow(to, "Đặt lại mật khẩu LAVEN", content);
    }

    /** Báo mã bồi thường khi Staff hủy đơn. Gửi lỗi chỉ ghi log để không làm hỏng việc hủy đơn. */
    public void sendCompensationEmail(String to, String code, LocalDate expiryDate) {
        String content = "Chào bạn,\n\n"
                + "Rất tiếc đơn hàng của bạn đã bị cửa hàng hủy. "
                + "LAVEN xin gửi tặng bạn mã giảm giá bồi thường:\n\n"
                + "Mã: " + code + "\n"
                + "Chỉ áp dụng cho tài khoản này, dùng cho một đơn hàng, "
                + "hạn sử dụng đến " + expiryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".\n\n"
                + "Xin lỗi bạn vì sự bất tiện này.\n\n"
                + "LAVEN";
        try {
            send(to, "Mã giảm giá bồi thường từ LAVEN", content);
        } catch (MailException ex) {
            log.warn("Không gửi được email mã bồi thường tới {}", to, ex);
        }
    }

    private void sendOrThrow(String to, String subject, String content) {
        try {
            send(to, subject, content);
        } catch (MailException ex) {
            log.error("Không gửi được email tới {} (tiêu đề: {})", to, subject, ex);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không gửi được email, vui lòng thử lại sau");
        }
    }

    private void send(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}
