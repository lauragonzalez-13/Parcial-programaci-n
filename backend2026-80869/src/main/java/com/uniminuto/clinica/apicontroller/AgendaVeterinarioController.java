package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.entity.DisponibilidadVeterinario;
import com.uniminuto.clinica.entity.EspecialidadMedica;
import com.uniminuto.clinica.repository.DisponibilidadVeterinarioRepository;
import com.uniminuto.clinica.repository.EspecialidadMedicaRepository;
import com.uniminuto.clinica.repository.VeterinarioRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/veterinarios")
public class AgendaVeterinarioController {
    private final DisponibilidadVeterinarioRepository horarios;
    private final EspecialidadMedicaRepository especialidades;
    private final VeterinarioRepository veterinarios;
    public AgendaVeterinarioController(DisponibilidadVeterinarioRepository h, EspecialidadMedicaRepository e, VeterinarioRepository v) { horarios=h; especialidades=e; veterinarios=v; }

    @GetMapping("/especialidades") public List<EspecialidadMedica> especialidades() { return especialidades.findAll(); }
    @GetMapping("/{id}/disponibilidad") public List<DisponibilidadVeterinario> disponibilidad(@PathVariable Long id) { return horarios.findByVeterinarioIdAndActivaTrueOrderByDiaSemanaAscHoraInicioAsc(id); }
    @PostMapping("/{id}/disponibilidad") public ResponseEntity<?> crear(@PathVariable Long id, @RequestBody Map<String,String> body) {
        var vet = veterinarios.findById(id).orElseThrow(() -> new IllegalArgumentException("El veterinario no existe."));
        if (!vet.isActivo()) return ResponseEntity.badRequest().body(Map.of("mensaje", "El veterinario está inactivo."));
        int dia = Integer.parseInt(body.get("diaSemana")); LocalTime inicio=LocalTime.parse(body.get("horaInicio")), fin=LocalTime.parse(body.get("horaFin"));
        if (dia < 1 || dia > 7 || !inicio.isBefore(fin)) return ResponseEntity.badRequest().body(Map.of("mensaje", "Día u horario no válido."));
        boolean cruza=horarios.findByVeterinarioIdAndDiaSemanaAndActivaTrue(id,dia).stream().anyMatch(h -> inicio.isBefore(h.getHoraFin()) && fin.isAfter(h.getHoraInicio()));
        if (cruza) return ResponseEntity.badRequest().body(Map.of("mensaje", "El horario se cruza con otra disponibilidad."));
        DisponibilidadVeterinario h=new DisponibilidadVeterinario(); h.setVeterinario(vet); h.setDiaSemana(dia); h.setHoraInicio(inicio); h.setHoraFin(fin); return ResponseEntity.ok(horarios.save(h));
    }
    @DeleteMapping("/{id}/disponibilidad/{horarioId}") public ResponseEntity<Void> desactivar(@PathVariable Long id, @PathVariable Long horarioId) {
        DisponibilidadVeterinario h=horarios.findById(horarioId).orElseThrow(() -> new IllegalArgumentException("El horario no existe."));
        if (!h.getVeterinario().getId().equals(id)) return ResponseEntity.notFound().build(); h.setActiva(false); horarios.save(h); return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}/especialidad") public ResponseEntity<?> asignarEspecialidad(@PathVariable Long id, @RequestBody Map<String,Long> body) {
        var vet=veterinarios.findById(id).orElseThrow(() -> new IllegalArgumentException("El veterinario no existe."));
        EspecialidadMedica e=especialidades.findById(body.get("especialidadId")).orElseThrow(() -> new IllegalArgumentException("La especialidad no existe.")); vet.setEspecialidad(e); return ResponseEntity.ok(veterinarios.save(vet));
    }
}
