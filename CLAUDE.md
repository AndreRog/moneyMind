# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

```bash
# Build all modules (from root)
./mvnw clean install

# Run in development mode (hot reload) - must run from app/ directory
cd app && ../mvnw quarkus:dev

# Run tests (from root)
./mvnw test

# Run tests for app module only
./mvnw test -pl app

# Run a single test class
./mvnw test -pl app -Dtest=UpdateTransactionsTest

# Run a single test method
./mvnw test -pl app -Dtest=UpdateTransactionsTest#shouldReturnNullWhenTransactionDoesNotExist

# Generate jOOQ classes (requires Docker, run from app/)
cd app && ../mvnw generate-sources -Pjooq

# Build native executable
./mvnw package -Pnative -pl app
```

The application runs at `http://localhost:9000` (configured in `app/src/main/resources/application.properties`).

## Architecture Overview

MoneyMind follows **Hexagonal Architecture** (Ports and Adapters) and is structured as a **multi-module Maven project**:

### Maven Modules

- **Root `pom.xml`** — parent pom, defines shared properties and dependency management
- **`classifier/`** — standalone ML classifier jar (no Quarkus dependency)
- **`app/`** — Quarkus application (finance bounded context), depends on `classifier`

### Module Structure

```
classifier/src/main/java/com/moneymind/classifier/
├── domain/           # Transaction, PartialTransaction, ClassificationResult
├── ports/            # Classifier, TrainingDataService (interfaces)
├── infrastructure/   # Training data repository (jOOQ)
├── di/               # CDI config for classifier beans
└── WekaRandomForestClassifier.java

app/src/main/java/com/moneymind/
├── finance/
│   ├── domain/
│   │   ├── core/         # FinancialRecord, ClassifiedFinancialRecord, ClassificationQuery, etc.
│   │   ├── ports/        # ALL secondary ports: TransactionRepository, TransactionClassifier,
│   │   │                 #   TransactionsParser, BankRegistry
│   │   ├── transactions/ # Use cases: ImportTransactions, SearchTransactions, etc.
│   │   └── banks/        # ListBanks use case
│   ├── infrastructure/
│   │   ├── web/http/     # REST API resources + DTOs (DTO never leaks to domain)
│   │   ├── postgres/     # Database persistence (jOOQ)
│   │   ├── file/         # CSV/XLS parsers per bank (TransactionsParserFactory implements BankRegistry)
│   │   └── txClassifier/ # ClassificationEngine (adapts Classifier port → TransactionClassifier port)
│   └── di/               # CDI dependency injection config
└── App.java
```

### Key Patterns

- **Use Cases**: Located in `finance/domain/transactions/` - each class represents a single use case (ImportTransactions, SearchTransactions, UpdateTransactions, ClassifyTransactions)
- **All Ports in Domain**: All secondary port interfaces live in `finance/domain/ports/` - domain never imports from infrastructure
- **DI Classes**: `finance/di/DI.java` and `classifier/di/DI.java` wire up all dependencies using CDI producers
- **BankRegistry**: `TransactionsParserFactory` implements `BankRegistry` port — auto-discovers parsers via CDI
- **ClassificationEngine**: Anti-corruption layer adapting `Classifier` (classifier module) → `TransactionClassifier` (finance port)
- **ML Classifier**: `WekaRandomForestClassifier` uses Weka's RandomForest with TF-IDF text features; swappable via `Classifier` interface

### Classifier Swappability

To replace the Weka classifier with an external REST API:
1. Implement `com.moneymind.classifier.ports.Classifier` in a new module
2. Update `app/finance/di/DI.java` to wire the new implementation
3. No finance domain code changes required

### Database

- PostgreSQL with Flyway migrations in `src/main/resources/db/migration/`
- jOOQ for type-safe SQL queries
- Main table: `bank_transactions`

### Adding a New Bank Parser

1. Create a parser class in `app/src/main/java/.../finance/infrastructure/file/` implementing `TransactionsParser`
2. Annotate with `@ApplicationScoped` and `@BankType("BANK_NAME")`
3. The factory will automatically discover and register it via CDI

### REST API

API spec available at `/docs/moneymind.yaml`. Main endpoints:
- `POST /transactions/import` - Import CSV file
- `GET /transactions/search` - Search with filters
- `PUT /transactions/{id}` - Update category
- `POST /transactions/classify` - Auto-classify transactions

## Dependencies

- **Quarkus 3.25** - Runtime framework
- **jOOQ** - SQL query builder
- **Weka** - Machine learning (RandomForest classifier)
- **OpenCSV** - CSV parsing
- **Apache POI** - Excel file support
- **Flyway** - Database migrations

## Native Libraries (WSL2/Linux)

For optimal ML performance, install native BLAS/LAPACK:
```bash
sudo apt-get install -y libatlas3-base libopenblas-base libgfortran3
```
