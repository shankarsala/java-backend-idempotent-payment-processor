package in.shankar.sala.paymentprocessor.dto;

import in.shankar.sala.paymentprocessor.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class TransactionRequestDto {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Amount must be greater than zero"
    )
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    public TransactionRequestDto() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
}
