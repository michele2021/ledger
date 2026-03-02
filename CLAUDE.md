# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **financial accounting/ledger system** built with **Java 21** and **Dropwizard 4.0.12**. It processes financial transactions through a **double-entry bookkeeping system** using **journal templates** and **message queue processing**.

## Development Commands

### Build and Run
```bash
# Build and run
./gradlew clean build          # Clean build
./gradlew run                  # Run locally (uses server.yaml)
./gradlew test                 # Run tests
./gradlew generateOpenAPI      # Generate swagger.json/yaml

# Docker development workflow
./run.sh                       # Full automated startup
./run.sh --build              # Force rebuild and start
./shutdown.sh                  # Stop everything
./shutdown.sh --stop          # Stop app only (keep infrastructure)
```

### Prerequisites
- Java 21
- Gradle 8.12.1 (initialize with: `gradle wrapper --gradle-version 8.12.1`)
- Docker and Docker-Compose
- PostgreSQL 17.2

## Architecture

### Core Stack
- **Dropwizard 4.0.12** - REST framework with embedded Jetty
- **JDBI 3.49.1** - Database access layer  
- **Google Guice 7.0.0** - Dependency injection
- **PostgreSQL** - Primary database
- **Kafka** - Message queue processing
- **AWS SDK** - Cloud integration (SQS, SNS, S3, DynamoDB)

### Key Ports
- **8080** - Main API endpoints
- **8081** - Admin/metrics endpoints
- **8090** - Debug endpoints
- **8086** - Swagger documentation
- **5432** - PostgreSQL
- **4566** - AWS LocalStack (development)

### Application Structure

**Entry Point**: `AccountingCoreApp.java` - Dropwizard application with JWT authentication, health checks, and Guice DI

**Configuration**: `AccountingCoreAppConfig.java` - Centralized config supporting database, AWS services, Kafka, CORS, and environment variables

**Dependency Injection**: `AccountingCoreModule.java` - Configurable caching layer that can switch between direct DAO access or cached repositories

### Database Layer (DAO Pattern)

**Primary DAO**: `JournalDAO.java` handles:
- Journal creation with optional booking dates
- Transaction (Tx) management with conflict resolution
- Batch processing for accounting periods
- Journal line creation (double-entry bookkeeping)
- Payment and order tracking

**Core Entities**:
- **Journal** - Main accounting journal entry
- **JournalLine** - Individual debit/credit lines  
- **Tx** - Financial transactions
- **Batch** - Accounting period groupings
- **Payment/Order** - Business transaction details

### Message Processing System

**MessageProcessor.java** implements a pipeline:
1. Message validation (source, event type, JSON schema)
2. Account validation (code, type, test account handling)
3. Amount validation (currency validation)
4. Template resolution via TemplateCache
5. Journal template execution via JournalTemplateFactory

### Template System

Templates in `/templates/` directory define business logic:
- **PaymentReceived** - Customer payment processing
- **PaymentMatch** - Matching payments to orders
- **OrderReceived** - Order booking
- **BankPayment** - Bank transaction processing
- **PaymentReceivedCorrection** - Correction entries

Templates implement `JournalTemplateIf.java` interface and are instantiated via `JournalTemplateFactory`.

### REST API Endpoints

**TemplateExecutionResource** (`/accounting`):
- `POST /journals/from-template` - Execute predefined templates
- `POST /journals` - Create custom journal entries
- `POST /journals/wa` - Journal creation with file attachments

**AccountResource** (`/accounting/account`):
- `POST /create` - Create new accounts

Authentication uses API keys with JWT tokens and role-based access control.

## Key Design Patterns

### Double-Entry Bookkeeping
- Every financial transaction creates balanced journal entries
- JournalLines represent debits and credits
- Batches group transactions by accounting periods

### Template-Driven Processing
- Business logic encapsulated in template classes
- JSON schema validation for message structure
- Template cache for performance
- Factory pattern for template instantiation

### Repository Pattern with Caching
Two-tier access strategy configurable via `AccountingCoreModule`:
- **Direct DAO access** - For development/debugging
- **Cached repositories** - For production performance

### Message Queue Architecture
- Kafka-based async processing
- Dead letter queues for failed messages
- Message persistence in DynamoDB
- Configurable processing capabilities

## Environment Configuration

The application uses extensive environment variable configuration. Key variables:

- `ENVIRONMENT` - Running environment (integration, staging, etc.)
- `API_KEY` - API authentication
- `PG_HOST`, `PG_PORT`, `ACC_DB_USERNAME`, `ACC_DB_PASSWORD` - Database connection
- `AWS_*` variables - AWS service configuration
- `LEDGER_AUTO_SKIP_MESSAGE` - Debug flag (default: false)

See `server.yaml` for complete configuration structure.

## Testing

- **JUnit 5** with **Mockito** for unit testing
- **AssertJ** for fluent assertions
- **Liquibase** for database migrations in tests
- **PostgreSQL test extensions** for integration testing
- Tests exclude `testUtils` package by default

## API Documentation

Swagger/OpenAPI documentation available at:
- http://localhost:8086/swagger.json
- http://localhost:8086/swagger.yaml

Generate with: `./gradlew generateOpenAPI`