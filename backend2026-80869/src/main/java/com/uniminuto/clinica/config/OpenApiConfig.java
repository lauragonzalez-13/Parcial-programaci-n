package com.uniminuto.clinica.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI clinicaOpenApi() {
        return new OpenAPI()
                .info(new Info().title("API Clínica Veterinaria").version("1.0.0")
                        .description("Servicios de autenticación, pacientes, agenda, atención médica, inventario, dispensación, facturación y reportes. Usa Authorize con el token Bearer obtenido en /auth/login."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("opaque token")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
