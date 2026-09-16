# Idempotent Payment/Wallet Event Processor

A Spring Boot backend that processes wallet transactions while handling duplicate transaction IDs and concurrent debit requests safely.

## Technology Stack

* Java 17
* Spring Boot
* Spring Data JPA / Hibernate
* H2 in-memory database
* Maven
* JUnit 5

## How to Run

Start the application with:

```bash
./mvnw spring-boot:run
```

The application runs on the default Spring Boot port:

```text
http://localhost:8080
```

No external database setup is required because the application uses an in-memory H2 database.

## Run Tests

Run the complete test suite with:

```bash
./mvnw test
```

## API

### Process Transaction

```text
POST /api/v1/transactions/process
```

Example request:

```json
{
  "transactionId": "TXN-001",
  "userId": 1001,
  "amount": 100.00,
  "type": "DEBIT"
}
```

Supported transaction types:

* `DEBIT`
* `CREDIT`

## Concurrency and Idempotency

The wallet row is protected using a JPA `PESSIMISTIC_WRITE` lock inside a transactional service method.

For duplicate transaction IDs, the service checks for an existing transaction before processing and checks again after acquiring the wallet lock. This ensures that concurrent requests with the same transaction ID do not deduct the wallet balance multiple times.

The `transactionId` column also has a database-level unique constraint.

For different concurrent debit transactions against the same wallet, the wallet lock serializes balance updates. The balance is checked before each debit, preventing the wallet from becoming negative.
