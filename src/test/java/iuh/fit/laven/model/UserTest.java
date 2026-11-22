package iuh.fit.laven.model;

import iuh.fit.laven.model.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    private User khachHopLe() {
        return new User("USR2026111622150012", "An.Nguyen@gmail.com", "0901234567", "hash-bcrypt",
                "Nguyễn Văn An", null, null, null, UserRole.CUSTOMER, true,
                LocalDate.of(2002, 5, 6), null, null, null, null, false, null, null, null);
    }

    @Test
    void taoKhachHopLe_emailChuThuong_soLanSaiBang0() {
        User u = khachHopLe();
        assertEquals("an.nguyen@gmail.com", u.getEmail());
        assertEquals(0, u.getFailedLoginAttempts());
    }

    @Test
    void emailKhongPhaiGmail_baoLoi() {
        User u = khachHopLe();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> u.setEmail("an@yahoo.com"));
        assertEquals("Email không hợp lệ", ex.getMessage());
    }

    @Test
    void soDienThoaiKhong10ChuSo_baoLoi() {
        User u = khachHopLe();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> u.setPhone("09012"));
        assertEquals("Số điện thoại không hợp lệ", ex.getMessage());
    }

    @Test
    void hoTenCoSo_baoLoi() {
        User u = khachHopLe();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> u.setFullName("An 123"));
        assertEquals("Họ tên không hợp lệ", ex.getMessage());
    }

    @Test
    void ngaySinhHomNay_baoLoi() {
        User u = khachHopLe();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> u.setDateOfBirth(LocalDate.now()));
        assertEquals("Ngày sinh không hợp lệ", ex.getMessage());
    }

    @Test
    void nhanVienThieuNgayVaoLam_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new User("USR2026111622150013", "staff1@gmail.com", "0901234567", "hash", "Trần Bình",
                        null, null, null, UserRole.STAFF, true, LocalDate.of(1999, 1, 1), null,
                        null, null, null, false, null, 0, null));
        assertEquals("Ngày vào làm không được rỗng đối với nhân viên", ex.getMessage());
    }

    @Test
    void khoaTaiKhoanKhongCoLyDo_baoLoi() {
        User u = khachHopLe();
        u.setStatus(false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> u.setLockedReason(" "));
        assertEquals("Lý do khóa không được rỗng khi khóa tài khoản", ex.getMessage());
    }
}