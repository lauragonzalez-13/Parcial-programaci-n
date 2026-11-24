package com.uniminuto.clinica;

import com.uniminuto.clinica.serviceimpl.AuthService;
import com.uniminuto.clinica.serviceimpl.AuthService.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.web.servlet.HandlerInterceptor;

/** Enforces the demo profile restrictions at the API boundary. */
public class PerfilAccesoInterceptor implements HandlerInterceptor {
    private final AuthService auth;
    public PerfilAccesoInterceptor(AuthService auth) { this.auth = auth; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String context=request.getContextPath();
        String path=request.getRequestURI().substring(Math.min(context.length(),request.getRequestURI().length()));
        if ("/error".equals(path)) return true;
        if (path.equals("/swagger-ui.html") || path.startsWith("/swagger-ui/") ||
                path.equals("/v3/api-docs") || path.startsWith("/v3/api-docs/")) return true;
        if ("/auth/login".equals(path) && "POST".equalsIgnoreCase(request.getMethod())) return true;
        if ("/auth/validate-token".equals(path) && "POST".equalsIgnoreCase(request.getMethod())) return true;

        String authorization = request.getHeader("Authorization");
        String token = authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorization.substring(7).trim() : null;
        UsuarioAutenticado usuario = auth.autenticar(token).orElse(null);
        if (usuario == null) return denegar(response, HttpServletResponse.SC_UNAUTHORIZED, "Debes iniciar sesión o tu sesión venció.");
        request.setAttribute("usuarioAutenticado", usuario);
        boolean despacho="DESPACHO_MEDICO".equals(usuario.rol());

        if (path.equals("/despachos") || path.startsWith("/despachos/")) {
            if (Set.of("DESPACHO_MEDICO", "ADMINISTRADOR", "VETERINARIO", "SECRETARIA").contains(usuario.rol())) return true;
            return denegar(response,"El módulo de despacho requiere el perfil DESPACHO_MEDICO.");
        }
        if (despacho) {
            boolean lecturaInventario="GET".equalsIgnoreCase(request.getMethod()) && (
                    path.equals("/inventario/medicamentos") || path.equals("/inventario/lotes") ||
                    path.equals("/inventario/movimientos") || path.equals("/inventario/alertas"));
            if (lecturaInventario) return true;
            return denegar(response,"El perfil DESPACHO_MEDICO solo puede consultar inventario y gestionar despachos.");
        }
        return true;
    }

    private boolean denegar(HttpServletResponse response,String mensaje)throws IOException {
        return denegar(response, HttpServletResponse.SC_FORBIDDEN, mensaje);
    }

    private boolean denegar(HttpServletResponse response,int status,String mensaje)throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"mensaje\":\""+mensaje+"\"}");
        return false;
    }
}
