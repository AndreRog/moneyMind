# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

```bash
# Build the project
./mvnw clean install

# Run in development mode (hot reload)
./mvnw quarkus:dev

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=UpdateTransactionsTest

# Run a single test method
./mvnw test -Dtest=UpdateTransactionsTest#shouldReturnNullWhenTransactionDoesNotExist

# Generate jOOQ classes (requires Docker)
./mvnw generate-sources -Pjooq

# Build native executable
./mvnw package -Pnative
```

The application runs at `http://localhost:8080`.

## Architecture Overview

MoneyMind follows **Hexagonal Architecture** (Ports and Adapters) with two main bounded contexts:

### Module Structure

```
com.moneymind
├── finance/          # Main finance bounded context
│   ├── domain/       # Core business logic (use cases + entities)
│   │   ├── core/         # Domain entities (FinancialRecord, Category)
│   │   ├── transactions/ # Use cases (ImportTransactions, SearchTransactions, etc.)
│   │   ├── ports/        # Repository interfaces
│   │   └── banks/        # Bank-related domain logic
│   ├── infrastrucuture/  # Adapters (note: typo in codebase)
│   │   ├── web/http/     # REST API resources
│   │   ├── postgres/     # Database persistence (jOOQ)
│   │   ├── file/         # CSV parsers per bank
│   │   └── txClassifier/ # Classification engine adapter
│   └── di/               # CDI dependency injection config
│
└── classifier/       # ML classification bounded context
    ├── domain/           # Transaction, PartialTransaction entities
    ├── ports/            # Classifier, TrainingDataService interfaces
    ├── infrastructure/   # Training data repository
    └── di/               # CDI config for classifier beans
```

### Key Patterns

- **Use Cases**: Located in `domain/transactions/` - each class represents a single use case (ImportTransactions, SearchTransactions, UpdateTransactions, ClassifyTransactions)
- **DI Classes**: `finance/di/DI.java` and `classifier/di/DI.java` wire up all dependencies using CDI producers
- **Parser Factory**: `TransactionsParserFactory` selects the correct bank CSV parser based on type string
- **ML Classifier**: `WekaRandomForestClassifier` uses Weka's RandomForest with TF-IDF text features for auto-categorization

### Database

- PostgreSQL with Flyway migrations in `src/main/resources/db/migration/`
- jOOQ for type-safe SQL queries
- Main table: `bank_transactions`

### Adding a New Bank Parser

1. Create a parser class in `finance/infrastrucuture/file/` implementing `TransactionsParser`
2. Annotate with `@ApplicationScoped` and `@BankType("BANK_NAME")`
3. The factory will automatically discover and register it

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
