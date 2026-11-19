package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.dto.*;
import com.uniminuto.clinica.serviceimpl.AuthService;
import com.uniminuto.clinica.serviceimpl.AuthService.UsuarioAutenticado;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = auth.login(request);
        return response.login() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/validate-token")
    public ResponseEntity<ValidarTokenResponse> validarToken(@RequestBody ValidarTokenRequest request) {
        return ResponseEntity.ok(auth.validar(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorization.substring(7).trim() : null;
        auth.cerrarSesion(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(@RequestBody CrearUsuarioRequest request,
            @RequestAttribute("usuarioAutenticado") UsuarioAutenticado actor) {
        if (!"ADMINISTRADOR".equals(actor.rol()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", "Solo un administrador puede crear usuarios."));
        try { return ResponseEntity.status(HttpStatus.CREATED).body(auth.crearUsuario(request)); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage())); }
    }

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios(@RequestAttribute("usuarioAutenticado") UsuarioAutenticado actor) {
        if (!"ADMINISTRADOR".equals(actor.rol()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", "Solo un administrador puede consultar usuarios."));
        return ResponseEntity.ok(auth.listarUsuarios());
    }

    @DeleteMapping("/usuarios/{usuarioId}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long usuarioId,
            @RequestAttribute("usuarioAutenticado") UsuarioAutenticado actor) {
        if (!"ADMINISTRADOR".equals(actor.rol()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", "Solo un administrador puede eliminar usuarios."));
        try { auth.eliminarUsuario(usuarioId, actor.usuarioId()); return ResponseEntity.noContent().build(); }
        catch (IllegalArgumentException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage())); }
        catch (IllegalStateException e) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage())); }
    }
}
