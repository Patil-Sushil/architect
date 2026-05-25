package com.kalibyte.architect.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalibyte.architect.audit.entity.AuditLog;
import com.kalibyte.architect.audit.entity.enums.AuditStatus;
import com.kalibyte.architect.audit.repository.AuditLogRepository;
import com.kalibyte.architect.auth.security.token.CustomUserDetails;
import com.kalibyte.architect.common.annotation.LoggableAction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Intercepts all methods annotated with @LoggableAction
     */
    @Around("@annotation(com.kalibyte.architect.common.annotation.LoggableAction)")
    public Object auditAction(ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.currentTimeMillis();

        // Get annotation details
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LoggableAction annotation = method.getAnnotation(LoggableAction.class);

        // Get current user info
        String username = getCurrentUsername();
        UUID userId = getCurrentUserId();

        // Get request info
        String ipAddress = getClientIpAddress();
        String userAgent = getUserAgent();

        // Build base audit log
        AuditLog.AuditLogBuilder auditBuilder = AuditLog.builder()
                .userId(userId)
                .username(username)
                .action(annotation.action())
                .actionDescription(annotation.value())
                .entityType(annotation.entityType().isEmpty()
                        ? null : annotation.entityType())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now());

        Object result = null;
        try {
            // Execute the actual method
            result = joinPoint.proceed();

            // SUCCESS - extract entity info from result
            enrichWithEntityInfo(auditBuilder, result, annotation);
            auditBuilder.status(AuditStatus.SUCCESS);

            long duration = System.currentTimeMillis() - startTime;
            auditBuilder.durationMs(duration);

            // Save audit log asynchronously
            saveAuditLogAsync(auditBuilder.build());

            return result;

        } catch (Exception ex) {
            // FAILURE - log the error
            auditBuilder
                    .status(AuditStatus.FAILURE)
                    .errorMessage(truncate(ex.getMessage(), 500))
                    .durationMs(System.currentTimeMillis() - startTime);

            saveAuditLogAsync(auditBuilder.build());

            throw ex; // Re-throw so controller handles it
        }
    }

    /**
     * Save audit log asynchronously to not block the main thread
     */
    @Async
    protected void saveAuditLogAsync(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: user={}, action={}, status={}",
                    auditLog.getUsername(),
                    auditLog.getAction(),
                    auditLog.getStatus());
        } catch (Exception e) {
            // Never let audit logging break the application
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract entity ID and name from the method result
     */
    private void enrichWithEntityInfo(
            AuditLog.AuditLogBuilder builder,
            Object result,
            LoggableAction annotation) {

        if (result == null) return;

        try {
            // Handle ApiResponse wrapper
            Object actualResult = result;
            if (result.getClass().getSimpleName().equals("ApiResponse")) {
                try {
                    actualResult = result.getClass().getMethod("getData").invoke(result);
                } catch (Exception ignored) {}
            }

            if (actualResult == null) return;

            Class<?> resultClass = actualResult.getClass();

            // Try to get entity ID
            try {
                Object idValue = resultClass.getMethod("getId").invoke(actualResult);
                if (idValue instanceof UUID) {
                    builder.entityId((UUID) idValue);
                }
            } catch (NoSuchMethodException ignored) {}

            // Try to get entity name (project name)
            try {
                Object nameValue = resultClass
                        .getMethod("getProjectName").invoke(actualResult);
                if (nameValue instanceof String) {
                    builder.entityName((String) nameValue);
                }
            } catch (NoSuchMethodException ignored) {
                // Try generic getName()
                try {
                    Object nameValue = resultClass
                            .getMethod("getName").invoke(actualResult);
                    if (nameValue instanceof String) {
                        builder.entityName((String) nameValue);
                    }
                } catch (NoSuchMethodException ignored2) {}
            }

            // Serialize result to JSON for new_value
            try {
                String json = objectMapper.writeValueAsString(actualResult);
                builder.newValue(truncate(json, 2000));
            } catch (Exception ignored) {}

        } catch (Exception e) {
            log.debug("Could not enrich audit log with entity info: {}", e.getMessage());
        }
    }

    /**
     * Get current authenticated username
     */
    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception e) {
            log.debug("Could not get username from security context: {}", e.getMessage());
        }
        return "SYSTEM";
    }

    /**
     * Get current authenticated user ID
     */
    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                // Try different possible method names
                UUID userId = extractUserId(userDetails);

                if (userId != null) {
                    log.debug("Extracted userId: {} for username: {}", userId, userDetails.getUsername());
                    return userId;
                } else {
                    log.warn("Could not extract userId from CustomUserDetails for username: {}",
                            userDetails.getUsername());
                }
            }
        } catch (Exception e) {
            log.error("Error extracting userId from security context", e);
        }
        return null;
    }

    /**
     * Extract user ID from CustomUserDetails using reflection
     */
    private UUID extractUserId(CustomUserDetails userDetails) {
        try {
            // Try common method names
            String[] possibleMethods = {"getId", "getUserId", "getUuid", "getInternalId"};

            for (String methodName : possibleMethods) {
                try {
                    Method method = userDetails.getClass().getMethod(methodName);
                    Object value = method.invoke(userDetails);

                    if (value instanceof UUID) {
                        return (UUID) value;
                    } else if (value instanceof String) {
                        return UUID.fromString((String) value);
                    }
                } catch (NoSuchMethodException ignored) {
                    // Try next method
                }
            }

            log.warn("No suitable method found to extract UUID from CustomUserDetails. Available methods: {}",
                    (Object[]) userDetails.getClass().getMethods());

        } catch (Exception e) {
            log.error("Error extracting userId via reflection", e);
        }
        return null;
    }

    /**
     * Get client IP address (handles proxies)
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder
                            .currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();

            // Check for proxy headers first
            String[] headers = {
                    "X-Forwarded-For",
                    "X-Real-IP",
                    "Proxy-Client-IP",
                    "WL-Proxy-Client-IP"
            };

            for (String header : headers) {
                String ip = request.getHeader(header);
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    // X-Forwarded-For can contain multiple IPs
                    return ip.split(",")[0].trim();
                }
            }

            return request.getRemoteAddr();

        } catch (Exception ignored) {
            return "unknown";
        }
    }

    /**
     * Get user agent from request
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder
                            .currentRequestAttributes();
            String userAgent = attrs.getRequest().getHeader("User-Agent");
            return truncate(userAgent, 500);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Truncate string to max length
     */
    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength
                ? value.substring(0, maxLength) + "..."
                : value;
    }
}