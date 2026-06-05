package edu.udla.integracion.progreso2.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaRequest {

    private String idCita;
    private String paciente;
    private String correo;
    private String especialidad;
    private String fechaCita;
    private String sede;
    private Double valor;
}
