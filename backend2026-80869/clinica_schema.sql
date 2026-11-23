-- Esquema inicial de Clínica Veterinaria. Base de datos requerida: prueba.
-- MySQL 8.4 / InnoDB. Se puede ejecutar más de una vez sin borrar datos.
CREATE DATABASE IF NOT EXISTS prueba CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE prueba;

CREATE TABLE IF NOT EXISTS cliente (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, nombre VARCHAR(100) NOT NULL, apellido VARCHAR(100) NOT NULL,
 documento VARCHAR(20) NOT NULL UNIQUE, telefono VARCHAR(20), correo VARCHAR(150), activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS especialidad_medica (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, nombre VARCHAR(100) NOT NULL UNIQUE, descripcion VARCHAR(500), activa BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS veterinario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, nombre VARCHAR(100) NOT NULL, apellido VARCHAR(100) NOT NULL,
 documento VARCHAR(20) NOT NULL UNIQUE, tarjeta_profesional VARCHAR(30), correo VARCHAR(100), activo BOOLEAN NOT NULL DEFAULT TRUE,
 especialidad_id BIGINT, CONSTRAINT fk_veterinario_especialidad FOREIGN KEY (especialidad_id) REFERENCES especialidad_medica(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS mascota (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, nombre VARCHAR(100) NOT NULL, especie VARCHAR(50) NOT NULL, raza VARCHAR(50),
 sexo VARCHAR(20), edad INT, activa BOOLEAN NOT NULL DEFAULT TRUE, cliente_id BIGINT NOT NULL,
 CONSTRAINT fk_mascota_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS consulta (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, fecha_hora DATETIME NOT NULL, duracion_minutos INT DEFAULT 30,
 motivo VARCHAR(1000) NOT NULL, diagnostico VARCHAR(2000), activa BOOLEAN NOT NULL DEFAULT TRUE,
 estado VARCHAR(20) NOT NULL, mascota_id BIGINT NOT NULL, veterinario_id BIGINT NOT NULL,
 CONSTRAINT fk_consulta_mascota FOREIGN KEY (mascota_id) REFERENCES mascota(id),
 CONSTRAINT fk_consulta_veterinario FOREIGN KEY (veterinario_id) REFERENCES veterinario(id),
 INDEX idx_consulta_fecha (fecha_hora), INDEX idx_consulta_vet_fecha (veterinario_id, fecha_hora)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS disponibilidad_veterinario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, veterinario_id BIGINT NOT NULL, dia_semana INT NOT NULL,
 hora_inicio TIME NOT NULL, hora_fin TIME NOT NULL, activa BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT fk_disponibilidad_vet FOREIGN KEY (veterinario_id) REFERENCES veterinario(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS historia_clinica (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, fecha_creacion DATETIME NOT NULL, mascota_id BIGINT NOT NULL UNIQUE,
 CONSTRAINT fk_historia_mascota FOREIGN KEY (mascota_id) REFERENCES mascota(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS anotacion_clinica (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, fecha_hora DATETIME NOT NULL, descripcion VARCHAR(1000) NOT NULL,
 observacion VARCHAR(2000), diagnostico VARCHAR(2000), sintomas VARCHAR(2000), recomendaciones VARCHAR(2000),
 historia_clinica_id BIGINT NOT NULL, consulta_id BIGINT NOT NULL UNIQUE, veterinario_id BIGINT NOT NULL,
 CONSTRAINT fk_anotacion_historia FOREIGN KEY (historia_clinica_id) REFERENCES historia_clinica(id),
 CONSTRAINT fk_anotacion_consulta FOREIGN KEY (consulta_id) REFERENCES consulta(id),
 CONSTRAINT fk_anotacion_vet FOREIGN KEY (veterinario_id) REFERENCES veterinario(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS tarifa_servicio (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, codigo VARCHAR(40) NOT NULL UNIQUE, nombre VARCHAR(140) NOT NULL,
 descripcion VARCHAR(500), tipo_servicio VARCHAR(30) NOT NULL DEFAULT 'CONSULTA', especialidad_id BIGINT,
 valor DECIMAL(14,2) NOT NULL DEFAULT 0, iva_porcentaje DECIMAL(5,2) NOT NULL DEFAULT 0,
 activa BOOLEAN NOT NULL DEFAULT TRUE, fecha_actualizacion DATETIME NOT NULL,
 CONSTRAINT fk_tarifa_especialidad FOREIGN KEY (especialidad_id) REFERENCES especialidad_medica(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS log_tarifa (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tarifa_id BIGINT NOT NULL, accion VARCHAR(20) NOT NULL,
 valor_anterior DECIMAL(14,2), valor_nuevo DECIMAL(14,2) NOT NULL, iva_anterior DECIMAL(5,2), iva_nuevo DECIMAL(5,2) NOT NULL,
 usuario VARCHAR(120) NOT NULL, fecha DATETIME NOT NULL,
 CONSTRAINT fk_log_tarifa FOREIGN KEY (tarifa_id) REFERENCES tarifa_servicio(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS medicamento (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, codigo VARCHAR(8) UNIQUE, nombre VARCHAR(100) NOT NULL UNIQUE,
 descripcion TEXT, presentacion VARCHAR(100), fecha_compra DATE, fecha_vence DATE, tipo VARCHAR(20), registro_invima VARCHAR(80),
 stock_minimo DECIMAL(12,3), iva_porcentaje DECIMAL(5,2) NOT NULL DEFAULT 0, fecha_proximo_pedido DATE,
 activo BOOLEAN NOT NULL DEFAULT TRUE, fecha_creacion_registro DATETIME NOT NULL, fecha_modificacion_registro DATETIME
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS lote_inventario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, medicamento_id BIGINT NOT NULL, codigo_lote VARCHAR(80) NOT NULL,
 cantidad_disponible DECIMAL(12,3) NOT NULL, precio_compra DECIMAL(14,2) NOT NULL, precio_venta DECIMAL(14,2) NOT NULL,
 fecha_ingreso DATE NOT NULL, fecha_vencimiento DATE, UNIQUE KEY uk_lote_producto_codigo(medicamento_id,codigo_lote),
 CONSTRAINT fk_lote_medicamento FOREIGN KEY (medicamento_id) REFERENCES medicamento(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS importacion_inventario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, nombre_archivo VARCHAR(255) NOT NULL, usuario_responsable VARCHAR(120) NOT NULL,
 fecha DATETIME NOT NULL, registros_leidos INT NOT NULL DEFAULT 0, registros_importados INT NOT NULL DEFAULT 0,
 registros_rechazados INT NOT NULL DEFAULT 0, detalle_errores TEXT
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS formula_medica (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, fecha_emision DATE NOT NULL, medicamento VARCHAR(150) NOT NULL, dosis VARCHAR(100) NOT NULL,
 frecuencia VARCHAR(100) NOT NULL, duracion_dias INT NOT NULL, indicaciones VARCHAR(1000) NOT NULL, via_administracion VARCHAR(80),
 activa BOOLEAN NOT NULL DEFAULT TRUE, medicamento_inventario_id BIGINT, cantidad_requerida DECIMAL(12,3), unidades_por_toma DECIMAL(12,3),
 tomas_por_dia INT, cantidad_dispensada DECIMAL(12,3), estado_dispensacion VARCHAR(20), mascota_id BIGINT NOT NULL,
 cliente_id BIGINT NOT NULL, veterinario_id BIGINT NOT NULL, consulta_id BIGINT NOT NULL,
 CONSTRAINT fk_formula_medicamento FOREIGN KEY (medicamento_inventario_id) REFERENCES medicamento(id),
 CONSTRAINT fk_formula_mascota FOREIGN KEY (mascota_id) REFERENCES mascota(id), CONSTRAINT fk_formula_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
 CONSTRAINT fk_formula_vet FOREIGN KEY (veterinario_id) REFERENCES veterinario(id), CONSTRAINT fk_formula_consulta FOREIGN KEY (consulta_id) REFERENCES consulta(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS despacho_medicamento (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, formula_medica_id BIGINT NOT NULL UNIQUE, estado VARCHAR(40) NOT NULL,
 creado_en DATETIME NOT NULL, actualizado_en DATETIME, entregado_en DATETIME, usuario_responsable VARCHAR(120), observacion VARCHAR(500),
 CONSTRAINT fk_despacho_formula FOREIGN KEY (formula_medica_id) REFERENCES formula_medica(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS movimiento_inventario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tipo VARCHAR(20) NOT NULL, medicamento_id BIGINT NOT NULL, lote_id BIGINT NOT NULL,
 formula_medica_id BIGINT, despacho_id BIGINT, cantidad DECIMAL(12,3) NOT NULL, fecha DATETIME NOT NULL,
 usuario_responsable VARCHAR(120) NOT NULL, observacion VARCHAR(300),
 CONSTRAINT fk_mov_medicamento FOREIGN KEY (medicamento_id) REFERENCES medicamento(id), CONSTRAINT fk_mov_lote FOREIGN KEY (lote_id) REFERENCES lote_inventario(id),
 CONSTRAINT fk_mov_formula FOREIGN KEY (formula_medica_id) REFERENCES formula_medica(id), CONSTRAINT fk_mov_despacho FOREIGN KEY (despacho_id) REFERENCES despacho_medicamento(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS detalle_despacho (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, despacho_id BIGINT NOT NULL, lote_id BIGINT NOT NULL, movimiento_id BIGINT NOT NULL UNIQUE,
 cantidad DECIMAL(12,3) NOT NULL, fecha DATETIME NOT NULL,
 CONSTRAINT fk_det_despacho FOREIGN KEY (despacho_id) REFERENCES despacho_medicamento(id),
 CONSTRAINT fk_det_lote FOREIGN KEY (lote_id) REFERENCES lote_inventario(id), CONSTRAINT fk_det_mov FOREIGN KEY (movimiento_id) REFERENCES movimiento_inventario(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS evento_despacho (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, despacho_id BIGINT NOT NULL, estado VARCHAR(40) NOT NULL,
 usuario_responsable VARCHAR(120) NOT NULL, fecha DATETIME NOT NULL, observacion VARCHAR(500),
 CONSTRAINT fk_evento_despacho FOREIGN KEY (despacho_id) REFERENCES despacho_medicamento(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS factura (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, numero VARCHAR(30) UNIQUE, consulta_id BIGINT NOT NULL UNIQUE, cliente_id BIGINT NOT NULL,
 mascota_id BIGINT NOT NULL, veterinario_id BIGINT NOT NULL, fecha_generacion DATETIME NOT NULL, estado VARCHAR(24) NOT NULL,
 subtotal DECIMAL(14,2) NOT NULL, descuento DECIMAL(14,2) NOT NULL, impuestos DECIMAL(14,2) NOT NULL, total DECIMAL(14,2) NOT NULL,
 pagado DECIMAL(14,2) NOT NULL, descuento_autorizado_por VARCHAR(160), motivo_anulacion VARCHAR(500), anulada_por VARCHAR(120), fecha_anulacion DATETIME,
 CONSTRAINT fk_factura_consulta FOREIGN KEY (consulta_id) REFERENCES consulta(id), CONSTRAINT fk_factura_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
 CONSTRAINT fk_factura_mascota FOREIGN KEY (mascota_id) REFERENCES mascota(id), CONSTRAINT fk_factura_vet FOREIGN KEY (veterinario_id) REFERENCES veterinario(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS linea_factura (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, factura_id BIGINT NOT NULL, tipo VARCHAR(20) NOT NULL, descripcion VARCHAR(240) NOT NULL,
 cantidad DECIMAL(12,3) NOT NULL, valor_unitario DECIMAL(14,2) NOT NULL, iva_porcentaje DECIMAL(5,2) NOT NULL,
 subtotal DECIMAL(14,2) NOT NULL, impuesto DECIMAL(14,2) NOT NULL, total DECIMAL(14,2) NOT NULL, codigo_lote VARCHAR(100),
 CONSTRAINT fk_linea_factura FOREIGN KEY (factura_id) REFERENCES factura(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS pago_factura (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, factura_id BIGINT NOT NULL, valor DECIMAL(14,2) NOT NULL, medio VARCHAR(30) NOT NULL,
 referencia VARCHAR(100), registrado_por VARCHAR(120) NOT NULL, fecha DATETIME NOT NULL,
 CONSTRAINT fk_pago_factura FOREIGN KEY (factura_id) REFERENCES factura(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS evento_factura (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, factura_id BIGINT NOT NULL, accion VARCHAR(40) NOT NULL, detalle VARCHAR(500),
 usuario VARCHAR(120) NOT NULL, fecha DATETIME NOT NULL, CONSTRAINT fk_evento_factura FOREIGN KEY (factura_id) REFERENCES factura(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS datos_clinica (
 id BIGINT PRIMARY KEY, nombre VARCHAR(140) NOT NULL, nit VARCHAR(30), telefono VARCHAR(30), direccion VARCHAR(200), correo VARCHAR(150)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS usuario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(80) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL,
 rol VARCHAR(30) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE, veterinario_id BIGINT
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS sesion_usuario (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, usuario_id BIGINT NOT NULL, token_hash VARCHAR(64) NOT NULL UNIQUE,
 expira_en DATETIME NOT NULL, revocada BOOLEAN NOT NULL DEFAULT FALSE,
 CONSTRAINT fk_sesion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB;

-- Las restricciones de solapamiento horario, propietario-mascota y estados se validan en servicios backend.
-- Hibernate ddl-auto=update mantiene el esquema alineado durante este proyecto académico.
