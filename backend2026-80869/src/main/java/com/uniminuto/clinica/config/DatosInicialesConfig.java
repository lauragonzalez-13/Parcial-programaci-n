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
            // No mezclar datos de ejemplo con información ya existente.
            if (clientes.count() > 0 || mascotas.count() > 0
                    || veterinarios.count() > 0 || consultas.count() > 0) {
                return;
            }

            Cliente clienteUno = new Cliente();
            clienteUno.setNombre("Ana");
            clienteUno.setApellido("Gómez");
            clienteUno.setDocumento("PRUEBA-CLI-001");
            clienteUno.setTelefono("3000000001");
            clienteUno.setCorreo("ana.gomez@example.test");
            clienteUno.setActivo(true);
            clientes.save(clienteUno);

            Cliente clienteDos = new Cliente();
            clienteDos.setNombre("Carlos");
            clienteDos.setApellido("Ruiz");
            clienteDos.setDocumento("PRUEBA-CLI-002");
            clienteDos.setTelefono("3000000002");
            clienteDos.setCorreo("carlos.ruiz@example.test");
            clienteDos.setActivo(true);
            clientes.save(clienteDos);

            Veterinario veterinario = new Veterinario();
            veterinario.setNombre("Camila");
            veterinario.setApellido("Pérez");
            veterinario.setDocumento("PRUEBA-VET-001");
            veterinario.setTarjetaProfesional("TP-PRUEBA-001");
            veterinario.setCorreo("camila.perez@example.test");
            veterinario.setActivo(true);
            veterinarios.save(veterinario);

            Mascota luna = crearMascota("Luna", "Canino", "Mestiza", clienteUno);
            Mascota milo = crearMascota("Milo", "Felino", "Criollo", clienteDos);
            mascotas.save(luna);
            mascotas.save(milo);

            LocalDateTime ahora = LocalDateTime.now().withSecond(0).withNano(0);
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

    private Mascota crearMascota(String nombre, String especie, String raza, Cliente cliente) {
        Mascota mascota = new Mascota();
        mascota.setNombre(nombre);
        mascota.setEspecie(especie);
        mascota.setRaza(raza);
        mascota.setEdad(2);
        mascota.setActiva(true);
        mascota.setCliente(cliente);
        return mascota;
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
