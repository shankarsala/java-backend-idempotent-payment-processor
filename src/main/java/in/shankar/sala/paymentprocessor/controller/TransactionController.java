package in.shankar.sala.paymentprocessor.controller;

import in.shankar.sala.paymentprocessor.dto.TransactionRequestDto;
import in.shankar.sala.paymentprocessor.dto.TransactionResponseDto;
import in.shankar.sala.paymentprocessor.entity.Transaction;
import in.shankar.sala.paymentprocessor.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/process")
    public ResponseEntity<TransactionResponseDto> processTransaction(
            @Valid @RequestBody TransactionRequestDto request) {

        Transaction transaction = transactionService.processTransaction(
                request.getTransactionId(),
                request.getUserId(),
                request.getAmount(),
                request.getType()
        );

        return ResponseEntity.ok(
                TransactionResponseDto.from(transaction)
        );
    }
}
