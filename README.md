# MoneyMind

MoneyMind is a personal finance management application that helps users import, categorize, and track bank transactions 
from CSV files. Designed with modularity and extensibility in mind, MoneyMind leverages Quarkus for high performance and
PostgreSQL for secure, reliable data storage.

This project demonstrates strong adherence to clean architecture principles (Hexagonal Architecture) with a focus on 
code quality, modularity, and domain-driven design.

---

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Functional Requirements](#functional-requirements)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
    - [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)


---

## Features

- Import and parse financialRecords from CSV files for multiple banks.
- Automatically or manually categorize financialRecords.
- Track your spending by day, week, month, and year.
- Extensible CSV parser support for different banks (open for community contribution).
- Clean and modular design using Hexagonal Architecture (Ports and Adapters).
- RESTful API for easy interaction.

---

## Technology Stack

- **Backend Framework**: [Quarkus](https://quarkus.io/) (Supersonic, Subatomic Java)
- **Programming Language**: Java 11+
- **Database**: PostgreSQL

---

## Project Structure

The application follows **Hexagonal Architecture (Ports and Adapters)**, ensuring a clean separation of concerns between
business logic, infrastructure, and user interfaces.

```
com.moneymind
  ├── domain          // Core business entities and logic
  ├── adapters         // Adapters like database repositories, CSV parsers, etc.
  ├──── in.web         // REST API for interacting with the system, allowing users to import financialRecords, categorize them, and view analytics.
  ├──── out.postgres   // Manages database persistence (PostgreSQL).
  ├── application     // Service implementations in the form of use cases, parser logic
  ├──── ports     // ports for the application

```

## Functional Requirements

1. **Import Transactions from CSV Files**
    - Upload and parse CSV files containing bank `financialRecords`.
    - Support for multiple bank formats (with extendable parser support).

2. **List All Transactions**
    - Retrieve a list of all `financialRecords`, filtered by date range, category, or bank.

3. **Add Single Expense**
    - Manually add individual expenses not captured by CSV imports.

4. **Categorize Transactions**
    - Assign categories to `financialRecords` manually or through an automated categorization process.
    - Auto-categorization improves over time based on user input (rule-based learning).

5. **Summarize Spending**
    - Display total spending for specific periods (daily, weekly, monthly, yearly).
    - View spending breakdowns by category and `financialRecord` source (bank).

6. **Extensible Parser Contributions**
    - Public API for third-party contributors to create and submit parsers for new banks.

---

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Java 11+**: Required to run the Quarkus application.
- **Maven**: To manage dependencies and build the project.
- **PostgreSQL**: The database used for storing `bank_transactions`.
- **Docker** (optional): For running PostgreSQL in a containerized environment.

### Installation

1. **Configure PostgreSQL:**

   Update the `application.properties` in `src/main/resources` with your PostgreSQL credentials:

   ```properties
   quarkus.datasource.db-kind=postgresql
   quarkus.datasource.username=<your-username>
   quarkus.datasource.password=<your-password>
   quarkus.datasource.jdbc.url=jdbc:postgresql://<ip>:5432/<moneymind>
   ```

2. **Build the project:**
   ```bash
   ./mvnw clean install
   ```

4. **Run the application:**
   ```bash
   ./mvnw quarkus:dev
   ```

The application will be running at `http://localhost:8080`.

---

## API Endpoints

Check `/docs/moneymind.yaml`
