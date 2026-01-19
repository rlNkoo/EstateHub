# EstateHub – Infrastructure & Services

# UserService

UserService is a microservice responsible for user management and security within the system.  
It acts as the central source of truth for user data and is responsible for issuing JWT tokens used by other services.

The service covers the full user lifecycle: registration, account activation, authentication, password reset, and role management.  
Communication with other services is handled asynchronously using domain events (Kafka).

---

## Responsibilities

- user registration and email uniqueness validation
- account activation using a one-time activation token
- user authentication and JWT token generation
- password reset flow (request / confirm)
- user role management (ADMIN)
- publishing domain events (event-driven architecture)

---

## Architecture

UserService is structured into clearly separated layers:

- **API** – REST controllers and DTOs, responsible only for the HTTP contract
- **Domain** – business logic, domain services, and rules
- **Persistence** – JPA entities, repositories, and mapping
- **Security** – security configuration and JWT handling
- **Events** – domain event publishing
- **Config / Exception** – technical configuration and global error handling

This separation ensures clarity, testability, and ease of future development.

---

## Account Activation

After registration, a user account is created in an inactive state.  
UserService generates a one-time activation token and publishes an event that can be consumed by an external service (e.g. NotificationService) to send an activation email.

Account activation:
- is a one-time operation
- is based on a token with an expiration date
- is required in order to log in

---

## Endpoints

### Public
- `POST /auth/register`
- `POST /auth/confirm-registration`
- `POST /auth/login`
- `POST /auth/password-reset/request`
- `POST /auth/password-reset/confirm`

### Administrative
- `PATCH /admin/users/{id}/roles`

---

## Events

UserService publishes domain events such as:

- user registered
- user activated
- password reset requested / confirmed

Thanks to event-based communication, other services are not directly coupled to the user database.

---

# ListingService

ListingService is a microservice responsible for managing real estate listings and their lifecycle.  
It handles listing creation, versioned content management, publication, controlled editing of published listings, and archiving.

The service enforces ownership rules, lifecycle transitions, and strict separation between public and internal views of listing content.  
All communication with other services is asynchronous and event-driven.

---

## Responsibilities

- creation and management of listing drafts
- versioned editing of listing content
- publication of listings
- controlled modification of published listings
- archiving listings
- enforcing ownership and role-based access rules
- exposing public read access for published listings
- publishing domain events

---

## Architecture

ListingService follows the same layered architecture pattern as other services:

- **API** – REST controllers and request/response DTOs
- **Domain** – business rules and lifecycle logic
- **Persistence** – JPA entities and repositories
- **Security** – ownership and authorization enforcement
- **Events** – domain event publishing
- **Exception** – global error handling

---

## Listing Lifecycle

Listings move through the following lifecycle:

```
DRAFT → PUBLISHED → ARCHIVED
```

Listings can only be modified by their owner or an ADMIN user.  
Archived listings are immutable and not publicly visible.

---

## Versioned Content Model

Listing content is stored as immutable versions.

- `ListingEntity` represents the aggregate root and holds the current state and version pointers
- `ListingVersionEntity` represents an immutable snapshot of listing content

Public users always see the **published version** of a listing.  
Authenticated owners and administrators may work with newer versions when editing listings.

---

## Editing Published Listings

Published listings are not modified directly.  
To change the content of a published listing, ListingService uses a controlled edit and republish flow.

The process consists of:

1. Starting an edit session, which creates a new working version based on the currently published version
2. Updating the working version, producing new immutable versions
3. Republishing the listing, which promotes the working version to the new published version

During this process, the listing remains published and publicly visible with the previous version until republished.

---

## Endpoints

### Public

- `GET /listings/{id}` – retrieve a published listing

### Authenticated

- `POST /listings` – create a new draft listing
- `PUT /listings/{id}` – update listing content
- `POST /listings/{id}/publish` – publish a draft listing
- `POST /listings/{id}/edit` – start editing a published listing
- `POST /listings/{id}/republish` – republish edited listing content
- `POST /listings/{id}/archive` – archive a listing
- `GET /listings/mine` – retrieve listings owned by the current user

---

## Events

ListingService publishes domain events when the public state of a listing changes:

- **ListingPublished** – when a listing is published
- **ListingUpdated** – when a published listing is republished with new content
- **ListingArchived** – when a listing is archived

Draft creation and draft or working-copy edits do not produce events.

These events are consumed by other services such as SearchService and NotificationService.

