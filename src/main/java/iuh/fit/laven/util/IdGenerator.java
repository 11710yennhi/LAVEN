package iuh.fit.laven.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

/** Sinh mã dạng TIỀN_TỐ + yyyyMMddHHmmssSS (SS = 2 chữ số đầu của mili giây). */
public final class IdGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSS");
    private static final int MAX_ATTEMPTS = 20;

    private IdGenerator() {
    }

    public static String next(String prefix) {
        return prefix + LocalDateTime.now().format(FORMATTER);
    }

    /** Sinh mã, nếu trùng thì chờ 10ms rồi sinh lại (theo tài liệu mục i.4). */
    public static String nextUnique(String prefix, Predicate<String> exists) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String id = next(prefix);
            if (!exists.test(id)) {
                return id;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IllegalStateException("Không sinh được mã duy nhất cho tiền tố " + prefix);
    }
}
