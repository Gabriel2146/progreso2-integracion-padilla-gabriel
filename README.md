# Progreso 2 - Integración de Sistemas: Salud360

**Estudiante:** Gabriel Padilla  
**Asignatura:** Integración de Sistemas  
**Evaluación:** Examen Práctico Progreso 2  
**Fecha:** 2026-06-04

---

## Repositorio GitHub de la solución

> https: https://github.com/Gabriel2146/progreso2-integracion-padilla-gabriel 

---

## Descripción

Solución de integración para la organización **Salud360**, que automatiza el flujo de registro y distribución de citas médicas confirmadas mediante:

- **API REST** para recibir solicitudes de cita
- **Apache Camel** para orquestar el flujo de integración
- **RabbitMQ** con patrones Point-to-Point (facturación) y Publish/Subscribe (notificaciones + analítica)
- **Archivo CSV** para integrar con el sistema legado de auditoría
- **Log de errores** para solicitudes inválidas

---

## Tecnologías

| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Apache Camel | 4.4.4 |
| RabbitMQ | 3.13 |
| Docker / Docker Compose | 24+ |

---

## Levantar RabbitMQ

```bash
docker-compose up -d
```

Acceder al panel de administración: http://localhost:15672  
Usuario: `guest` / Contraseña: `guest`

---

## Ejecutar la aplicación

### Opción 1: con Docker (recomendado)

> requisito: tener instalado `mvn`, funciona igual ya que la build se hace dentro del container

1) Levantar RabbitMQ + la aplicación:

```bash
docker-compose up -d --build
```

2) La aplicación levanta en: http://localhost:8080

### Opción 2: con Maven (si si está instalado)

```bash
mvn spring-boot:run
```

La aplicación levanta en: http://localhost:8080


---

## Endpoint disponible

| Método | URL | Descripción |
|---|---|---|
| POST | `/api/citas` | Registrar una solicitud de cita médica |

---

## Ejemplo de request válido

```bash
curl -X POST http://localhost:8080/api/citas \
  -H "Content-Type: application/json" \
  -d '{
    "idCita": "CITA-1001",
    "paciente": "Ana Torres",
    "correo": "ana.torres@email.com",
    "especialidad": "Cardiología",
    "fechaCita": "2026-06-15",
    "sede": "Centro Norte",
    "valor": 45.50
  }'
```

**Respuesta:**
```json
{
  "estado": "REGISTRADA",
  "mensaje": "Cita registrada y distribuida correctamente",
  "idCita": "CITA-1001"
}
```

---

## Ejemplo de request inválido

```bash
curl -X POST http://localhost:8080/api/citas \
  -H "Content-Type: application/json" \
  -d '{
    "paciente": "Ana Torres"
  }'
```

**Respuesta (400 Bad Request):**
```json
{
  "estado": "RECHAZADA",
  "errores": [
    "idCita es obligatorio",
    "correo es obligatorio",
    "especialidad es obligatoria",
    "fechaCita es obligatoria",
    "sede es obligatoria",
    "valor debe ser mayor a 0"
  ]
}
```

---
Adicional: se creo el json de la coleccion con todos los parametros para importarlo a Postman y poder validar el funcionamiento mas rapido

## Arquitectura de integración

### ¿Dónde se aplica Point-to-Point?

En la ruta `direct:billing` → exchange `billing` (tipo **direct**) → cola `billing.queue`.

El sistema de facturación es el **único consumidor** de esta cola. El mensaje solo se procesará una vez, garantizando que no se generen duplicados en las órdenes de cobro. Este es el patrón **Point-to-Point Channel**.

### ¿Dónde se aplica Publish/Subscribe?

En la ruta `direct:eventos-pubsub` → exchange `appointments.events` (tipo **fanout**) → colas `notifications.queue` y `analytics.queue`.

El mismo evento `CITA_CONFIRMADA` llega **simultáneamente** a ambos consumidores (sistema de notificaciones y sistema de analítica). Ninguno interfiere con el otro. Este es el patrón **Publish/Subscribe Channel**.

### ¿Dónde se aplica transferencia de archivos?

En la ruta `direct:csv-legado` → componente `file` de Camel → `data/outbox/auditoria-citas.csv`.

El sistema legado de auditoría no tiene API ni mensajería. Solo puede leer archivos CSV desde una carpeta compartida. La integración escribe una línea por cada cita válida procesada.

### ¿Cómo se manejan los errores?

- **Validación en API:** Si el payload es inválido, el controlador responde HTTP 400 y registra el rechazo en `data/errors/citas-rechazadas.log` con timestamp, idCita, motivos y payload recibido.
- **Errores en rutas Camel:** Se usa un Dead Letter Channel (`direct:manejo-error-camel`) con 3 reintentos y delay de 2 segundos entre intentos.

---

## Evidencia esperada

Para verificar el funcionamiento completo:

1. `docker-compose up -d` → RabbitMQ corriendo en http://localhost:15672
2. `mvn spring-boot:run` → aplicación iniciada sin errores
3. Request válido → respuesta 200, mensaje en `billing.queue`, evento en `notifications.queue` y `analytics.queue`, nueva línea en `data/outbox/auditoria-citas.csv`
4. Request inválido → respuesta 400, línea nueva en `data/errors/citas-rechazadas.log`

---

## Estructura del proyecto

```
progreso2-integracion-padilla-gabriel/
├── README.md
├── docker-compose.yml
├── pom.xml
├── src/main/java/edu/udla/integracion/progreso2/
│   ├── Progreso2Application.java
│   ├── config/
│   │   └── RabbitMQConfig.java
│   ├── controller/
│   │   └── CitaController.java
│   ├── model/
│   │   └── CitaRequest.java
│   ├── routes/
│   │   └── CitaIntegrationRoute.java
│   └── service/
│       └── CitaValidationService.java
├── src/main/resources/
│   └── application.properties
├── data/
│   ├── outbox/auditoria-citas.csv
│   └── errors/citas-rechazadas.log
└── docs/capturas/
```
