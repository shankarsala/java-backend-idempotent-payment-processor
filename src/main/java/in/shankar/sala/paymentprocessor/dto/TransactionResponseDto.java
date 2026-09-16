package in.shankar.sala.paymentprocessor.dto;

import in.shankar.sala.paymentprocessor.entity.Transaction;
import in.shankar.sala.paymentprocessor.enums.TransactionStatus;
import in.shankar.sala.paymentprocessor.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponseDto {

    private String transactionId;
    private Long userId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private LocalDateTime createdAt;

    public TransactionResponseDto() {
    }

    public TransactionResponseDto(
            String transactionId,
            Long userId,
            BigDecimal amount,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime createdAt) {

        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static TransactionResponseDto from(Transaction transaction) {

        return new TransactionResponseDto(
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
