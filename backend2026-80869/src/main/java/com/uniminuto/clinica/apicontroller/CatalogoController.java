package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.entity.Cliente;
import com.uniminuto.clinica.entity.Mascota;
import com.uniminuto.clinica.entity.Veterinario;
import com.uniminuto.clinica.repository.ClienteRepository;
import com.uniminuto.clinica.repository.MascotaRepository;
import com.uniminuto.clinica.repository.VeterinarioRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.text.Normalizer;
import java.util.UUID;

@RestController
public class CatalogoController {
    private final ClienteRepository clienteRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;

    public CatalogoController(ClienteRepository clienteRepository,
            MascotaRepository mascotaRepository,
            VeterinarioRepository veterinarioRepository) {
        this.clienteRepository = clienteRepository;
        this.mascotaRepository = mascotaRepository;
        this.veterinarioRepository = veterinarioRepository;
    }

    @GetMapping("/clientes")
    public List<Cliente> clientes() {
        return clienteRepository.findAll();
    }

    @PostMapping("/clientes")
    public ResponseEntity<?> crearCliente(@RequestBody java.util.Map<String, String> datos) {
        String nombre = datos.get("nombre");
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio.");
        }
        String nombreNormalizado = normalizar(nombre);
        List<Cliente> coincidencias = clienteRepository.findAll().stream()
                .filter(cliente -> nombreNormalizado.equals(normalizar(cliente.getNombre()))
                        || nombreNormalizado.equals(normalizar(
                                cliente.getNombre() + " " + (cliente.getApellido() == null
                                        ? "" : cliente.getApellido()))))
                .toList();
        if (coincidencias.size() == 1) {
            return ResponseEntity.ok(coincidencias.get(0));
        }
        if (coincidencias.size() > 1) {
            return ResponseEntity.status(409).body(Map.of(
                    "mensaje", "Hay varios clientes con ese nombre. Selecciona el correcto de la lista."));
        }
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre.trim());
        cliente.setApellido("");
        cliente.setDocumento("CLI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        cliente.setActivo(true);
        return ResponseEntity.status(201).body(clienteRepository.save(cliente));
    }

    @GetMapping("/clientes/{clienteId}")
    public Cliente cliente(@PathVariable Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));
    }

    @GetMapping("/veterinarios")
    public List<Veterinario> veterinarios() {
        return veterinarioRepository.findAll();
    }

    @PostMapping("/veterinarios")
    public ResponseEntity<Veterinario> crearVeterinario(@RequestBody Veterinario datos) {
        if (datos == null || datos.getNombre() == null || datos.getNombre().isBlank()
                || datos.getApellido() == null || datos.getApellido().isBlank()
                || datos.getDocumento() == null || datos.getDocumento().isBlank()) {
            throw new IllegalArgumentException("Nombre, apellido y documento son obligatorios.");
        }

        String documento = datos.getDocumento().trim();
        if (veterinarioRepository.existsByDocumentoIgnoreCase(documento)) {
            throw new IllegalArgumentException("Ya existe un veterinario con ese documento.");
        }

        Veterinario nuevo = new Veterinario();
        nuevo.setNombre(datos.getNombre().trim());
        nuevo.setApellido(datos.getApellido().trim());
        nuevo.setDocumento(documento);
        nuevo.setTarjetaProfesional(datos.getTarjetaProfesional());
        nuevo.setCorreo(datos.getCorreo());
        nuevo.setActivo(true);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(veterinarioRepository.save(nuevo));
    }

    @GetMapping("/clientes/{clienteId}/mascotas")
    public List<Mascota> mascotasDelCliente(@PathVariable Long clienteId) {
        return mascotaRepository.findByClienteId(clienteId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
    }

    private String limpiar(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private String normalizar(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(java.util.Locale.ROOT);
    }
}
