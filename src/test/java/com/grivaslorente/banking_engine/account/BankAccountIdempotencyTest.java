package com.grivaslorente.banking_engine.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")

public class BankAccountIdempotencyTest {

    @Autowired
    private BankAccountService service;

    @Test
    void repeatingSameTransferDoesNotMoveMoneyTwice() {
        BankAccount alice = service.createAccount("Alice Test", "EUR");
        BankAccount bob = service.createAccount("Bob Test", "EUR");

        service.deposit(alice.getId(), new BigDecimal("200.00"));

        String key = UUID.randomUUID().toString();

        service.transfer(
                alice.getId(), bob.getId(),
                new BigDecimal("50.00"), key
        );

        service.transfer(
                alice.getId(), bob.getId(),
                new BigDecimal("50.00"), key
        );

        assertEquals(
                0,
                new BigDecimal("150.00").compareTo(
                        service.getAccount(alice.getId()).getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("50.00").compareTo(
                        service.getAccount(bob.getId()).getBalance())
        );
    }

    @Test
    void reusingKeyWithDifferentAmountReturnsConflict() {
        BankAccount alice = service.createAccount("Alice Test", "EUR");
        BankAccount bob = service.createAccount("Bob Test", "EUR");

        service.deposit(alice.getId(), new BigDecimal("200.00"));

        String key = UUID.randomUUID().toString();

        service.transfer(
                alice.getId(), bob.getId(),
                new BigDecimal("50.00"), key
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.transfer(
                        alice.getId(), bob.getId(),
                        new BigDecimal("80.00"), key
                )
        );

        assertEquals(409, exception.getStatusCode().value());

        assertEquals(
                0,
                new BigDecimal("150.00").compareTo(
                        service.getAccount(alice.getId()).getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("50.00").compareTo(
                        service.getAccount(bob.getId()).getBalance())
        );
    }


    @Test
    void concurrentTransfersWithSameKeyExecuteOnlyOnce() throws Exception {
        BankAccount alice = service.createAccount("Alice Concurrent", "EUR");
        BankAccount bob = service.createAccount("Bob Concurrent", "EUR");

        service.deposit(alice.getId(), new BigDecimal("200.00"));

        String key = UUID.randomUUID().toString();

        java.util.concurrent.ExecutorService executor =
                java.util.concurrent.Executors.newFixedThreadPool(2);

        java.util.concurrent.CountDownLatch start =
                new java.util.concurrent.CountDownLatch(1);

        try {
            java.util.concurrent.Callable<Void> transfer = () -> {
                start.await();
                service.transfer(
                        alice.getId(),
                        bob.getId(),
                        new BigDecimal("50.00"),
                        key
                );
                return null;
            };

            var first = executor.submit(transfer);
            var second = executor.submit(transfer);

            start.countDown();

            first.get(15, java.util.concurrent.TimeUnit.SECONDS);
            second.get(15, java.util.concurrent.TimeUnit.SECONDS);

            assertEquals(
                    0,
                    new BigDecimal("150.00").compareTo(
                            service.getAccount(alice.getId()).getBalance())
            );

            assertEquals(
                    0,
                    new BigDecimal("50.00").compareTo(
                            service.getAccount(bob.getId()).getBalance())
            );

        } finally {
            executor.shutdownNow();
        }


    }

    @Test
    void failedTransferDoesNotChangeBalances() {
        BankAccount alice = service.createAccount("Alice", "EUR");
        BankAccount bob = service.createAccount("Bob", "EUR");

        service.deposit(alice.getId(), new BigDecimal("100.00"));

        assertThrows(ResponseStatusException.class, () ->
                service.transfer(
                        alice.getId(),
                        bob.getId(),
                        new BigDecimal("150.00"),
                        UUID.randomUUID().toString()
                )
        );

        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(
                        service.getAccount(alice.getId()).getBalance()
                )
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        service.getAccount(bob.getId()).getBalance()
                )
        );
    }

}
