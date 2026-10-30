package com.jornal.security;

import com.jornal.entity.Role;
import com.jornal.entity.User;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Usuario autenticado. Solo guarda lo necesario para autorizar y mostrar el
 * encabezado; nunca el documento ni otros datos personales sensibles.
 * Se guarda en la sesion HTTP, por eso es inmutable y serializable.
 */
public final class JornalUserDetails implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final String fullName;
    private final String initials;
    private final boolean enabled;
    private final boolean accountNonLocked;

    private JornalUserDetails(User user, LocalDateTime now) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.fullName = user.getFullName();
        this.initials = user.getInitials();
        this.enabled = user.isActive();
        this.accountNonLocked = !user.isLocked(now);
    }

    public static JornalUserDetails from(User user, LocalDateTime now) {
        return new JornalUserDetails(user, now);
    }

    public UUID id() {
        return id;
    }

    public Role role() {
        return role;
    }

    public String fullName() {
        return fullName;
    }

    public String initials() {
        return initials;
    }

    public boolean isForeman() {
        return role == Role.FOREMAN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
