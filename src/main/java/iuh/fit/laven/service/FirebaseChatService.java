package iuh.fit.laven.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import iuh.fit.laven.dto.response.ChatSessionResponse;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.User;
import iuh.fit.laven.model.enums.UserRole;
import iuh.fit.laven.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Tạo Firebase custom token; tin nhắn đi thẳng từ React đến Realtime Database. */
@Service
@RequiredArgsConstructor
public class FirebaseChatService {
    private final UserRepository userRepository;
    private FirebaseAuth firebaseAuth;

    @Value("${firebase.service-account-path:}") private String serviceAccountPath;
    @Value("${firebase.project-id:}") private String projectId;
    @Value("${firebase.web-api-key:}") private String webApiKey;
    @Value("${firebase.auth-domain:}") private String authDomain;
    @Value("${firebase.database-url:}") private String databaseUrl;
    @Value("${firebase.app-id:}") private String appId;

    public ChatSessionResponse createSession(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại"));
        if (!user.isStatus()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }
        requireConfig();
        String firebaseToken;
        try {
            // uid = userId LAVEN; Firebase Rules kiểm tra uid và vai trò này.
            firebaseToken = getFirebaseAuth().createCustomToken(userId, Map.of("role", user.getRole().name()));
        } catch (FirebaseAuthException ex) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Không tạo được Firebase token");
        }
        List<ChatSessionResponse.Customer> customers = user.getRole() == UserRole.CUSTOMER
                ? List.of()
                : userRepository.findByRole(UserRole.CUSTOMER).stream()
                    .filter(User::isStatus)
                    .map(c -> new ChatSessionResponse.Customer(c.getUserId(), c.getFullName()))
                    .toList();
        return new ChatSessionResponse(firebaseToken, userId, user.getFullName(), user.getRole(),
                new ChatSessionResponse.FirebaseWebConfig(webApiKey, authDomain, databaseUrl, projectId, appId),
                customers);
    }

    private synchronized FirebaseAuth getFirebaseAuth() {
        if (firebaseAuth != null) {
            return firebaseAuth;
        }
        try (var keyFile = Files.newInputStream(Path.of(serviceAccountPath))) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(keyFile))
                    .setProjectId(projectId)
                    .setDatabaseUrl(databaseUrl)
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options, "laven-chat");
            firebaseAuth = FirebaseAuth.getInstance(app);
            return firebaseAuth;
        } catch (IOException | IllegalArgumentException ex) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Không đọc được Firebase service account; kiểm tra FIREBASE_SERVICE_ACCOUNT_PATH");
        }
    }

    private void requireConfig() {
        if (serviceAccountPath.isBlank() || projectId.isBlank() || webApiKey.isBlank()
                || authDomain.isBlank() || databaseUrl.isBlank() || appId.isBlank()) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Chưa cấu hình đủ FIREBASE_* trong .env");
        }
    }
}
