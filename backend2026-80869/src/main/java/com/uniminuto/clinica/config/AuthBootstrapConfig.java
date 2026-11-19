package com.uniminuto.clinica.config;

import com.uniminuto.clinica.entity.Usuario;
import com.uniminuto.clinica.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class AuthBootstrapConfig {
    @Bean
    CommandLineRunner crearAdministradorInicial(UsuarioRepository usuarios, PasswordEncoder encoder,
            @Value("${clinica.auth.bootstrap.username:}") String username,
            @Value("${clinica.auth.bootstrap.password:}") String password) {
        return args -> {
            if (username == null || username.isBlank() || password == null || password.isBlank()
                    || usuarios.existsByUsernameIgnoreCase(username.trim())) return;
            Usuario admin = new Usuario(); admin.setUsername(username.trim()); admin.setPasswordHash(encoder.encode(password));
            admin.setRol("ADMINISTRADOR"); admin.setActivo(true); usuarios.save(admin);
        };
    }
}
