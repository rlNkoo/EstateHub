## 🐳 Building Docker Images

The project consists of multiple Spring Boot microservices.  
Each service is built as a separate Docker image using **Java 25**.

---

## Services and Ports

| Service               | Docker Image                         | Port |
|-----------------------|--------------------------------------|------|
| API Gateway           | `estatehub/gateway-service`          | 8080 |
| User Service          | `estatehub/user-service`             | 8081 |
| Listing Service       | `estatehub/listing-service`          | 8082 |
| Media Service         | `estatehub/media-service`            | 8083 |
| Search Service        | `estatehub/search-service`           | 8084 |
| Notification Service  | `estatehub/notification-service`     | 8085 |

---

## Build All Docker Images

All commands should be executed from the **root directory** of the project.

### Build API Gateway
```bash
docker build -f gateway-service/Dockerfile -t estatehub/gateway-service .
```

### Build User Service
```bash
docker build -f user-service/Dockerfile -t estatehub/user-service .
```

### Build Listing Service
```bash
docker build -f listing-service/Dockerfile -t estatehub/listing-service .
```

### Build Media Service
```bash
docker build -f media-service/Dockerfile -t estatehub/media-service .
```

### Build Search Service
```bash
docker build -f search-service/Dockerfile -t estatehub/search-service .
```

### Build Notification Service
```bash
docker build -f notification-service/Dockerfile -t estatehub/notification-service .
```

### Run a Single Service (Example)
```bash
docker run --rm -p 8080:8080 estatehub/gateway-service
```
