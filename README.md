# Idempotent Payment/Wallet Event Processor

A Spring Boot backend service that processes wallet transactions while safely handling duplicate transaction requests and concurrent debit operations.

## Technology Stack

* Java 17+
* Spring Boot
* Spring Data JPA / Hibernate
* H2 In-Memory Database
* Maven
* JUnit 5

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

## Idempotency

The `transactionId` is used as the idempotency key.

The service first checks whether the transaction already exists. For concurrent requests with the same transaction ID, it performs a second check after acquiring the wallet row lock.

If another request has already processed the transaction, the existing transaction is returned instead of processing the debit again.

The `transactionId` column also has a database-level unique constraint to prevent duplicate transaction records.

## Concurrency and Locking

The wallet row is protected using JPA `PESSIMISTIC_WRITE` locking.

The transaction-processing service uses `@Transactional`, keeping the wallet lock, balance check, balance update, and transaction persistence within the same database transaction.

This ensures that concurrent debit requests for the same wallet are processed safely and prevents the wallet balance from becoming negative.

For example, with a wallet balance of ₹500 and ten concurrent debit requests of ₹100:

* 5 requests successfully debit ₹100.
* The final wallet balance is exactly ₹0.
* The remaining 5 requests fail because of insufficient funds.
* The balance does not become negative.

## Integration Tests

The project uses an H2 in-memory database, so the tests require no external database setup.

Run the complete test suite with:

```bash
./mvnw test
```

The test suite includes the required scenarios:

### 1. Happy Path

> Processes a single valid debit transaction successfully.

Verifies that a valid debit transaction is processed and the wallet balance is updated correctly.

### 2. Idempotency

> Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once.

Verifies that concurrent duplicate requests do not process the same transaction more than once.

### 3. Race Condition

> Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds.

Verifies that database-level locking prevents concurrent debit requests from producing an incorrect or negative balance.

## How to Run

### Prerequisites

* Java 17 or later

Maven does not need to be installed separately because the project includes the Maven Wrapper.

### Start the Application

```bash
./mvnw spring-boot:run
```

The application runs on:

```text
http://localhost:8080
```

### Run Tests

```bash
./mvnw test
```

No external database setup is required because the application uses an H2 in-memory database.
