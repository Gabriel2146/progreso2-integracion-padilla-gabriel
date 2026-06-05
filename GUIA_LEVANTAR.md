# Guía rápida: Levantar y volver a levantar el proyecto (Docker)

> Recomendado porque en tu entorno no existe `mvn` en el host.

---

## 1) Antes de levantar
1. Abre una terminal en la carpeta del proyecto:
   `progreso2-integracion-padilla-gabriel/`
2. Asegúrate de que Docker Desktop esté abierto.

---

## 2) Levantar todo por primera vez
Ejecuta:
```bash
docker-compose up -d --build
```

- RabbitMQ queda publicado en: http://localhost:15672 (user `guest` / pass `guest`)
- La API queda publicada en: http://localhost:8080

---

## 3) (Opcional) Ver estado de contenedores
```bash
docker ps
```

---

## 4) Probar el endpoint (opcional)
Cuando el backend esté “Up”:

### Request válido
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

### Request inválido
```bash
curl -X POST http://localhost:8080/api/citas \
  -H "Content-Type: application/json" \
  -d '{
    "paciente": "Ana Torres"
  }'
```

> Nota: si tu terminal interpreta mal `curl` (por ejemplo en PowerShell), usa Postman o ejecuta `curl` en CMD.

---

## 5) Cuando cierres el proyecto (para apagar)
Para detener servicios sin borrar:
```bash
docker-compose down
```

Si quieres detener y además borrar imágenes/volúmenes (más agresivo):
```bash
docker-compose down --volumes
```

---

## 6) Volver a levantar después de haberlo cerrado
En la misma carpeta del proyecto ejecuta:
```bash
docker-compose up -d
```

> Si cambiaste código y quieres reconstruir:
```bash
docker-compose up -d --build
```

---

## 7) Qué revisar si no levanta
1. Logs del backend:
```bash
docker-compose logs -f app
```
2. Logs de RabbitMQ:
```bash
docker-compose logs -f rabbitmq
```
3. Estado de contenedores:
```bash
docker ps
```

