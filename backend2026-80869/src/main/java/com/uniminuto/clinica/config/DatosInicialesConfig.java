package com.uniminuto.clinica.config;

import com.uniminuto.clinica.entity.AnotacionClinica;
import com.uniminuto.clinica.entity.Cliente;
import com.uniminuto.clinica.entity.Consulta;
import com.uniminuto.clinica.entity.EstadoConsulta;
import com.uniminuto.clinica.entity.HistoriaClinica;
import com.uniminuto.clinica.entity.Mascota;
import com.uniminuto.clinica.entity.Veterinario;
import com.uniminuto.clinica.repository.AnotacionClinicaRepository;
import com.uniminuto.clinica.repository.ClienteRepository;
import com.uniminuto.clinica.repository.ConsultaRepository;
import com.uniminuto.clinica.repository.HistoriaClinicaRepository;
import com.uniminuto.clinica.repository.MascotaRepository;
import com.uniminuto.clinica.repository.VeterinarioRepository;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DatosInicialesConfig {

    @Bean
    CommandLineRunner cargarDatosIniciales(ClienteRepository clientes,
            MascotaRepository mascotas,
            VeterinarioRepository veterinarios,
            ConsultaRepository consultas,
            HistoriaClinicaRepository historias,
            AnotacionClinicaRepository anotaciones) {
        return args -> {
            Cliente clienteUno = buscarOcrearCliente(clientes, "Ana", "Gómez",
                    "PRUEBA-CLI-001", "3000000001", "ana.gomez@example.test");
            Cliente clienteDos = buscarOcrearCliente(clientes, "Carlos", "Ruiz",
                    "PRUEBA-CLI-002", "3000000002", "carlos.ruiz@example.test");
            Cliente clienteTres = buscarOcrearCliente(clientes, "Laura", "Martínez",
                    "PRUEBA-CLI-003", "3000000003", "laura.martinez@example.test");
            Cliente clienteCuatro = buscarOcrearCliente(clientes, "Diego", "Ramírez",
                    "PRUEBA-CLI-004", "3000000004", "diego.ramirez@example.test");
            Cliente clienteCinco = buscarOcrearCliente(clientes, "Mariana", "Torres",
                    "PRUEBA-CLI-005", "3000000005", "mariana.torres@example.test");
            Cliente clienteSeis = buscarOcrearCliente(clientes, "Andrés", "Vargas",
                    "PRUEBA-CLI-006", "3000000006", "andres.vargas@example.test");

            Veterinario veterinario = veterinarios.findAll().stream()
                    .filter(v -> "PRUEBA-VET-001".equals(v.getDocumento()))
                    .findFirst()
                    .orElseGet(() -> {
                        Veterinario nuevo = new Veterinario();
                        nuevo.setNombre("Camila");
                        nuevo.setApellido("Pérez");
                        nuevo.setDocumento("PRUEBA-VET-001");
                        nuevo.setTarjetaProfesional("TP-PRUEBA-001");
                        nuevo.setCorreo("camila.perez@example.test");
                        nuevo.setActivo(true);
                        return veterinarios.save(nuevo);
                    });

            Mascota luna = guardarOActualizarMascota(mascotas, "Luna", "Canino",
                    "Mestiza", "Hembra", 3, clienteUno);
            Mascota milo = guardarOActualizarMascota(mascotas, "Milo", "Felino",
                    "Criollo", "Macho", 2, clienteDos);
            guardarOActualizarMascota(mascotas, "Rocky", "Canino",
                    "Pitbull", "Macho", 5, clienteUno);
            guardarOActualizarMascota(mascotas, "Nala", "Felino",
                    "Siamés", "Hembra", 4, clienteDos);
            guardarOActualizarMascota(mascotas, "Toby", "Canino",
                    "Beagle", "Macho", 2, clienteUno);
            guardarOActualizarMascota(mascotas, "Simba", "Felino",
                    "Persa", "Macho", 6, clienteDos);
            guardarOActualizarMascota(mascotas, "Coco", "Canino",
                    "Poodle", "Hembra", 1, clienteUno);
            guardarOActualizarMascota(mascotas, "Luna Bella", "Felino",
                    "Angora", "Hembra", 2, clienteDos);
            guardarOActualizarMascota(mascotas, "Max", "Canino",
                    "Golden Retriever", "Macho", 7, clienteUno);
            guardarOActualizarMascota(mascotas, "Olivia", "Felino",
                    "Criolla", "Hembra", 3, clienteDos);
            guardarOActualizarMascota(mascotas, "Bruno", "Canino",
                    "Husky Siberiano", "Macho", 8, clienteTres);
            guardarOActualizarMascota(mascotas, "Mika", "Felino",
                    "Ragdoll", "Hembra", 10, clienteCuatro);
            guardarOActualizarMascota(mascotas, "Daisy", "Canino",
                    "Chihuahua", "Hembra", 12, clienteCinco);
            guardarOActualizarMascota(mascotas, "Zeus", "Canino",
                    "Pastor Alemán", "Macho", 9, clienteSeis);
            guardarOActualizarMascota(mascotas, "Kiara", "Felino",
                    "Maine Coon", "Hembra", 5, clienteTres);

            LocalDateTime ahora = LocalDateTime.now().withSecond(0).withNano(0);
            if (consultas.count() > 0) {
                return;
            }
            Consulta consultaConHistoria = crearConsulta(
                    ahora.minusDays(3), "Control general", luna, veterinario,
                    EstadoConsulta.ATENDIDA);
            consultaConHistoria.setDiagnostico("Paciente en buen estado general.");
            consultas.save(consultaConHistoria);

            // Cita atendida sin anotación, disponible para probar una anotación nueva.
            consultas.save(crearConsulta(
                    ahora.minusDays(2), "Revisión de seguimiento", luna, veterinario,
                    EstadoConsulta.ATENDIDA));

            // Cita atendida de otra mascota, disponible para crear otra historia.
            consultas.save(crearConsulta(
                    ahora.minusDays(1), "Valoración inicial", milo, veterinario,
                    EstadoConsulta.ATENDIDA));

            // Cita futura para probar el flujo de programación.
            consultas.save(crearConsulta(
                    ahora.plusDays(3), "Vacunación", milo, veterinario,
                    EstadoConsulta.PROGRAMADA));

            HistoriaClinica historia = new HistoriaClinica();
            historia.setMascota(luna);
            historia.setFechaCreacion(consultaConHistoria.getFechaHora());
            historia = historias.save(historia);

            AnotacionClinica anotacion = new AnotacionClinica();
            anotacion.setHistoriaClinica(historia);
            anotacion.setConsulta(consultaConHistoria);
            anotacion.setVeterinario(veterinario);
            anotacion.setFechaHora(consultaConHistoria.getFechaHora());
            anotacion.setDescripcion(consultaConHistoria.getMotivo());
            anotacion.setObservacion("Se recomienda mantener los controles preventivos.");
            anotacion.setDiagnostico(consultaConHistoria.getDiagnostico());
            anotaciones.save(anotacion);
        };
    }

    private Cliente buscarOcrearCliente(ClienteRepository clientes, String nombre,
            String apellido, String documento, String telefono, String correo) {
        return clientes.findAll().stream()
                .filter(c -> documento.equals(c.getDocumento()))
                .findFirst()
                .orElseGet(() -> {
                    Cliente cliente = new Cliente();
                    cliente.setNombre(nombre);
                    cliente.setApellido(apellido);
                    cliente.setDocumento(documento);
                    cliente.setTelefono(telefono);
                    cliente.setCorreo(correo);
                    cliente.setActivo(true);
                    return clientes.save(cliente);
                });
    }

    private Mascota guardarOActualizarMascota(MascotaRepository mascotas, String nombre,
            String especie, String raza, String sexo, int edad, Cliente cliente) {
        Mascota mascota = mascotas.findAll().stream()
                .filter(m -> nombre.equalsIgnoreCase(m.getNombre()))
                .findFirst()
                .orElseGet(Mascota::new);
        mascota.setNombre(nombre);
        mascota.setEspecie(especie);
        mascota.setRaza(raza);
        mascota.setSexo(sexo);
        mascota.setEdad(edad);
        mascota.setActiva(true);
        mascota.setCliente(cliente);
        return mascotas.save(mascota);
    }

    private Consulta crearConsulta(LocalDateTime fechaHora, String motivo, Mascota mascota,
            Veterinario veterinario, EstadoConsulta estado) {
        Consulta consulta = new Consulta();
        consulta.setFechaHora(fechaHora);
        consulta.setMotivo(motivo);
        consulta.setMascota(mascota);
        consulta.setVeterinario(veterinario);
        consulta.setEstado(estado);
        consulta.setActiva(true);
        return consulta;
    }
}
