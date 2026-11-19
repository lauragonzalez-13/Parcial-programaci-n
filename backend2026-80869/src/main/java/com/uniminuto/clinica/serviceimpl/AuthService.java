package com.uniminuto.clinica.serviceimpl;

import com.uniminuto.clinica.dto.*;
import com.uniminuto.clinica.entity.SesionUsuario;
import com.uniminuto.clinica.entity.Usuario;
import com.uniminuto.clinica.repository.SesionUsuarioRepository;
import com.uniminuto.clinica.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String[] ROLES = {"ADMINISTRADOR", "SECRETARIA", "VETERINARIO", "DESPACHO_MEDICO"};
    private final UsuarioRepository usuarios;
    private final SesionUsuarioRepository sesiones;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarios, SesionUsuarioRepository sesiones, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios; this.sesiones = sesiones; this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request == null || request.username() == null ? "" : request.username().trim();
        String password = request == null || request.password() == null ? "" : request.password();
        Usuario usuario = usuarios.findByUsernameIgnoreCase(username).orElse(null);
        if (usuario == null || !usuario.isActivo() || !passwordEncoder.matches(password, usuario.getPasswordHash())) {
            return new LoginResponse(username, null, false, null, null, null);
        }

        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        SesionUsuario sesion = new SesionUsuario();
        sesion.setUsuario(usuario); sesion.setTokenHash(hash(token)); sesion.setExpiraEn(LocalDateTime.now().plusHours(12));
        sesiones.save(sesion);
        return new LoginResponse(usuario.getUsername(), usuario.getId(), true, token, usuario.getRol(), usuario.getVeterinarioId());
    }

    @Transactional(readOnly = true)
    public ValidarTokenResponse validar(ValidarTokenRequest request) {
        String token = request == null ? null : request.token();
        UsuarioAutenticado usuario = autenticar(token).orElse(null);
        return usuario == null
                ? new ValidarTokenResponse(null, token, null, false, null, null)
                : new ValidarTokenResponse(usuario.usuarioId(), token, usuario.username(), true, usuario.rol(), usuario.veterinarioId());
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioAutenticado> autenticar(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return sesiones.findByTokenHashAndRevocadaFalse(hash(token.trim()))
                .filter(s -> s.getExpiraEn().isAfter(LocalDateTime.now()) && s.getUsuario().isActivo())
                .map(s -> new UsuarioAutenticado(s.getUsuario().getId(), s.getUsuario().getUsername(),
                        s.getUsuario().getRol(), s.getUsuario().getVeterinarioId()));
    }

    @Transactional
    public void cerrarSesion(String token) {
        if (token == null || token.isBlank()) return;
        sesiones.findByTokenHashAndRevocadaFalse(hash(token.trim())).ifPresent(s -> { s.setRevocada(true); sesiones.save(s); });
    }

    @Transactional
    public UsuarioResponse crearUsuario(CrearUsuarioRequest request) {
        if (request == null || request.username() == null || request.username().trim().length() < 3)
            throw new IllegalArgumentException("El username debe tener al menos 3 caracteres.");
        if (request.password() == null || request.password().length() < 8)
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        String username = request.username().trim();
        if (usuarios.existsByUsernameIgnoreCase(username)) throw new IllegalArgumentException("Ese username ya está registrado.");
        String rol = request.rol() == null ? "" : request.rol().trim().toUpperCase(Locale.ROOT);
        if (!java.util.Arrays.asList(ROLES).contains(rol)) throw new IllegalArgumentException("El rol no es válido.");
        if ("VETERINARIO".equals(rol) && request.veterinarioId() == null)
            throw new IllegalArgumentException("Para el rol VETERINARIO debes indicar veterinarioId.");
        Usuario usuario = new Usuario(); usuario.setUsername(username); usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(rol); usuario.setVeterinarioId(request.veterinarioId()); usuario.setActivo(true);
        usuario = usuarios.save(usuario);
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getRol(), usuario.getVeterinarioId(), usuario.isActivo());
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios() {
        return usuarios.findAll().stream().map(u -> new UsuarioResponse(u.getId(), u.getUsername(), u.getRol(),
                u.getVeterinarioId(), u.isActivo())).toList();
    }

    @Transactional
    public void eliminarUsuario(Long usuarioId, Long administradorId) {
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        if (usuario.getId().equals(administradorId)) throw new IllegalStateException("No puedes eliminar la cuenta con la que tienes la sesión activa.");
        if ("ADMINISTRADOR".equals(usuario.getRol()) && usuarios.countByRolAndActivoTrue("ADMINISTRADOR") <= 1)
            throw new IllegalStateException("No puedes eliminar el último usuario administrador activo.");
        sesiones.borrarPorUsuarioId(usuarioId);
        usuarios.delete(usuario);
    }

    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("No se pudo procesar el token.", e); }
    }

    public record UsuarioAutenticado(Long usuarioId, String username, String rol, Long veterinarioId) {}
}
