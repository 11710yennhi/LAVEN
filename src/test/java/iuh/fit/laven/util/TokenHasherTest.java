package iuh.fit.laven.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TokenHasherTest {

    @Test
    void hash_abc_dungSha256() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                TokenHasher.hash("abc"));
    }

    @Test
    void hash_dai64KyTu() {
        assertEquals(64, TokenHasher.hash("bat-ky-chuoi-nao").length());
    }

    @Test
    void generateToken_haiLanKhacNhau() {
        assertNotEquals(TokenHasher.generateToken(), TokenHasher.generateToken());
    }
}