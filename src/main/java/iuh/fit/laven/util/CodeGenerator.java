package iuh.fit.laven.util;

import java.security.SecureRandom;
import java.util.function.Predicate;

/** Sinh mã giảm giá gồm chữ in hoa A-Z và số 0-9. */
public final class CodeGenerator {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 50;

    private CodeGenerator() {
    }

    public static String random(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public static String nextUnique(int length, Predicate<String> exists) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String code = random(length);
            if (!exists.test(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không sinh được mã giảm giá duy nhất");
    }
}
