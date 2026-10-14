package rw.coopconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rw.coopconnect.domain.AuditLog;
import rw.coopconnect.domain.User;
import rw.coopconnect.repository.AuditLogRepository;
import rw.coopconnect.repository.UserRepository;
import rw.coopconnect.security.SecurityUtils;

import java.time.LocalDateTime;

/**
 * FR-13: Audit log service for capturing sensitive actions (who, what, when, IP).
 * NFR-01: Security tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, Long entityId, String details) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
            User user = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
            String ip = SecurityUtils.getClientIp();

            AuditLog logEntry = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ip)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(logEntry);
            log.info("AUDIT [{}] by user {} on {}:{} - details: {}", action,
                    user != null ? user.getEmail() : "ANONYMOUS", entityType, entityId, details);
        } catch (Exception ex) {
            log.error("Failed to record audit log: {}", ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(User user, String action, String entityType, Long entityId, String details) {
        try {
            String ip = SecurityUtils.getClientIp();
            AuditLog logEntry = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ip)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(logEntry);
            log.info("AUDIT [{}] by user {} on {}:{} - details: {}", action,
                    user != null ? user.getEmail() : "SYSTEM", entityType, entityId, details);
        } catch (Exception ex) {
            log.error("Failed to record audit log: {}", ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }
}
