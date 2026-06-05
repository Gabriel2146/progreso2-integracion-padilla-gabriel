package edu.udla.integracion.progreso2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.udla.integracion.progreso2.model.CitaRequest;
import edu.udla.integracion.progreso2.service.CitaValidationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class CitaController {

    @Autowired
    private CitaValidationService validationService;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/citas")
    public ResponseEntity<?> registrarCita(@RequestBody CitaRequest cita) {
        log.info("Solicitud recibida - POST /api/citas: {}", cita);

        List<String> errores = validationService.validar(cita);

        if (!errores.isEmpty()) {
            try {
                String payload = objectMapper.writeValueAsString(cita);
                validationService.registrarError(cita, errores, payload);
            } catch (Exception e) {
                log.error("No se pudo serializar el payload para el log de errores", e);
                validationService.registrarError(cita, errores, cita != null ? cita.toString() : "null");
            }
            return ResponseEntity.badRequest().body(Map.of(
                    "estado", "RECHAZADA",
                    "errores", errores
            ));
        }

        producerTemplate.sendBody("direct:cita-valida", cita);

        log.info("Cita {} procesada e integrada correctamente", cita.getIdCita());
        return ResponseEntity.ok(Map.of(
                "estado", "REGISTRADA",
                "mensaje", "Cita registrada y distribuida correctamente",
                "idCita", cita.getIdCita()
        ));
    }
}
