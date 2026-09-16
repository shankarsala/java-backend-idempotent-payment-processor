# Design Decisions

## 1. Handling the Concurrency Race Condition

The application uses two complementary mechanisms to handle concurrency.

### Wallet row locking

`WalletRepository.findByUserId()` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)`. The transaction-processing service is also marked with `@Transactional`.

For a debit, the application acquires the wallet row lock before checking and updating the balance. This ensures that concurrent transactions for the same wallet are processed one at a time.

The balance check and balance update therefore happen inside the same database transaction while the wallet row is locked. This prevents two different debit transactions from both reading the same balance and causing the wallet to become negative.

### Duplicate transaction IDs

`transactionId` has a database-level unique constraint, and the service first checks whether the transaction already exists.

For concurrent requests using the same `transactionId`, the service performs a second transaction lookup after acquiring the wallet lock. If another request has already created the transaction, the later request returns the existing transaction instead of processing the debit again.

The unique constraint provides an additional database-level guarantee that duplicate transaction records cannot be created.

### Different concurrency problems

These mechanisms address different problems:

* **Duplicate transaction ID:** idempotency lookup prevents the same transaction from being processed more than once.
* **Different transaction IDs for the same wallet:** `PESSIMISTIC_WRITE` locking serializes balance modifications so simultaneous debits cannot spend the same balance.

The `@Transactional` boundary keeps the lock, balance validation, balance update, and transaction persistence within the same database transaction.

## 2. AI Assistant — Incorrect or Sub-optimal Suggestion

During the final technical review, the AI assistant initially considered the existing concurrent duplicate-transaction test sufficient because it verified that the final wallet balance was deducted only once.

The test, however, allowed some concurrent requests to return `409` or even fail with an exception. The test could therefore pass while the application was actually hitting the database `UNIQUE` constraint for duplicate transaction IDs.

The implementation was corrected by re-checking `transactionId` after acquiring the wallet lock, and the test was strengthened so all three concurrent requests with the same transaction ID must return successfully while only one transaction is stored and only one debit affects the wallet.

This highlighted an important testing lesson: a concurrency test should verify the expected behavior of individual concurrent requests, not only the final database state.
