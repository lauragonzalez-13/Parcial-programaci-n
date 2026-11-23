
package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.entity.Mascota;
import com.uniminuto.clinica.entity.Cliente;
import com.uniminuto.clinica.repository.MascotaRepository;
import com.uniminuto.clinica.repository.ClienteRepository;

import java.text.Normalizer;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public ResponseEntity<?> crear(@RequestBody Mascota datos) {
        try {
            validar(datos);
            datos.setId(null);
            datos.setCliente(resolverCliente(datos));
            datos.setActiva(true);
            return ResponseEntity.status( org.springframework.http.HttpStatus.CREATED).body(mascotaRepository.save(datos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("mensaje", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
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
            existente.setCliente(resolverCliente(datos));
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
                || mascota.getCliente() == null
                || (mascota.getCliente().getId() == null
                        && (mascota.getCliente().getNombre() == null
                                || mascota.getCliente().getNombre().isBlank()))) {
            throw new IllegalArgumentException("Nombre, especie y cliente son obligatorios.");
        }
        if (mascota.getEdad() != null && mascota.getEdad() < 0) {
            throw new IllegalArgumentException("La edad no puede ser negativa.");
        }
    }

    private com.uniminuto.clinica.entity.Cliente resolverCliente(Mascota datos) {
        if (datos.getCliente().getId() != null) {
            return clienteRepository.findById(datos.getCliente().getId())
                    .orElseThrow(() -> new IllegalArgumentException("El cliente indicado no existe."));
        }
        String nombre = datos.getCliente().getNombre().trim();
        String nombreNormalizado = normalizar(nombre);
        List<Cliente> coincidencias = clienteRepository.findAll().stream()
                .filter(cliente -> nombreNormalizado.equals(normalizar(cliente.getNombre()))
                        || nombreNormalizado.equals(normalizar(
                                cliente.getNombre() + " " + (cliente.getApellido() == null
                                        ? "" : cliente.getApellido()))))
                .toList();
        if (coincidencias.size() == 1) {
            return coincidencias.get(0);
        }
        if (coincidencias.size() > 1) {
            throw new IllegalArgumentException(
                    "Hay varios clientes con ese nombre. Especifica el nombre completo del cliente.");
        }
        if (nombre.length() > 100) {
            throw new IllegalArgumentException("El nombre del cliente no puede superar 100 caracteres.");
        }
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setApellido("");
        cliente.setDocumento("CLI-" + java.util.UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16));
        cliente.setActivo(true);
        return clienteRepository.save(cliente);
    }

    private String normalizar(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(java.util.Locale.ROOT);
    }
}
