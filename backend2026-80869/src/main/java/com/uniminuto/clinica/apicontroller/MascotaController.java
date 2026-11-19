
package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.entity.Mascota;
import com.uniminuto.clinica.repository.MascotaRepository;
import com.uniminuto.clinica.repository.ClienteRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mascotas")
public class MascotaController {

    private final MascotaRepository mascotaRepository;
    private final ClienteRepository clienteRepository;

    public MascotaController(MascotaRepository mascotaRepository, ClienteRepository clienteRepository) {
        this.mascotaRepository = mascotaRepository;
        this.clienteRepository = clienteRepository;
    }

    @GetMapping
    public ResponseEntity<List<Mascota>> listar() {
        List<Mascota> mascotas = mascotaRepository.findAll();
        return ResponseEntity.ok(mascotas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mascota> buscarPorId(@PathVariable Long id) {
        return mascotaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Mascota datos) {
        try {
            validar(datos);
            datos.setId(null);
            datos.setCliente(clienteRepository.findById(datos.getCliente().getId())
                    .orElseThrow(() -> new IllegalArgumentException("El cliente indicado no existe.")));
            datos.setActiva(true);
            return ResponseEntity.status( org.springframework.http.HttpStatus.CREATED).body(mascotaRepository.save(datos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("mensaje", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Mascota datos) {
        try {
            validar(datos);
            Mascota existente = mascotaRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("La mascota no existe."));
            existente.setNombre(datos.getNombre().trim());
            existente.setEspecie(datos.getEspecie().trim());
            existente.setRaza(datos.getRaza());
            existente.setSexo(datos.getSexo());
            existente.setEdad(datos.getEdad());
            existente.setCliente(clienteRepository.findById(datos.getCliente().getId())
                    .orElseThrow(() -> new IllegalArgumentException("El cliente indicado no existe.")));
            return ResponseEntity.ok(mascotaRepository.save(existente));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("mensaje", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        return mascotaRepository.findById(id).<ResponseEntity<?>>map(mascota -> {
            mascota.setActiva(false);
            return ResponseEntity.ok(mascotaRepository.save(mascota));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void validar(Mascota mascota) {
        if (mascota == null || mascota.getNombre() == null || mascota.getNombre().isBlank()
                || mascota.getEspecie() == null || mascota.getEspecie().isBlank()
                || mascota.getCliente() == null || mascota.getCliente().getId() == null) {
            throw new IllegalArgumentException("Nombre, especie y cliente son obligatorios.");
        }
        if (mascota.getEdad() != null && mascota.getEdad() < 0) {
            throw new IllegalArgumentException("La edad no puede ser negativa.");
        }
    }
}
