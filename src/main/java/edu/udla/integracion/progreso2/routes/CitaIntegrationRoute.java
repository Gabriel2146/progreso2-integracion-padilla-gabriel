package edu.udla.integracion.progreso2.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.udla.integracion.progreso2.model.CitaRequest;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class CitaIntegrationRoute extends RouteBuilder {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void configure() throws Exception {

        // Manejo global de errores con DLQ
        errorHandler(deadLetterChannel("direct:manejo-error-camel")
                .maximumRedeliveries(3)
                .redeliveryDelay(2000)
                .logRetryAttempted(true));

        // -------------------------------------------------------
        // RUTA PRINCIPAL: Orquesta el flujo completo
        // -------------------------------------------------------
        from("direct:cita-valida")
                .routeId("ruta-principal")
                .log(">>> Cita recibida para integración: ${body}")
                .multicast().parallelProcessing()
                    .to("direct:billing",
                        "direct:eventos-pubsub",
                        "direct:csv-legado")
                .end()
                .log(">>> Integración completada para cita");

        // -------------------------------------------------------
        // RF2 - Point-to-Point: Facturación (billing.queue)
        // Solo un consumidor procesará este mensaje
        // -------------------------------------------------------
        from("direct:billing")
                .routeId("ruta-facturacion-p2p")
                .log(">>> [P2P] Preparando comando de facturación")
                .process(exchange -> {
                    CitaRequest cita = exchange.getIn().getBody(CitaRequest.class);
                    Map<String, Object> mensaje = new LinkedHashMap<>();
                    mensaje.put("idCita", cita.getIdCita());
                    mensaje.put("paciente", cita.getPaciente());
                    mensaje.put("especialidad", cita.getEspecialidad());
                    mensaje.put("valor", cita.getValor());
                    mensaje.put("tipoMensaje", "COMANDO_FACTURAR_CITA");
                    exchange.getIn().setBody(objectMapper.writeValueAsString(mensaje));
                    exchange.getIn().setHeader("content-type", "application/json");
                })
                .log(">>> [P2P] Enviando a billing.queue: ${body}")
                .to("spring-rabbitmq:billing?routingKey=billing.queue");

        // -------------------------------------------------------
        // RF3 - Publish/Subscribe: appointments.events (fanout)
        // notifications.queue y analytics.queue recibirán el evento
        // -------------------------------------------------------
        from("direct:eventos-pubsub")
                .routeId("ruta-pubsub-eventos")
                .log(">>> [PubSub] Preparando evento CITA_CONFIRMADA")
                .process(exchange -> {
                    CitaRequest cita = exchange.getIn().getBody(CitaRequest.class);
                    Map<String, Object> evento = new LinkedHashMap<>();
                    evento.put("idCita", cita.getIdCita());
                    evento.put("paciente", cita.getPaciente());
                    evento.put("correo", cita.getCorreo());
                    evento.put("especialidad", cita.getEspecialidad());
                    evento.put("fechaCita", cita.getFechaCita());
                    evento.put("sede", cita.getSede());
                    evento.put("tipoEvento", "CITA_CONFIRMADA");
                    exchange.getIn().setBody(objectMapper.writeValueAsString(evento));
                    exchange.getIn().setHeader("content-type", "application/json");
                })
                .log(">>> [PubSub] Publicando en appointments.events: ${body}")
                .to("spring-rabbitmq:appointments.events");

        // -------------------------------------------------------
        // RF4 - Transferencia de archivos: sistema legado (CSV)
        // -------------------------------------------------------
        from("direct:csv-legado")
                .routeId("ruta-csv-auditoria")
                .log(">>> [CSV] Generando línea de auditoría")
                .process(exchange -> {
                    CitaRequest cita = exchange.getIn().getBody(CitaRequest.class);
                    String linea = String.format(Locale.US, "%s,%s,%s,%s,%s,%s,%.2f%n",
                            cita.getIdCita(),
                            cita.getPaciente(),
                            cita.getCorreo(),
                            cita.getEspecialidad(),
                            cita.getFechaCita(),
                            cita.getSede(),
                            cita.getValor());
                    exchange.getIn().setBody(linea);
                })
                .to("file:data/outbox?fileName=auditoria-citas.csv&fileExist=Append&charset=UTF-8")
                .log(">>> [CSV] Línea escrita en auditoria-citas.csv");

        // -------------------------------------------------------
        // RF5 - Manejo de errores Camel (DLQ fallback)
        // -------------------------------------------------------
        from("direct:manejo-error-camel")
                .routeId("ruta-error-camel")
                .log(">>> [ERROR] Fallo en ruta Camel: ${exception.message}")
                .process(exchange -> {
                    Exception ex = exchange.getProperty("CamelExceptionCaught", Exception.class);
                    String msg = ex != null ? ex.getMessage() : "Error desconocido";
                    exchange.getIn().setBody("ERROR_CAMEL | " + msg);
                });
    }
}
