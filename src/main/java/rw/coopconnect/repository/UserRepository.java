package rw.coopconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.User;
import rw.coopconnect.domain.enums.Role;

import java.util.List;
import java.util.Optional;

/**
 * FR-01: User persistence with lookup by email, phone, and role.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<User> findByRole(Role role);

    Optional<User> findByPasswordResetToken(String token);

    List<User> findByEnabledTrue();
}
