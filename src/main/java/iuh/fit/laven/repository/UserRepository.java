package iuh.fit.laven.repository;

import iuh.fit.laven.model.User;
import iuh.fit.laven.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    List<User> findByRole(UserRole role);

    /** Tìm theo vai trò; keyword (có thể null) khớp một phần họ tên, email hoặc số điện thoại. */
    @Query("""
            select u from User u
            where u.role = :role
              and (:keyword is null
                   or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(u.email) like lower(concat('%', :keyword, '%'))
                   or u.phone like concat('%', :keyword, '%'))
            order by u.userId desc
            """)
    List<User> search(@Param("role") UserRole role, @Param("keyword") String keyword);

    Optional<User> findByUserId(String userId);

    long countByRole(UserRole role);

    long countByRoleAndUserIdStartingWith(UserRole role, String userIdPrefix);
}