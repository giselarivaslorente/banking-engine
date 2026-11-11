package com.grivaslorente.banking_engine.account;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class BankAccountConcurrencyTest {

    @Autowired
    private BankAccountService service;

    @Test
    void concurrentWithdrawalsCannotOverdraw() throws Exception {

        // Create an account with 70 EUR
        BankAccount account =
                service.createAccount("Concurrency Test", "EUR");

        UUID accountId = account.getId();
        service.deposit(accountId, new BigDecimal("70.00"));

        // Prepare two concurrent threads
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> withdrawal = () -> {
            start.await();

            try {
                service.withdraw(accountId, new BigDecimal("50.00"));
                return true;
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().value() == 409) {
                    return false;
                }
                throw ex;
            }
        };

        try {
            Future<Boolean> first = executor.submit(withdrawal);
            Future<Boolean> second = executor.submit(withdrawal);

            // Release both threads at the same time
            start.countDown();

            int successfulWithdrawals =
                    (first.get() ? 1 : 0) +
                            (second.get() ? 1 : 0);

            // Only one withdrawal should succeed
            assertEquals(1, successfulWithdrawals);

            // Final balance must be 20 EUR
            BigDecimal finalBalance =
                    service.getAccount(accountId).getBalance();

            assertEquals(
                    0,
                    new BigDecimal("20.00").compareTo(finalBalance)
            );

        } finally {
            executor.shutdownNow();
        }
    }
}
