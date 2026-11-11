package com.grivaslorente.banking_engine.account;


import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class BankAccountTest {
    @Test
    void depositIncreasesBalance() {
        BankAccount account = new BankAccount("Alice", "EUR");

        account.deposit(new BigDecimal("100.00"));

        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(account.getBalance())
        );
    }

    @Test
    void withdrawDecreasesBalance() {
        BankAccount account = new BankAccount("Alice", "EUR");
        account.deposit(new BigDecimal("100.00"));

        account.withdraw(new BigDecimal("30.00"));

        assertEquals(
                0,
                new BigDecimal("70.00").compareTo(account.getBalance())
        );
    }

    @Test
    void withdrawRejectsInsufficientFunds() {
        BankAccount account = new BankAccount("Alice", "EUR");
        account.deposit(new BigDecimal("50.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> account.withdraw(new BigDecimal("80.00"))
        );
    }

    @Test
    void depositRejectsNegativeAmount() {
        BankAccount account = new BankAccount("Alice", "EUR");

        assertThrows(
                IllegalArgumentException.class,
                () -> account.deposit(new BigDecimal("-10.00"))
        );
    }
}
