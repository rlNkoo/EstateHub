# EstateHub – Infrastructure & Services

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
It acts as the central source of truth for listing data and exposes both public and authenticated access to listing content.

The service covers the full listing lifecycle: draft creation, content updates, publication, and archiving.
Communication with other services is handled asynchronously using domain events (Kafka).

---

## Responsibilities

- creation and management of real estate listings
- handling the full listing lifecycle (draft, published, archived)
- versioned storage of listing content
- publication and archiving of listings
- enforcing ownership and role-based access rules
- exposing public read access for published listings
- publishing domain events (event-driven architecture)

---

## Architecture

ListingService is structured into clearly separated layers:

- **API** – REST controllers and DTOs, responsible only for the HTTP contract
- **Domain** – business logic, lifecycle rules, and validations
- **Persistence** – JPA entities, repositories, and mapping
- **Security** – ownership and authorization enforcement
- **Events** – domain event publishing
- **Config / Exception** – technical configuration and global error handling

This separation ensures clarity, testability, and ease of future development.

---

## Listing Lifecycle

Listings move through the following lifecycle:

```
DRAFT → PUBLISHED → ARCHIVED
```

- Listings are created as drafts.
- Draft listings are only visible to their owner or ADMIN users.
- Published listings are publicly visible.
- Archived listings are immutable and not publicly visible.

Only the listing owner or an ADMIN user may modify a listing.

---

## Versioned Content Model

ListingService uses a versioned content model.

- Listing metadata and state are stored in `ListingEntity`
- Listing content is stored as immutable versions in `ListingVersionEntity`

Each update creates a new content version, allowing the system to track changes over time and ensure consistency between stored data and public views.

---

## Read Access Semantics

- Public users can access only published listings.
- Owners and ADMIN users can access their own draft listings.
- The service automatically resolves the correct version of content to return based on listing state.

---

## Endpoints

### Public
- `GET /listings/{id}` – retrieve a published listing

### Authenticated
- `POST /listings` – create a new draft listing
- `PUT /listings/{id}` – update listing content
- `POST /listings/{id}/publish` – publish a draft listing
- `POST /listings/{id}/archive` – archive a listing
- `GET /listings/mine` – retrieve listings owned by the current user

---

## Events

ListingService publishes domain events when the public state of a listing changes:

- **ListingPublished** – when a draft listing is published
- **ListingUpdated** – when a published listing content changes
- **ListingArchived** – when a listing is archived

Thanks to event-based communication, other services are not directly coupled to the listing database.

---

## Media Integration (Event-Driven)

ListingService integrates with MediaService using asynchronous events.

### Consumed Events (from `media-events` topic)

- **PhotoUploadedV1**
  - adds `mediaId` to the current readable listing version
- **PhotoDeletedV1**
  - removes `mediaId` from the listing version

Photo changes are applied to the version currently used for reads:
- published version for `PUBLISHED` listings
- current draft version for `DRAFT` listings

If a listing is `PUBLISHED`, photo changes trigger a `ListingUpdated` event.

---

## Search Service Integration

ListingService integrates with **SearchService** to provide fast
searching and filtering of published listings.

The integration is event‑driven. Whenever the public state of a listing
changes, ListingService publishes events that are consumed by
SearchService to update the Elasticsearch index.

### Published Events

-   **ListingPublishedV1**
-   **ListingUpdatedV1**
-   **ListingArchivedV1**

### Reindex Source Endpoint

ListingService exposes an administrative endpoint used by SearchService
when rebuilding the search index:

`GET /admin/listings/published`

The endpoint returns paginated data for all published listings
including:

-   listing metadata
-   versioned content
-   address data
-   price and area
-   property attributes
-   photo identifiers

This endpoint is intended strictly for **search index rebuild
operations** and requires the `ADMIN` role.

---

# MediaService

MediaService is a microservice responsible for managing media files (photos) associated with real estate listings.  
It handles secure upload, storage, processing, and controlled access to listing photos.

The service integrates with external object storage (S3-compatible, MinIO) and communicates with other services using domain events.  
It does not store media files in the database – only metadata and references.

---

## Responsibilities

- validating uploaded media files (size, content type)
- uploading original photos to object storage
- generating and storing image thumbnails
- managing media metadata
- enforcing ownership and access rules
- exposing presigned URLs for secure access
- publishing media domain events (event-driven architecture)

---

## Architecture

MediaService follows the same layered architecture as other services:

- **API** – REST controllers and upload endpoints
- **Domain** – media rules, ownership verification, and processing logic
- **Persistence** – media metadata entities and repositories
- **Storage** – S3-compatible object storage integration (MinIO)
- **Security** – JWT-based authentication and ownership enforcement
- **Events** – media event publishing
- **Config / Exception** – technical configuration and global error handling

---

## Media Access Rules

- Photos of published listings are publicly readable (via presigned URLs).
- Photos of draft listings are accessible only to the listing owner or ADMIN users.
- Upload and deletion are restricted to listing owners and ADMIN users.

---

## Endpoints

### Authenticated
- `POST /media/listings/{listingId}/photos` – upload a photo
- `DELETE /media/photos/{mediaId}` – delete a photo

### Public
- `GET /media/listings/{listingId}/photos` – list photos for a listing
- `GET /media/photos/{mediaId}` – retrieve photo metadata and URLs

---

## Events

MediaService publishes domain events such as:

- **PhotoUploadedV1**
- **PhotoDeletedV1**

