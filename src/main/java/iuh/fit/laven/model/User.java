package iuh.fit.laven.model;

import iuh.fit.laven.model.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(length = 19, nullable = false)
    private String userId;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(length = 10, nullable = false)
    private String phone;

    @Column(length = 60, nullable = false)
    private String passwordHash;

    @Column(length = 50, nullable = false)
    private String fullName;

    @Column(length = 255)
    private String addressLine;

    @Column(length = 100)
    private String ward;

    @Column(length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UserRole role;

    @Builder.Default
    @Column(nullable = false)
    private boolean status = true;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    private LocalDate hireDate;

    private LocalDate lockedDate;

    @Column(length = 255)
    private String lockedReason;

    @Builder.Default
    @Column(nullable = false)
    private boolean pendingLock = false;

    private LocalDateTime passwordChangedAt;

    @Builder.Default
    @Column(nullable = false)
    private Integer failedLoginAttempts = 0;

    private LocalDateTime loginLockedUntil;
}