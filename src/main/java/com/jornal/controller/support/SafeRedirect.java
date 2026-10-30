package com.jornal.controller.support;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.http.HttpHeaders;

/**
 * Vuelve a la pagina anterior despues de un error, sin abrir una redireccion
 * abierta: del encabezado Referer solo se usa la ruta, y solo si viene de este
 * mismo sitio. Cualquier otra cosa lleva al destino por defecto.
 */
public final class SafeRedirect {

    private SafeRedirect() {
    }

    public static String back(HttpServletRequest request, String fallback) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.isBlank()) {
            return fallback;
        }
        try {
            URI uri = new URI(referer);
            boolean sameHost = uri.getHost() == null || uri.getHost().equalsIgnoreCase(request.getServerName());
            String path = uri.getRawPath();
            if (!sameHost || path == null || !path.startsWith("/") || path.startsWith("//")) {
                return fallback;
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (URISyntaxException e) {
            return fallback;
        }
    }
}
