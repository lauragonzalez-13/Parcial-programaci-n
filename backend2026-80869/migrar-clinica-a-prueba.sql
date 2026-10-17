-- Copia los datos existentes de clinica a prueba sin borrar el origen.
-- Ejecutar en MySQL Workbench con un usuario que tenga SELECT e INSERT
-- sobre ambos esquemas. Inserta primero las tablas padre para respetar FKs.

START TRANSACTION;

INSERT IGNORE INTO prueba.cliente
  (id, nombre, apellido, documento, telefono, correo, activo)
SELECT id, nombre, apellido, documento, telefono, correo, activo
FROM clinica.cliente;

INSERT IGNORE INTO prueba.veterinario
  (id, nombre, apellido, documento, tarjeta_profesional, correo, activo)
SELECT id, nombre, apellido, documento, tarjeta_profesional, correo, activo
FROM clinica.veterinario;

INSERT IGNORE INTO prueba.mascota
  (id, nombre, especie, raza, sexo, edad, activa, cliente_id)
SELECT id, nombre, especie, raza, sexo, edad, activa, cliente_id
FROM clinica.mascota;

INSERT IGNORE INTO prueba.consulta
  (id, fecha_hora, motivo, diagnostico, activa, mascota_id, veterinario_id, estado)
SELECT id, fecha_hora, motivo, diagnostico, activa, mascota_id, veterinario_id, estado
FROM clinica.consulta;

INSERT IGNORE INTO prueba.formula_medica
  (id, fecha_emision, medicamento, dosis, frecuencia, duracion_dias,
   indicaciones, activa, mascota_id, cliente_id, veterinario_id, consulta_id)
SELECT id, fecha_emision, medicamento, dosis, frecuencia, duracion_dias,
       indicaciones, activa, mascota_id, cliente_id, veterinario_id, consulta_id
FROM clinica.formula_medica;

COMMIT;

-- Verificación: estos conteos deben ser mayores que cero si clinica tenía datos.
SELECT 'clientes' AS entidad, COUNT(*) AS total FROM prueba.cliente
UNION ALL SELECT 'veterinarios', COUNT(*) FROM prueba.veterinario
UNION ALL SELECT 'mascotas', COUNT(*) FROM prueba.mascota
UNION ALL SELECT 'citas atendidas', COUNT(*) FROM prueba.consulta WHERE estado = 'ATENDIDA';
