package com.grivaslorente.banking_engine.account;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        UUID destinationAccountId,
        BigDecimal amount
) {
}
