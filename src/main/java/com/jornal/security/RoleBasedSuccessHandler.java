package com.jornal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Despues del login lleva a cada rol a su panel, o a la pagina protegida que
 * intentaba abrir. El destino se calcula por peticion (sin modificar el estado
 * del handler, que es compartido entre hilos).
 */
public class RoleBasedSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        if (authentication.getPrincipal() instanceof JornalUserDetails user) {
            return homeOf(user);
        }
        return super.determineTargetUrl(request, response, authentication);
    }

    public static String homeOf(JornalUserDetails user) {
        return user.isForeman() ? "/maestro" : "/trabajador";
    }
}
