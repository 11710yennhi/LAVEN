package iuh.fit.laven.model;

import iuh.fit.laven.model.enums.UserRole;
import iuh.fit.laven.util.TokenHasher;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenEntitiesTest {

    private final LocalDateTime now = LocalDateTime.now();
    private final String validHash = TokenHasher.hash("token-goc");   // 64 ký tự hex

    private User khach() {
        return new User("USR2026111722000001", "an@gmail.com", "0901234567", "hash", "Nguyễn Văn An",
                null, null, null, UserRole.CUSTOMER, true, LocalDate.of(2002, 5, 6), null,
                null, null, null, false, null, 0, null);
    }

    @Test
    void pendingHopLe_emailChuThuong() {
        PendingRegistration p = new PendingRegistration(validHash, "Binh@Gmail.com", "0912345678", "hash",
                "Trần Bình", LocalDate.of(2001, 1, 1), null, now.plusMinutes(30), now);
        assertEquals("binh@gmail.com", p.getEmail());
    }

    @Test
    void pendingTokenHashKhongPhai64Hex_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new PendingRegistration("abc", "binh@gmail.com", "0912345678", "hash", "Trần Bình",
                        LocalDate.of(2001, 1, 1), null, now.plusMinutes(30), now));
        assertEquals("Mã xác thực không hợp lệ", ex.getMessage());
    }

    @Test
    void pendingThieuHanHetHan_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new PendingRegistration(validHash, "binh@gmail.com", "0912345678", "hash", "Trần Bình",
                        LocalDate.of(2001, 1, 1), null, null, now));
        assertEquals("Thời điểm hết hạn không được rỗng", ex.getMessage());
    }

    @Test
    void resetTokenThieuUser_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new PasswordResetToken(validHash, null, now.plusMinutes(30), now));
        assertEquals("Người dùng không được rỗng", ex.getMessage());
    }

    @Test
    void refreshTokenHopLe() {
        RefreshToken t = new RefreshToken(validHash, khach(), now.plusDays(7), now);
        assertEquals(validHash, t.getTokenHash());
    }

    @Test
    void refreshTokenHashSai_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new RefreshToken("khong-phai-hash", khach(), now.plusDays(7), now));
        assertEquals("Mã phiên đăng nhập không hợp lệ", ex.getMessage());
    }
}