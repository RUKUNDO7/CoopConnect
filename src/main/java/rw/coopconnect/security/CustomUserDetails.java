package rw.coopconnect.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import rw.coopconnect.domain.User;
import rw.coopconnect.domain.enums.Role;

import java.util.Collection;
import java.util.List;

/**
 * FR-01: Custom Spring Security UserDetails wrapping the User domain entity.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Long id;
    private final String email;
    private final String password;
    private final String fullName;
    private final String phone;
    private final Role role;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.enabled = Boolean.TRUE.equals(user.getEnabled());
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
