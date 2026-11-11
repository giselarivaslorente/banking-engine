package com.grivaslorente.banking_engine.account;

import java.math.BigDecimal;

public record WithdrawRequest(
        BigDecimal amount
) {
}
