package com.jornal.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

/**
 * Distingue cuenta bloqueada o desactivada de credenciales incorrectas.
 * Para credenciales incorrectas el mensaje es el mismo exista o no el correo,
 * asi el formulario no sirve para averiguar quien tiene cuenta.
 */
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String target = "/login?error";
        if (exception instanceof LockedException) {
            target = "/login?bloqueada";
        } else if (exception instanceof DisabledException) {
            target = "/login?inactiva";
        }
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
