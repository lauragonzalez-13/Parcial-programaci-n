package com.uniminuto.clinica.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class MigracionInventarioConfig {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    CommandLineRunner permitirFechasDeLoteNulas(JdbcTemplate jdbc) {
        return args -> {
            // En el esquema inicial estas fechas estaban en medicamento y eran NOT NULL.
            // Ahora cada lote contiene sus propias fechas, por lo que el producto puede tener varios lotes.
            jdbc.execute("ALTER TABLE medicamento MODIFY COLUMN fecha_compra DATE NULL, MODIFY COLUMN fecha_vence DATE NULL");
            jdbc.update("UPDATE medicamento SET codigo = LPAD(RIGHT(UPPER(HEX(id)), 8), 8, '0') WHERE codigo IS NULL OR codigo = ''");
            jdbc.update("UPDATE medicamento SET tipo = 'MEDICAMENTO' WHERE tipo IS NULL OR tipo = ''");
            jdbc.update("UPDATE medicamento SET stock_minimo = 0 WHERE stock_minimo IS NULL");
            jdbc.update("UPDATE medicamento SET activo = TRUE WHERE activo IS NULL");
            jdbc.update("UPDATE formula_medica SET cantidad_dispensada = 0 WHERE cantidad_dispensada IS NULL");
            jdbc.update("UPDATE formula_medica SET estado_dispensacion = 'PENDIENTE' WHERE estado_dispensacion IS NULL OR estado_dispensacion = ''");
        };
    }
}
