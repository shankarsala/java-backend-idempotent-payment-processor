package in.shankar.sala.paymentprocessor;

import in.shankar.sala.paymentprocessor.entity.Transaction;
import in.shankar.sala.paymentprocessor.entity.Wallet;
import in.shankar.sala.paymentprocessor.enums.TransactionStatus;
import in.shankar.sala.paymentprocessor.repository.TransactionRepository;
import in.shankar.sala.paymentprocessor.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TransactionIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    @Transactional
    @DisplayName("Processes a single valid debit transaction successfully.")
    void processesSingleValidDebitTransactionSuccessfully() throws Exception {

        Long userId = 1001L;
        BigDecimal initialBalance = new BigDecimal("500.00");
        BigDecimal debitAmount = new BigDecimal("100.00");
        String transactionId = "TXN-TEST-001";

        Wallet wallet = new Wallet(userId, initialBalance);
        walletRepository.save(wallet);

        String requestBody = """
                {
                    "transactionId": "TXN-TEST-001",
                    "userId": 1001,
                    "amount": 100.00,
                    "type": "DEBIT"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/transactions/process")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk());

        Wallet updatedWallet = walletRepository
                .findByUserId(userId)
                .orElseThrow();

        assertEquals(
                new BigDecimal("400.00"),
                updatedWallet.getBalance()
        );

        assertEquals(1, transactionRepository.count());

        Transaction savedTransaction = transactionRepository
                .findByTransactionId(transactionId)
                .orElseThrow();

        assertEquals(transactionId, savedTransaction.getTransactionId());
        assertEquals(userId, savedTransaction.getUserId());
        assertEquals(debitAmount, savedTransaction.getAmount());
        assertEquals(TransactionStatus.SUCCESS, savedTransaction.getStatus());

        assertTrue(
                transactionRepository.findByTransactionId(transactionId).isPresent()
        );
    }


    @Test
    @DisplayName("Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once.")
    void sendsThreeIdenticalTransactionIdsSimultaneously() throws Exception {

        Long userId = 1002L;
        BigDecimal initialBalance = new BigDecimal("500.00");
        BigDecimal debitAmount = new BigDecimal("100.00");
        String transactionId = "TXN-CONCURRENT-001";

        Wallet wallet = new Wallet(userId, initialBalance);
        walletRepository.save(wallet);

        String requestBody = """
            {
                "transactionId": "TXN-CONCURRENT-001",
                "userId": 1002,
                "amount": 100.00,
                "type": "DEBIT"
            }
            """;

        int numberOfRequests = 3;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfRequests);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Integer>> responses = new ArrayList<>();

        for (int i = 0; i < numberOfRequests; i++) {
            responses.add(executorService.submit(() -> {

                startLatch.await();

                return mockMvc.perform(
                        post("/api/v1/transactions/process")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                ).andReturn().getResponse().getStatus();
            }));
        }

        startLatch.countDown();

        int successfulResponses = 0;
        int conflictResponses = 0;
        int failedRequests = 0;

        for (Future<Integer> response : responses) {
            try {
                int status = response.get();

                if (status == 200) {
                    successfulResponses++;
                } else if (status == 409) {
                    conflictResponses++;
                } else {
                    throw new AssertionError(
                            "Unexpected HTTP status: " + status
                    );
                }

            } catch (Exception exception) {
                failedRequests++;
            }
        }

        executorService.shutdown();

        assertTrue(
                successfulResponses >= 1,
                "At least one request must succeed."
        );

        assertEquals(
                3,
                successfulResponses + conflictResponses + failedRequests
        );

        Wallet updatedWallet = walletRepository.findById(wallet.getId()).orElseThrow();

        assertEquals(
                new BigDecimal("400.00"),
                updatedWallet.getBalance()
        );

        List<Transaction> transactions =
                transactionRepository.findAll();

        assertEquals(1, transactions.size());

        Transaction savedTransaction =
                transactionRepository.findByTransactionId(transactionId).orElseThrow();

        assertEquals(transactionId, savedTransaction.getTransactionId());
        assertEquals(userId, savedTransaction.getUserId());
        assertEquals(debitAmount, savedTransaction.getAmount());
        assertEquals(TransactionStatus.SUCCESS, savedTransaction.getStatus());
    }
}
