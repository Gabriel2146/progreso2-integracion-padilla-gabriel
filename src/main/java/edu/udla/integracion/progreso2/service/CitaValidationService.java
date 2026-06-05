package edu.udla.integracion.progreso2.service;

import edu.udla.integracion.progreso2.model.CitaRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CitaValidationService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<String> validar(CitaRequest cita) {
        List<String> errores = new ArrayList<>();

        if (cita == null) {
            errores.add("El payload es nulo o vacío");
            return errores;
        }
        if (esBlanco(cita.getIdCita()))        errores.add("idCita es obligatorio");
        if (esBlanco(cita.getPaciente()))      errores.add("paciente es obligatorio");
        if (esBlanco(cita.getCorreo()))        errores.add("correo es obligatorio");
        if (esBlanco(cita.getEspecialidad()))  errores.add("especialidad es obligatoria");
        if (esBlanco(cita.getFechaCita()))     errores.add("fechaCita es obligatoria");
        if (esBlanco(cita.getSede()))          errores.add("sede es obligatoria");
        if (cita.getValor() == null || cita.getValor() <= 0) {
            errores.add("valor debe ser mayor a 0");
        }

        return errores;
    }

    public void registrarError(CitaRequest cita, List<String> motivos, String payloadJson) {
        try {
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String idCita = (cita != null && cita.getIdCita() != null) ? cita.getIdCita() : "N/A";
            String linea = timestamp
                    + " | idCita: " + idCita
                    + " | Motivos: " + String.join(", ", motivos)
                    + " | Payload: " + payloadJson
                    + System.lineSeparator();

            Path logPath = Paths.get("data/errors/citas-rechazadas.log");
            Files.createDirectories(logPath.getParent());
            Files.write(logPath, linea.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            log.warn("Cita rechazada - idCita: {} | Motivos: {}", idCita, motivos);
        } catch (IOException e) {
            log.error("No se pudo escribir en el log de errores", e);
        }
    }

    private boolean esBlanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