---

## 🧱 Infrastructure (Docker Compose)

The project uses local infrastructure managed via **Docker Compose**.
All required supporting services (database, messaging, search, storage) are defined in `docker-compose.yml`.

### Services in docker-compose.yml

| Component | Purpose | Default Port |
|---------|--------|--------------|
| PostgreSQL | Relational database (users, listings, etc.) | 5432 |
| Kafka | Event streaming / async communication | 9092 |
| Elasticsearch | Search & indexing engine | 9200 |
| Kibana | Elasticsearch UI | 5601 |
| MinIO | S3-compatible object storage (media files) | 9000 (API), 9001 (Console) |

Data is persisted using Docker volumes, so container restarts do not remove stored data.

---

### Start Infrastructure

Run from the **project root directory**:

```bash
docker compose up -d
```

Check status:
```bash
docker compose ps
```

View logs:
```bash
docker compose logs -f
```

Stop infrastructure:
```bash
docker compose down
```

Stop and remove volumes (⚠ deletes data):
```bash
docker compose down -v
```

---

### Quick Health Checks

**PostgreSQL**
```bash
docker exec -it estatehub-postgres pg_isready -U estatehub
```

**Kafka**
```bash
docker logs estatehub-kafka --tail 50
```

**Elasticsearch**
```bash
curl http://localhost:9200
```

**Kibana**
Open in browser:
```
http://localhost:5601
```

**MinIO**
- API: http://localhost:9000
- Console: http://localhost:9001  
  Credentials:
```
minioadmin / minioadmin
```

---

### Kafka End-to-End Test

Enter Kafka container:
```bash
docker exec -it estatehub-kafka sh
```

List topics:
```bash
kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Create topic:
```bash
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic test --partitions 1 --replication-factor 1
```

Producer:
```bash
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test
```

Consumer (second terminal):
```bash
docker exec -it estatehub-kafka sh
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test --from-beginning
```

---

## Kafka – Reading Events

### Enter Kafka container

```bash
docker exec -it estatehub-kafka sh
```

### List topics

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### Read events from a topic (from beginning)

```bash
/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic user-events \
  --from-beginning
```

```bash
/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic listing-events \
  --from-beginning
```

```bash
/opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic media-events \
  --from-beginning
```

### Exit container

```bash
exit
```

---

## 🐳 Building Docker Images

The project consists of multiple Spring Boot microservices.
Each service is built as a separate Docker image using **Java 25**.

---

## Services and Ports

| Service | Docker Image | Port |
|-------|--------------|------|
| API Gateway | estatehub/gateway-service | 8080 |
| User Service | estatehub/user-service | 8081 |
| Listing Service | estatehub/listing-service | 8082 |
| Media Service | estatehub/media-service | 8083 |
| Search Service | estatehub/search-service | 8084 |
| Notification Service | estatehub/notification-service | 8085 |

---

## Build Docker Images

All commands must be executed from the **project root directory**.

### API Gateway
```bash
docker build -f gateway-service/Dockerfile -t estatehub/gateway-service .
```

### User Service
```bash
docker build -f user-service/Dockerfile -t estatehub/user-service .
```

### Listing Service
```bash
docker build -f listing-service/Dockerfile -t estatehub/listing-service .
```

### Media Service
```bash
docker build -f media-service/Dockerfile -t estatehub/media-service .
```

### Search Service
```bash
docker build -f search-service/Dockerfile -t estatehub/search-service .
```

### Notification Service
```bash
docker build -f notification-service/Dockerfile -t estatehub/notification-service .
```

---

## ▶️ Run Services (docker run)

> Assumes infrastructure is already running (`docker compose up -d`)

### API Gateway
```bash
docker run --rm -p 8080:8080 estatehub/gateway-service
```

### User Service
```bash
docker run --rm -p 8081:8081 estatehub/user-service
```

### Listing Service
```bash
docker run --rm -p 8082:8082 estatehub/listing-service
```

### Media Service
```bash
docker run --rm -p 8083:8083 estatehub/media-service
```

### Search Service
```bash
docker run --rm -p 8084:8084 estatehub/search-service
```

### Notification Service
```bash
docker run --rm -p 8085:8085 estatehub/notification-service
```

---

## Useful Docker Commands

List images:
```bash
docker images
```

List containers:
```bash
docker ps -a
```

Follow container logs:
```bash
docker logs -f <container_name>
```
