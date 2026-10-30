package com.jornal.security;

import com.jornal.entity.User;
import com.jornal.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bloqueo temporal por intentos fallidos (OWASP A07). Escucha los eventos de
 * Spring Security, asi aplica igual al login web y al login de la API.
 * Los logs nunca incluyen el correo completo ni la contrasena intentada.
 */
@Component
public class LoginAttemptListener {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptListener.class);

    private final UserRepository userRepository;
    private final SecurityProperties properties;
    private final Clock clock;

    public LoginAttemptListener(UserRepository userRepository, SecurityProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String email = User.normalizeEmail(String.valueOf(event.getAuthentication().getPrincipal()));
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            boolean locked = user.registerFailedLogin(LocalDateTime.now(clock),
                    properties.maxLoginAttempts(), properties.lockDuration());
            if (locked) {
                log.warn("Cuenta {} bloqueada temporalmente por intentos fallidos", user.getId());
            } else {
                log.info("Intento de inicio de sesion fallido para la cuenta {}", user.getId());
            }
        }, () -> log.info("Intento de inicio de sesion con un correo no registrado ({})", mask(email)));
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof JornalUserDetails details) {
            userRepository.findById(details.id()).ifPresent(User::registerSuccessfulLogin);
            log.info("Inicio de sesion exitoso de la cuenta {}", details.id());
        }
    }

    /** h***@correo.com: suficiente para depurar sin exponer el dato personal. */
    static String mask(String email) {
        if (email == null || !email.contains("@") || email.indexOf('@') == 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(email.indexOf('@'));
    }
}
