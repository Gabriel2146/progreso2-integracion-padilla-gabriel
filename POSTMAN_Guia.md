# Postman: consultas para probar el endpoint `/api/citas`

## 1) Endpoint base
- **URL:** `http://localhost:8080/api/citas`
- **Método:** `POST`
- **Header:** `Content-Type: application/json`

---

## 2) Request válido (debería devolver 200)
**Body (raw / JSON):**
```json
{
  "idCita": "CITA-1001",
  "paciente": "Ana Torres",
  "correo": "ana.torres@email.com",
  "especialidad": "Cardiología",
  "fechaCita": "2026-06-15",
  "sede": "Centro Norte",
  "valor": 45.50
}
```

---

## 3) Request inválido (debería devolver 400)
**Body (raw / JSON):**
```json
{
  "paciente": "Ana Torres"
}
```

---

## 4) Colección lista para importar (recomendado)
Archivo: `POSTMAN_COLLECCION.json`

Pasos:
1. Abre Postman
2. **Import** → selecciona `POSTMAN_COLLECCION.json`
3. Ejecuta los requests:
   - `Registrar cita - Válida (200)`
   - `Registrar cita - Inválida (400)`

---

## 5) ¿Qué revisar si no funciona?
1. Contenedores:
   - `docker ps`
2. Logs del backend:
   - `docker-compose logs -f app`
3. Logs RabbitMQ:
   - `docker-compose logs -f rabbitmq`