These events are consumed by ListingService to keep listing photo references consistent.

---

# SearchService

SearchService is a microservice responsible for **searching, filtering,
and indexing listings**.

It provides a fast read model backed by **Elasticsearch**.\
Unlike other services, SearchService does not own listing data. Instead,
it builds and maintains a search index based on events published by
ListingService.

---

## Responsibilities

-   maintaining Elasticsearch index of listings
-   providing search and filtering API
-   processing listing lifecycle events
-   supporting full index rebuilds
-   exposing administrative diagnostics endpoints

---

## Architecture

SearchService follows the same layered architecture pattern:

-   **API** -- search controllers and DTOs
-   **Domain** -- search orchestration and indexing logic
-   **Persistence** -- Elasticsearch repositories and documents
-   **Integration** -- Kafka event consumers
-   **Security** -- JWT authentication for administrative endpoints
-   **Config / Exception** -- technical configuration and global error
    handling

---

## Search Model

SearchService stores listing documents in Elasticsearch using:

`SearchListingDocument`

Each document contains:

-   listing id
-   title and description
-   price and currency
-   address data
-   property attributes (area, rooms, floor)
-   photo identifiers
-   publication timestamp

Only listings with status **PUBLISHED** are indexed.

---

## Event‑Driven Index Updates

SearchService consumes events from the **listing-events** Kafka topic.

### Consumed Events

-   **ListingPublishedV1** -- creates a new document
-   **ListingUpdatedV1** -- updates the document
-   **ListingArchivedV1** -- removes the document

This keeps the search index synchronized with the public state of
listings.

---

## Public Endpoints

### Search Listings

`GET /search/listings`

Supports filtering by:

-   full‑text query
-   city / country
-   price range
-   area range
-   rooms
-   floor
-   property type

Also supports:

-   pagination
-   sorting

---

### Get Listing

`GET /search/listings/{listingId}`

Returns a single listing document from the search index.

---

## Administrative Endpoints

### Index Info

`GET /search/admin/index-info`

Returns:

-   index name
-   existence status
-   number of indexed documents

Requires `ADMIN` role.

---

### Reindex

`POST /search/admin/reindex`

Triggers a full rebuild of the Elasticsearch index.

Process:

1.  SearchService calls\
    `ListingService → /admin/listings/published`
2.  Fetches all listings page by page
3.  Recreates Elasticsearch documents
4.  Rebuilds the search index

Used for:

-   disaster recovery
-   index rebuilds
-   schema changes

---

# NotificationService

NotificationService is a microservice responsible for sending transactional emails based on domain events.  
It consumes events from Kafka and delivers email notifications to end users without direct coupling to other services’ databases.

The service is fully event-driven and operates asynchronously.

---

## Responsibilities

- consuming domain events from Kafka (`user-events`, `listing-events`)
- sending transactional emails (registration, activation, password reset, listing lifecycle)
- maintaining a local read model for resolving `userId → email`
- ensuring idempotent event processing
- logging notification delivery status (`SENT`, `FAILED`)
- safe handling of event reprocessing (at-least-once delivery)

---

## Architecture

NotificationService follows the same layered architecture pattern as other services:

- **Domain** – notification orchestration and processing logic
- **Persistence** – notification logs and user email index
- **Integration (Kafka)** – event consumers and deserialization
- **Mail** – Thymeleaf template rendering and SMTP delivery
- **Config / Exception** – technical configuration and global error handling

The service does not expose public business endpoints and is not routed through the API Gateway.

---

## Event Consumption

NotificationService consumes events from the following Kafka topics:

### From `user-events`
- **UserRegisteredV1**
- **UserActivatedV1**
- **PasswordResetRequestedV1**
- **PasswordResetCompletedV1**

### From `listing-events`
- **ListingPublishedV1**
- **ListingUpdatedV1**
- **ListingArchivedV1**

Events are deserialized using the shared `EventEnvelope` from the `common-events` module.

Each event is processed exactly once using **eventId-based idempotency**.

---

## Idempotency & Delivery Log

Every processed event is stored in the `notification_log` table.

The log contains:
- `eventId` (unique constraint)
- `eventType`
- recipient email
- related `userId` / `listingId`
- status: `RECEIVED → SENT → FAILED`
- number of delivery attempts
- error details
- timestamps

This ensures safe retries and operational visibility.

---

## User Email Read Model

Listing events contain only `ownerId`, so NotificationService maintains a local read model:

```
user_email_index
```

This table is populated from user-related events and allows resolving:

```
userId → email
```

This keeps the system fully event-driven and avoids synchronous calls to UserService.

---

## Email Delivery

Emails are sent using:

- Spring Mail
- Thymeleaf templates
- SMTP (MailHog in local development)

Transactional emails include:

### User Lifecycle
- registration (activation token)
- account activated
- password reset requested (reset token)
- password reset completed

### Listing Lifecycle
- listing published
- listing updated
- listing archived

Emails contain tokens for backend confirmation flows and instructions for API usage.

---

## Processing Flow

1. A business service publishes a domain event.
2. Kafka delivers the event to NotificationService.
3. The service:
  - checks idempotency using `eventId`
  - resolves the recipient email
  - renders the appropriate email template
  - sends the email via SMTP
  - updates the `notification_log` status

---

## Failure Handling

If email delivery fails:

- the notification is marked as `FAILED`
- error details are stored in the log
- the event can be safely retried without duplication

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
