package in.shankar.sala.paymentprocessor.service;

import in.shankar.sala.paymentprocessor.entity.Transaction;
import in.shankar.sala.paymentprocessor.entity.Wallet;
import in.shankar.sala.paymentprocessor.enums.TransactionStatus;
import in.shankar.sala.paymentprocessor.enums.TransactionType;
import in.shankar.sala.paymentprocessor.exception.InsufficientBalanceException;
import in.shankar.sala.paymentprocessor.exception.InvalidTransactionException;
import in.shankar.sala.paymentprocessor.exception.WalletNotFoundException;
import in.shankar.sala.paymentprocessor.repository.TransactionRepository;
import in.shankar.sala.paymentprocessor.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public Transaction processTransaction(
            String transactionId,
            Long userId,
            BigDecimal amount,
            TransactionType type) {

        validateTransaction(transactionId, userId, amount, type);

        return transactionRepository.findByTransactionId(transactionId)
                .orElseGet(() -> processNewTransaction(
                        transactionId,
                        userId,
                        amount,
                        type
                ));
    }

    private Transaction processNewTransaction(
            String transactionId,
            Long userId,
            BigDecimal amount,
            TransactionType type) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found for user: " + userId
                        ));

        return transactionRepository.findByTransactionId(transactionId)
                .orElseGet(() -> {

                    if (type == TransactionType.DEBIT) {
                        return processDebit(
                                transactionId,
                                userId,
                                amount,
                                wallet
                        );
                    }

                    return processCredit(
                            transactionId,
                            userId,
                            amount,
                            wallet
                    );
                });
    }

    private Transaction processDebit(
            String transactionId,
            Long userId,
            BigDecimal amount,
            Wallet wallet) {

        if (wallet.getBalance().compareTo(amount) < 0) {

            Transaction failedTransaction = new Transaction(
                    transactionId,
                    userId,
                    amount,
                    TransactionType.DEBIT,
                    TransactionStatus.FAILED,
                    LocalDateTime.now()
            );

            transactionRepository.save(failedTransaction);

            throw new InsufficientBalanceException(
                    "Insufficient balance for user: " + userId
            );
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        Transaction successfulTransaction = new Transaction(
                transactionId,
                userId,
                amount,
                TransactionType.DEBIT,
                TransactionStatus.SUCCESS,
                LocalDateTime.now()
        );

        return transactionRepository.save(successfulTransaction);
    }

    private Transaction processCredit(
            String transactionId,
            Long userId,
            BigDecimal amount,
            Wallet wallet) {

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        Transaction successfulTransaction = new Transaction(
                transactionId,
                userId,
                amount,
                TransactionType.CREDIT,
                TransactionStatus.SUCCESS,
                LocalDateTime.now()
        );

        return transactionRepository.save(successfulTransaction);
    }

    private void validateTransaction(
            String transactionId,
            Long userId,
            BigDecimal amount,
            TransactionType type) {

        if (transactionId == null || transactionId.isBlank()) {
            throw new InvalidTransactionException(
                    "Transaction ID is required"
            );
        }

        if (userId == null) {
            throw new InvalidTransactionException(
                    "User ID is required"
            );
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException(
                    "Amount must be greater than zero"
            );
        }

        if (type == null) {
            throw new InvalidTransactionException(
                    "Transaction type is required"
            );
        }
    }
}