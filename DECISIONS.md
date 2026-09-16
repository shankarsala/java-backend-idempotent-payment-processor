# Design Decisions

## 1. How did you handle the concurrency race condition?

The application uses database-level pessimistic locking together with transactional processing.

### Wallet Row Locking

`WalletRepository.findByUserId()` uses:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

The transaction-processing service uses:

```java
@Transactional
```

For a debit transaction, the wallet row is locked before the balance is checked and updated.

This ensures that concurrent transactions for the same wallet are processed one at a time.

The balance check and balance update occur within the same database transaction while the wallet row is locked. This prevents concurrent debit requests from reading the same balance and causing the wallet to become negative.

### Duplicate Transaction IDs

The `transactionId` column has a database-level unique constraint.

The service first checks whether the transaction already exists. For concurrent requests using the same transaction ID, the service performs a second transaction lookup after acquiring the wallet lock.

If another request has already processed the transaction, the later request returns the existing transaction instead of processing the debit again.

This ensures that duplicate requests do not deduct the wallet balance more than once.

### Summary

Two mechanisms handle the two concurrency scenarios:

* **Duplicate transaction IDs:** Idempotency checks prevent the same transaction from being processed more than once.
* **Different transaction IDs for the same wallet:** `PESSIMISTIC_WRITE` locking serializes wallet balance updates and prevents negative balances.

The `@Transactional` boundary keeps the locking, balance validation, balance update, and transaction persistence within the same database transaction.

---

## 2. Where did your AI assistant give you an incorrect or sub-optimal suggestion?

During the technical review, the AI assistant initially considered the concurrent duplicate-transaction test sufficient because it verified that the final wallet balance was deducted only once.

However, the earlier version of the test allowed some concurrent requests to return `409 Conflict` or fail with an exception.

This meant that the test could potentially pass while the application was relying on the database `UNIQUE` constraint to reject duplicate transaction records.

The implementation was therefore improved by performing a second `transactionId` lookup after acquiring the wallet lock.

The concurrent idempotency test was also strengthened so that all three concurrent requests using the same transaction ID complete successfully, while:

* Only one transaction record is stored.
* Only one debit affects the wallet balance.
* Duplicate requests do not cause additional balance deductions.

This highlighted an important testing lesson: a concurrency test should verify both the final database state and the expected behavior of individual concurrent requests, rather than checking only the final balance.
