package com.jornal.security;

import com.jornal.entity.User;
import com.jornal.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carga usuarios desde la base de datos para el login con sesion y para el filtro JWT. */
@Service
public class JornalUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final Clock clock;

    public JornalUserDetailsService(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public JornalUserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(User.normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));
        return JornalUserDetails.from(user, LocalDateTime.now(clock));
    }

    /** Usado por el filtro JWT: el token trae el id, no el correo. */
    @Transactional(readOnly = true)
    public JornalUserDetails loadById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario inexistente"));
        return JornalUserDetails.from(user, LocalDateTime.now(clock));
    }
}
