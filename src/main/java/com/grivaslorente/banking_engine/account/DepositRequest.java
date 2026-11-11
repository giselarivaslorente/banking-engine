package com.grivaslorente.banking_engine.account;

import java.math.BigDecimal;

public record DepositRequest(
        BigDecimal amount
) {
}
