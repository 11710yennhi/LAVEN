package iuh.fit.laven.dto.response;

import iuh.fit.laven.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** JWT LAVEN đã xác thực; React dùng token này để đăng nhập Firebase. */
@Getter
@AllArgsConstructor
public class ChatSessionResponse {
    private String firebaseToken;
    private String userId;
    private String fullName;
    private UserRole role;
    private FirebaseWebConfig firebaseConfig;
    private List<Customer> customers;

    @Getter
    @AllArgsConstructor
    public static class FirebaseWebConfig {
        private String apiKey;
        private String authDomain;
        private String databaseURL;
        private String projectId;
        private String appId;
    }

    @Getter
    @AllArgsConstructor
    public static class Customer {
        private String userId;
        private String fullName;
    }
}
