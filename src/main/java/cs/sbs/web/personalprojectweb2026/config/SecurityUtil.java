package cs.sbs.web.personalprojectweb2026.config;

import cs.sbs.web.personalprojectweb2026.model.entity.User;
import cs.sbs.web.personalprojectweb2026.repository.UserRepository;
import cs.sbs.web.personalprojectweb2026.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    /**
     * Get current user ID without hitting the database.
     * Extracted from UserPrincipal stored in SecurityContext during JWT auth.
     */
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserId();
        }
        throw new RuntimeException("用户未登录");
    }

    /**
     * Get full User entity (requires a DB query).
     * Prefer {@link #getCurrentUserId()} when only the ID is needed.
     */
    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
