package cs.sbs.web.personalprojectweb2026.security;

import lombok.Getter;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

/**
 * Spring Security UserDetails that carries our domain User's ID,
 * so SecurityUtil can read it directly from SecurityContext without a DB query.
 */
@Getter
public class UserPrincipal extends User {

    private final Long userId;

    public UserPrincipal(cs.sbs.web.personalprojectweb2026.model.entity.User user) {
        super(user.getUsername(), user.getPassword(), Collections.emptyList());
        this.userId = user.getId();
    }
}
