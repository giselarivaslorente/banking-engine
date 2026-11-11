package com.grivaslorente.banking_engine.account;

public record CreateAccountRequest (
        String ownerName,
        String currency
) {
        }
